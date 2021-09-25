package com.networknt.scheduler.service;

import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.processor.ProcessorContext;
import com.networknt.scheduler.TaskDefinition;
import com.networknt.scheduler.TaskDefinitionKey;
import com.networknt.scheduler.TimeUnit;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TaskManager {
    private final ProcessorContext processorContext;

    private final ConcurrentHashMap<TimeUnit, TaskHandler> taskHandlers;

    public TaskManager(final ProcessorContext processorContext) {
        this.processorContext = processorContext;

        this.taskHandlers = new ConcurrentHashMap();

        this.taskHandlers.put(TimeUnit.MILLISECONDS, TaskHandler.taskHandler(processorContext, java.util.concurrent.TimeUnit.MILLISECONDS));
        this.taskHandlers.put(TimeUnit.SECONDS, TaskHandler.taskHandler(processorContext, java.util.concurrent.TimeUnit.SECONDS));
        this.taskHandlers.put(TimeUnit.MINUTES, TaskHandler.taskHandler(processorContext, java.util.concurrent.TimeUnit.MINUTES));
        this.taskHandlers.put(TimeUnit.HOURS, TaskHandler.taskHandler(processorContext, java.util.concurrent.TimeUnit.HOURS));
        this.taskHandlers.put(TimeUnit.DAYS, TaskHandler.taskHandler(processorContext, java.util.concurrent.TimeUnit.DAYS));
    }

    public KeyValue<TaskDefinitionKey, TaskDefinition> handle(final TaskDefinitionKey taskDefinitionKey,
                           final TaskDefinition taskDefinition) {
        final TimeUnit frequencyTimeUnit = taskDefinition.getFrequency().getTimeUnit();
        TaskHandler handler = null;
        switch (frequencyTimeUnit) {
            case DAYS:
                handler = this.taskHandlers.get(TimeUnit.DAYS);
                break;

            case HOURS:
                handler = this.taskHandlers.get(TimeUnit.HOURS);
                break;

            case MINUTES:
                handler = this.taskHandlers.get(TimeUnit.MINUTES);
                break;

            case SECONDS:
                handler = this.taskHandlers.get(TimeUnit.SECONDS);
                break;

            case MILLISECONDS:
                handler = this.taskHandlers.get(TimeUnit.MILLISECONDS);
                break;
        }

        switch (taskDefinition.getAction()) {
            case INSERT:
                handler.add(taskDefinitionKey, taskDefinition);
                break;
            case UPDATE:
                handler.delete(taskDefinitionKey);
                handler.add(taskDefinitionKey, taskDefinition);
                break;
            case DELETE:
                handler.delete(taskDefinitionKey);
                break;
        }
        return KeyValue.pair(taskDefinitionKey, taskDefinition);
    }

    public Map<TimeUnit, TaskHandler> getTaskHandlers() {
        return taskHandlers;
    }
}
