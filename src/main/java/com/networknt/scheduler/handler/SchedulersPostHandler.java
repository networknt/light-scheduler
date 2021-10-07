package com.networknt.scheduler.handler;

import com.networknt.body.BodyHandler;
import com.networknt.config.Config;
import com.networknt.config.JsonMapper;
import com.networknt.handler.LightHttpHandler;
import com.networknt.scheduler.*;
import com.networknt.utility.TimeUtil;
import io.undertow.server.HttpServerExchange;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;

/**
For more information on how to write business handlers, please check the link below.
https://doc.networknt.com/development/business-handler/rest/
*/
public class SchedulersPostHandler implements LightHttpHandler {
    private static final Logger logger = LoggerFactory.getLogger(SchedulersPostHandler.class);
    private static final SchedulerConfig config = (SchedulerConfig) Config.getInstance().getJsonObjectConfig(SchedulerConfig.CONFIG_NAME, SchedulerConfig.class);

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        Map<String, Object> bodyMap = (Map<String, Object>)exchange.getAttachment(BodyHandler.REQUEST_BODY);
        if(logger.isDebugEnabled()) logger.debug("bodyMap = " + JsonMapper.toJson(bodyMap));
        TaskDefinitionKey taskDefinitionKey = TaskDefinitionKey.newBuilder()
                .setName((String)bodyMap.get("name"))
                .setHost((String)bodyMap.get("host"))
                .build();

        if(logger.isDebugEnabled()) logger.debug("Created task definition key {}", taskDefinitionKey);

        // Task frequency definition triggers the task every 3 sec once
        Map<String, Object> frequencyMap = (Map<String, Object>) bodyMap.get("frequency");
        TaskFrequency taskFrequency = TaskFrequency.newBuilder()
                .setTimeUnit(TimeUnit.valueOf((String)frequencyMap.get("timeUnit")))
                .setTime((Integer)frequencyMap.get("time"))
                .build();
        if(logger.isDebugEnabled()) logger.debug("Created task frequency key {}", taskFrequency);
        // start is in the body, then use it for future task schedule. Otherwise, use the current time.
        long start = System.currentTimeMillis();
        Long longStart = (Long)bodyMap.get("start");
        if(longStart != null) {
            start = longStart;
        }
        start = TimeUtil.nextStartTimestamp(java.util.concurrent.TimeUnit.valueOf(taskFrequency.getTimeUnit().name()), start);
        Map<CharSequence, CharSequence> dataMap = (Map<CharSequence, CharSequence>) bodyMap.get("data");
        TaskDefinition taskDefinition = TaskDefinition.newBuilder()
                .setName((String)bodyMap.get("name"))
                .setHost((String)bodyMap.get("host"))
                .setAction(DefinitionAction.valueOf((String)bodyMap.get("action")))
                .setTopic((String)bodyMap.get("topic"))
                .setStart(start)
                .setFrequency(taskFrequency)
                .setData(dataMap)
                .build();
        if(logger.isDebugEnabled()) logger.debug("Created task definition key {}", taskDefinition);

        ProducerRecord<TaskDefinitionKey, TaskDefinition> producerRecord = new ProducerRecord(config.getInputTopic(),
                taskDefinitionKey, taskDefinition);

        final CountDownLatch latch = new CountDownLatch(1);
        SchedulerStartupHook.producer.send(producerRecord, (recordMetadata, e) -> {
            if (Objects.nonNull(e)) {
                logger.error("Exception occurred while pushing the task definition", e);
            } else {
                logger.info("Task Definition record pushed successfully. Received Record Metadata is {}",
                        recordMetadata);
            }
            latch.countDown();
        });
        latch.await();

        exchange.setStatusCode(201);
        exchange.endExchange();
    }
}
