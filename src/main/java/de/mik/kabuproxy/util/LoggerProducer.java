package de.mik.kabuproxy.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.InjectionPoint;

@ApplicationScoped
public class LoggerProducer
{
    @Produces
    @Dependent
    public Logger produceLogger(InjectionPoint injectionPoint)
    {
        return LogManager.getLogger(injectionPoint.getMember().getDeclaringClass());
    }
}
