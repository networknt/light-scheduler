package com.networknt.scheduler;

import org.apache.kafka.streams.processor.RecordContext;
import org.apache.kafka.streams.processor.TopicNameExtractor;

public class TargetTopicNameExtractor implements TopicNameExtractor<TaskDefinitionKey, TaskDefinition> {
    @Override
    public String extract(final TaskDefinitionKey taskDefinitionKey,
                          final TaskDefinition taskDefinition,
                          final RecordContext recordContext) {
        return taskDefinition.getTopic();
    }
}
