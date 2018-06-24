package poc.core.config

import java.util.logging.Logger
import javax.enterprise.context.Dependent
import javax.enterprise.inject.Produces
import javax.enterprise.inject.spi.InjectionPoint

@Dependent
class LoggerProducer {
    @Produces
    fun produceLogger(injectionPoint: InjectionPoint): Logger {
        return Logger.getLogger(injectionPoint.member.declaringClass.name)
    }
}