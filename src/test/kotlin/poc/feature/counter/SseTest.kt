package poc.feature.counter

import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should be`
import org.hamcrest.core.Is.`is`
import org.hamcrest.core.IsIterableContaining.hasItem
import org.jboss.arquillian.container.test.api.Deployment
import org.jboss.arquillian.container.test.api.RunAsClient
import org.jboss.arquillian.junit.Arquillian
import org.jboss.arquillian.test.api.ArquillianResource
import org.jboss.shrinkwrap.api.ShrinkWrap
import org.jboss.shrinkwrap.api.spec.JavaArchive
import org.jboss.shrinkwrap.api.spec.WebArchive
import org.jboss.shrinkwrap.resolver.impl.gradle.Gradle
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.net.URI
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.sse.InboundSseEvent
import javax.ws.rs.sse.SseEventSource

@RunWith(Arquillian::class)
class SseTest {
    @ArquillianResource
    private lateinit var url: URI

    private lateinit var inboundSseEvents: MutableList<InboundSseEvent>

    companion object {
        private val thirdPartyLibraries = Gradle.resolver()
                .forProjectDirectory(".")
                .importCompileAndRuntime()
                .resolve()
                .asList(JavaArchive::class.java)

        @JvmStatic
        @Deployment
        fun createDeployment() = ShrinkWrap.create(WebArchive::class.java)
                .addPackages(true, "poc")
                .addAsLibraries(thirdPartyLibraries)

    }

    @Before
    fun before() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()
        inboundSseEvents = mutableListOf()
    }

    @Test
    @RunAsClient
    fun `Server Sent Event Client should able to close the connection`() {
        // given
        val clientSideSseEventSource = createClientSideSseConnection()
        increaseCounterFromServerSide()
        latestSseDataReceivedInClientSide() `should be equal to` "1"

        // when
        clientSideSseEventSource.close()

        // then
        clientSideSseEventSource.isOpen `should be` false
        increaseCounterFromServerSide()
        latestSseDataReceivedInClientSide() `should be equal to` "1" /* the client side should not receive data anymore */
        verifySseLivenessInServerSide(isAlive = false)
    }

    private fun createClientSideSseConnection(): SseEventSource {
        val client = ClientBuilder.newClient()
        val target = client.target(sseUrl())
        val sseEventSource = SseEventSource.target(target).build()
        sseEventSource.register { inboundSseEvent ->
            inboundSseEvents.add(inboundSseEvent)
        }
        sseEventSource.open()
        sseEventSource.isOpen `should be` true
        latestSseDataReceivedInClientSide() `should be equal to` "0"

        verifySseLivenessInServerSide(isAlive = true)
        return sseEventSource
    }

    private fun verifySseLivenessInServerSide(isAlive: Boolean) {
        val response = RestAssured.given()
                .accept(ContentType.JSON)
            .`when`()
                .get(verifySseEventSinkStatusesUrl())
            .then()
                .statusCode(200)
        if (isAlive) {
            response
                    .body("size()", `is`(1))
                    .body("alive", hasItem(true))
        } else {
            response
                    .body("size()", `is`(0))
        }
    }

    private fun increaseCounterFromServerSide() {
        RestAssured.given()
                .accept(ContentType.JSON)
            .`when`()
                .post(increaseCounterUrl())
            .then()
                .statusCode(204)
    }

    private fun latestSseDataReceivedInClientSide(): String = inboundSseEvents.last().readData()

    private fun sseUrl() = "${url}api/counter/register"

    private fun increaseCounterUrl() = "${url}api/counter"

    private fun verifySseEventSinkStatusesUrl() = "${url}api/counter/sse-event-sink-statuses"
}