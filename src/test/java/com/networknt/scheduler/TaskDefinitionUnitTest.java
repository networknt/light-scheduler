package com.networknt.scheduler;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class TaskDefinitionUnitTest {
    @Test
    public void testTaskFrequencyCreation() {
        // Task frequency definition triggers the task every 3 min once
        TaskFrequency taskFrequency = TaskFrequency.newBuilder()
                .setTimeUnit(TimeUnit.MINUTES)
                .setTime(3)
                .build();
    }

    @Test
    public void taskDefinitionCreation() {
        // Task frequency definition triggers the task every 3 sec once
        TaskFrequency taskFrequency = TaskFrequency.newBuilder()
                .setTimeUnit(TimeUnit.SECONDS)
                .setTime(3)
                .build();

        TaskDefinition taskDefinition = TaskDefinition.newBuilder()
                .setName("petstore-health-check")
                .setHost("networknt.com")
                .setAction(DefinitionAction.INSERT)
                .setTopic("health-check")
                .setStart(System.currentTimeMillis())
                .setFrequency(taskFrequency)
                .build();
    }

    @Test
    public void taskDefinitionCreationWithConfigAndData() {
        // Task frequency definition triggers the task every 3 sec once
        TaskFrequency taskFrequency = TaskFrequency.newBuilder()
                .setTimeUnit(TimeUnit.SECONDS)
                .setTime(3)
                .build();

        Map<CharSequence, CharSequence> configData = new HashMap();
        configData.put("customerId", "10000");

        Map<CharSequence, CharSequence> data = new HashMap();
        configData.put("customerId", "10000");

        TaskDefinition taskDefinition = TaskDefinition.newBuilder()
                .setName("petstore-health-check")
                .setHost("networknt.com")
                .setAction(DefinitionAction.INSERT)
                .setTopic("health-check")
                .setStart(System.currentTimeMillis())
                .setData(data)
                .setFrequency(taskFrequency)
                .build();
    }

    @Test
    public void taskDefinitionCreationWithoutFrequency() {
        TaskDefinition taskDefinition = TaskDefinition.newBuilder()
                .setName("petstore-health-check")
                .setHost("networknt.com")
                .setAction(DefinitionAction.INSERT)
                .setTopic("health-check")
                .setStart(System.currentTimeMillis())
                .build();
    }
}
