package com.networknt.scheduler.service;

import com.networknt.utility.TimeUtil;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.processor.PunctuationType;
import org.apache.kafka.streams.processor.Punctuator;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.KeyValueStore;
import com.networknt.scheduler.TaskDefinition;
import com.networknt.scheduler.TaskDefinitionKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public abstract class AbstractTaskHandler implements TaskHandler, Punctuator {
    private final static Logger logger = LoggerFactory.getLogger(AbstractTaskHandler.class);

    private final ProcessorContext processorContext;
    private KeyValueStore<TaskDefinitionKey, TaskDefinition> taskDefinitionStore;
    private final TimeUnit timeUnit;

    @SuppressWarnings("unchecked")
    protected AbstractTaskHandler(final ProcessorContext processorContext,
                                  final String storeName, final TimeUnit timeUnit) {
        this.processorContext = processorContext;
        if(logger.isInfoEnabled()) logger.info("Looking for the store name {}", storeName);
        this.taskDefinitionStore = (KeyValueStore<TaskDefinitionKey, TaskDefinition>) processorContext
                .getStateStore(storeName);

        this.timeUnit = timeUnit;

        this.processorContext.schedule(getDuration(timeUnit), PunctuationType.WALL_CLOCK_TIME, this);
    }

    protected static Duration getDuration(TimeUnit timeUnit) {
        Duration duration = null;
        switch (timeUnit) {
            case MILLISECONDS:
                duration = Duration.ofMillis(1);
                break;

            case SECONDS:
                duration = Duration.ofSeconds(1);
                break;

            case MINUTES:
                duration = Duration.ofMinutes(1);
                break;

            case HOURS:
                duration = Duration.ofHours(1);
                break;

            case DAYS:
                duration = Duration.ofDays(1);
                break;
        }

        return duration;
    }

    @Override
    public TimeUnit handlingDuration() {
        return timeUnit;
    }

    @Override
    public void add(TaskDefinitionKey taskDefinitionKey, TaskDefinition taskDefinition) {
        taskDefinitionStore.put(taskDefinitionKey, taskDefinition);
    }

    @Override
    public TaskDefinition get(TaskDefinitionKey taskDefinitionKey) {
        return taskDefinitionStore.get(taskDefinitionKey);
    }

    @Override
    public TaskDefinition delete(TaskDefinitionKey taskDefinitionKey) {
        return taskDefinitionStore.delete(taskDefinitionKey);
    }

    @Override
    public void punctuate(long l) {
        final KeyValueIterator<TaskDefinitionKey, TaskDefinition> all = taskDefinitionStore.all();
        while (all.hasNext()) {
            final KeyValue<TaskDefinitionKey, TaskDefinition> next = all.next();
            // round both start and current to the next TimeUnit.
            long start = TimeUtil.nextStartTimestamp(TimeUnit.valueOf(next.value.getFrequency().getTimeUnit().name()), next.value.getStart());
            long current = TimeUtil.nextStartTimestamp(TimeUnit.valueOf(next.value.getFrequency().getTimeUnit().name()), l);
            if(current - start > 0L) {
                long freq = TimeUtil.oneTimeUnitMillisecond(timeUnit) * next.value.getFrequency().getTime();
                if(logger.isTraceEnabled()) logger.trace("start timestamp = " + start + " current timestamp = " + current + " period millisecond = " + freq);
                if((current-start) % freq == 0) {
                    if(logger.isDebugEnabled()) logger.debug("{} - Triggering task Key: {}, Value: {}", timeUnit, next.key, next.value);
                    // set the start timestamp to give executor detect the task age for skipping.
                    next.value.setStart(current);
                    this.processorContext.forward(next.key, next.value);
                }
            }
        }
        all.close();
    }
}

