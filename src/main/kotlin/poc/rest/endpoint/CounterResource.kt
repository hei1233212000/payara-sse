package poc.rest.endpoint

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Logger
import javax.annotation.PostConstruct
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.sse.Sse
import javax.ws.rs.sse.SseBroadcaster
import javax.ws.rs.sse.SseEventSink

@Path("counter")
@ApplicationScoped
class CounterResource {
    @Context
    private lateinit var sse: Sse

    @Inject
    private lateinit var logger: Logger
    private lateinit var sseBroadcaster: SseBroadcaster
    private lateinit var counter: AtomicInteger
    private lateinit var events: MutableSet<SseEventSink>

    @PostConstruct
    fun init() {
        events = ConcurrentHashMap.newKeySet()
        counter = AtomicInteger(0)
        sseBroadcaster = sse.newBroadcaster()
        sseBroadcaster.onClose { sseEventSink ->
            events.remove(sseEventSink)
            logger.info("sseEventSink[$sseEventSink] is closed")
        }
        sseBroadcaster.onError { sseEventSink, throwable ->
            logger.severe("error occurs in $sseEventSink: $throwable")
        }
    }

    @Path("register")
    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS)
    fun register(@Context sseEventSink: SseEventSink) {
        logger.info("new sseEventSink[$sseEventSink] is registered")
        events.add(sseEventSink)
        sseBroadcaster.register(sseEventSink)
        sseBroadcaster.broadcast(sse.newEvent(counter.get().toString()))
    }

    @POST
    fun increaseCounter() {
        logger.info("increase counter by ONE")
        sseBroadcaster.broadcast(sse.newEvent(counter.incrementAndGet().toString()))
    }

    @Path("sse-event-sink-statuses")
    @GET
    fun getEventStatus(): List<SseEventSinkStatus> {
        return events.map {
            SseEventSinkStatus(
                    alive = !it.isClosed
            )
        }
    }
}

data class SseEventSinkStatus(
        val alive: Boolean
)