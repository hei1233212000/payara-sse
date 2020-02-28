package poc.feature.counter;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.impl.gradle.Gradle;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.sse.InboundSseEvent;
import javax.ws.rs.sse.SseEventSource;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsIterableContaining.hasItem;
import static org.junit.Assert.*;

@RunWith(Arquillian.class)
public class SseTest {
    @ArquillianResource
    @SuppressWarnings("UnusedDeclaration")
    private URI url;

    private List<InboundSseEvent> inboundSseEvents;

    @Deployment
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static WebArchive createDeployment() {
        List<? extends Archive> thirdPartyLibraries =  Gradle.resolver()
                .forProjectDirectory(".")
                .importCompileAndRuntime()
                .resolve()
                .asList(JavaArchive.class);
        return ShrinkWrap.create(WebArchive.class)
                .addPackages(true, "poc")
                .addAsLibraries((Collection<? extends Archive<?>>) thirdPartyLibraries);
    }

    @Before
    public void before() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        inboundSseEvents = new ArrayList<>();
    }

    @Test
    @RunAsClient
    public void serverSentEventClientShouldAbleToCloseTheConnection() {
        // given
        SseEventSource clientSideSseEventSource = createClientSideSseConnection();
        increaseCounterFromServerSide();
        assertEquals("1", latestSseDataReceivedInClientSide());

        // when
        clientSideSseEventSource.close();

        // then
        assertFalse("The client side SSE connection should be closed", clientSideSseEventSource.isOpen());
        increaseCounterFromServerSide();
        assertEquals("1", latestSseDataReceivedInClientSide()); /* the client side should not receive data anymore */
        verifySseLivenessInServerSide(false /* isAlive */);
    }

    private SseEventSource createClientSideSseConnection() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(sseUrl());
        SseEventSource clientSideSseEventSource = SseEventSource.target(target).build();
        clientSideSseEventSource.register(inboundSseEvent -> inboundSseEvents.add(inboundSseEvent));
        clientSideSseEventSource.open();
        assertTrue("The client side SSE connection should be opened", clientSideSseEventSource.isOpen());

        await().until(() -> inboundSseEvents.size() == 1);
        assertEquals("0", latestSseDataReceivedInClientSide());

        verifySseLivenessInServerSide(true /* isAlive */);
        return clientSideSseEventSource;
    }

    private void verifySseLivenessInServerSide(boolean isAlive) {
        ValidatableResponse response = RestAssured.given()
                .accept(ContentType.JSON)
            .when()
                .get(verifySseEventSinkStatusesUrl())
            .then()
                .statusCode(200);
        if (isAlive) {
            response
                    .body("size()", is(1))
                    .body("alive", hasItem(true));
        } else {
            response
                    .body("size()", is(0));
        }
    }

    private void increaseCounterFromServerSide() {
        RestAssured.given()
                .accept(ContentType.JSON)
            .when()
                .post(increaseCounterUrl())
            .then()
                .statusCode(204);
    }

    private String latestSseDataReceivedInClientSide() {
        InboundSseEvent lastInboundSseEvent = inboundSseEvents.get(inboundSseEvents.size() - 1);
        return lastInboundSseEvent.readData();
    }

    private String sseUrl() {
        return url + "api/counter/register";
    }

    private String increaseCounterUrl() {
        return url + "api/counter";
    }

    private String verifySseEventSinkStatusesUrl() {
        return url + "api/counter/sse-event-sink-statuses";
    }
}
