package com.networknt.scheduler.transformer;

import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Transformer;
import org.apache.kafka.streams.kstream.TransformerSupplier;
import com.networknt.scheduler.TaskDefinition;
import com.networknt.scheduler.TaskDefinitionKey;

public class TaskSchedulingTransformerSupplier implements TransformerSupplier<TaskDefinitionKey, TaskDefinition,
        KeyValue<TaskDefinitionKey, TaskDefinition>> {
    @Override
    public Transformer<TaskDefinitionKey, TaskDefinition, KeyValue<TaskDefinitionKey, TaskDefinition>> get() {
        return new TaskSchedulingTransformer();
    }
}
