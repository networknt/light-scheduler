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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;

/**
 * This is the endpoint to insert, update or delete a task definition from the light-scheduler. For update and
 * delete action, it will check if the task definition exists before updating the store.
 *
 * @author Steve Hu
*/
public class SchedulersPostHandler implements LightHttpHandler {
    private static final Logger logger = LoggerFactory.getLogger(SchedulersPostHandler.class);
    private static final SchedulerConfig config = (SchedulerConfig) Config.getInstance().getJsonObjectConfig(SchedulerConfig.CONFIG_NAME, SchedulerConfig.class);
    private static final String OBJECT_NOT_FOUND = "ERR11637";

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        Map<String, Object> bodyMap = (Map<String, Object>)exchange.getAttachment(BodyHandler.REQUEST_BODY);
        if(logger.isDebugEnabled()) logger.debug("bodyMap = " + JsonMapper.toJson(bodyMap));
        DefinitionAction action = DefinitionAction.valueOf((String)bodyMap.get("action"));
        String name = (String)bodyMap.get("name");
        String host = (String)bodyMap.get("host");
        Map<String, Object> frequencyMap = (Map<String, Object>) bodyMap.get("frequency");
        String unit = (String)frequencyMap.get("timeUnit");
        if(action == DefinitionAction.DELETE || action == DefinitionAction.UPDATE) {
            // need to make sure that the updated or deleted task definition exists. Otherwise, return error.
            List<Map<String, Object>> definitions = SchedulersGetHandler.getClusterDefinition(exchange, host, name, unit);
            if(definitions == null || definitions.size() == 0) {
                setExchangeStatus(exchange, OBJECT_NOT_FOUND, "task definition", host + " " + name + " " + unit);
                return;
            }
        }
        TaskDefinitionKey taskDefinitionKey = TaskDefinitionKey.newBuilder()
                .setName(name)
                .setHost(host)
                .build();

        if(logger.isDebugEnabled()) logger.debug("Created task definition key {}", taskDefinitionKey);

        // Task frequency definition triggers the task every 3 sec once
        TaskFrequency taskFrequency = TaskFrequency.newBuilder()
                .setTimeUnit(TimeUnit.valueOf(unit))
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
        Map<String, String> dataMap = (Map<String, String>) bodyMap.get("data");
        TaskDefinition taskDefinition = TaskDefinition.newBuilder()
                .setName(name)
                .setHost(host)
                .setAction(action)
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
