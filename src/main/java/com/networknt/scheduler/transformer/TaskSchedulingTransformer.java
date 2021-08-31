package com.networknt.scheduler.transformer;

import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Transformer;
import org.apache.kafka.streams.processor.ProcessorContext;
import com.networknt.scheduler.TaskDefinition;
import com.networknt.scheduler.TaskDefinitionKey;
import com.networknt.scheduler.service.TaskManager;

public class TaskSchedulingTransformer implements Transformer<TaskDefinitionKey, TaskDefinition,
        KeyValue<TaskDefinitionKey, TaskDefinition>> {

    private ProcessorContext processorContext;
    private TaskManager taskManager;

    @Override
    public void init(final ProcessorContext processorContext) {
        this.processorContext = processorContext;
        this.taskManager = new TaskManager(processorContext);
    }

    @Override
    public KeyValue<TaskDefinitionKey, TaskDefinition> transform(final TaskDefinitionKey taskDefinitionKey,
                                                                 final TaskDefinition taskDefinition) {
        return taskManager.handle(taskDefinitionKey, taskDefinition);
    }

    @Override
    public void close() {
    }
}
