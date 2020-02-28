package poc.rest.endpoint;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseBroadcaster;
import javax.ws.rs.sse.SseEventSink;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Path("counter")
@ApplicationScoped
public class CounterResource {
    @Context
    private Sse sse;

    @Inject
    private Logger logger;
    private SseBroadcaster sseBroadcaster;
    private AtomicInteger counter;
    private Set<SseEventSink> events;

    @PostConstruct
    public void init() {
        events = ConcurrentHashMap.newKeySet();
        counter = new AtomicInteger(0);
        sseBroadcaster = sse.newBroadcaster();
        sseBroadcaster.onClose(sseEventSink -> {
            events.remove(sseEventSink);
            logger.info("sseEventSink[$sseEventSink] is closed");
        });
        sseBroadcaster.onError((sseEventSink, throwable) -> logger.severe("error occurs in $sseEventSink: $throwable"));
    }

    @Path("register")
    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void register(@Context SseEventSink sseEventSink) {
        logger.info("new sseEventSink[$sseEventSink] is registered");
        events.add(sseEventSink);
        sseBroadcaster.register(sseEventSink);
        sseBroadcaster.broadcast(sse.newEvent(Integer.toString(counter.get())));
    }

    @POST
    public void increaseCounter() {
        logger.info("increase counter by ONE");
        sseBroadcaster.broadcast(sse.newEvent(Integer.toString(counter.incrementAndGet())));
    }

    @Path("sse-event-sink-statuses")
    @GET
    public List<SseEventSinkStatus> getEventStatus() {
        return events.stream()
                .map(event -> new SseEventSinkStatus(!event.isClosed()))
                .collect(Collectors.toList());
    }

    public static class SseEventSinkStatus {
        private boolean alive;

        public SseEventSinkStatus(boolean alive) {this.alive = alive;}

        public boolean isAlive() {
            return alive;
        }

        public void setAlive(boolean alive) {
            this.alive = alive;
        }
    }
}
