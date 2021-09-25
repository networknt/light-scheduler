package com.networknt.scheduler;

import com.networknt.config.Config;
import com.networknt.kafka.common.KafkaStreamsConfig;
import com.networknt.kafka.streams.LightStreams;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class SchedulerStreams implements LightStreams {
    static private final Logger logger = LoggerFactory.getLogger(SchedulerStreams.class);
    static final KafkaStreamsConfig config = (KafkaStreamsConfig) Config.getInstance().getJsonObjectConfig(KafkaStreamsConfig.CONFIG_NAME, KafkaStreamsConfig.class);

    private KafkaStreams schedulerStreams;

    private void startSchedulerStreams(String ip, int port) {
        TaskSchedulingStreamTopology topology = new TaskSchedulingStreamTopology();
        Properties streamsProps = new Properties();
        streamsProps.putAll(config.getProperties());
        streamsProps.put(StreamsConfig.APPLICATION_SERVER_CONFIG, ip + ":" + port);
        schedulerStreams = new KafkaStreams(topology.buildTaskStreamingTopology(), streamsProps);
        if(config.isCleanUp()) {
            schedulerStreams.cleanUp();
        }
        schedulerStreams.start();
    }

    @Override
    public void start(String ip, int port) {
        if(logger.isDebugEnabled()) logger.info("ServiceStreams is starting...");
        startSchedulerStreams(ip, port);
    }

    @Override
    public void close() {
        if(logger.isDebugEnabled()) logger.info("ServiceStreams is closing...");
        schedulerStreams.close();
    }

    public KafkaStreams getKafkaStreams() {
        return schedulerStreams;
    }
}
