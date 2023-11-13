package com.networknt.scheduler;

import com.networknt.kafka.producer.NativeLightProducer;
import com.networknt.server.Server;
import com.networknt.server.ServerConfig;
import com.networknt.server.StartupHookProvider;
import com.networknt.service.SingletonServiceFactory;
import com.networknt.utility.NetUtils;
import org.apache.kafka.clients.producer.Producer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SchedulerStartupHook implements StartupHookProvider {
    private static final Logger logger = LoggerFactory.getLogger(SchedulerStartupHook.class);
    public static Producer producer = null;
    public static SchedulerStreams streams = null;

    @Override
    public void onStartup() {
        logger.info("SchedulerStartupHook onStartup begins.");
        // create Kafka sidecar producer for publishing scheduled task definition kafka topic
        NativeLightProducer lightProducer = SingletonServiceFactory.getBean(NativeLightProducer.class);
        lightProducer.open();
        producer = lightProducer.getProducer();

        // start the scheduler streams to generate execution task for executors
        int port = ServerConfig.getInstance().getHttpsPort();
        String ip = NetUtils.getLocalAddressByDatagram();
        logger.info("ip = " + ip + " port = " + port);
        streams = new SchedulerStreams();
        // start the kafka stream process
        streams.start(ip, port);
        logger.info("SchedulerStartupHook onStartup ends.");

    }

}
