package com.networknt.scheduler;

import com.networknt.kafka.producer.NativeLightProducer;
import com.networknt.server.ShutdownHookProvider;
import com.networknt.service.SingletonServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SchedulerShutdownHook implements ShutdownHookProvider {
    private static final Logger logger = LoggerFactory.getLogger(SchedulerShutdownHook.class);

    @Override
    public void onShutdown() {
        logger.info("SchedulerShutdownHook onShutdown begins.");
        // close the Kafka transactional producer before the server is shutdown
        NativeLightProducer producer = SingletonServiceFactory.getBean(NativeLightProducer.class);
        try { if(producer != null) producer.close(); } catch(Exception e) {e.printStackTrace();}
        // close the streams
        if(SchedulerStartupHook.streams != null) SchedulerStartupHook.streams.close();
        logger.info("SchedulerShutdownHook onShutdown ends.");
    }

}
