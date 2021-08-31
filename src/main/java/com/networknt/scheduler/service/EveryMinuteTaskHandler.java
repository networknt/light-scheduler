package com.networknt.scheduler.service;

import com.networknt.scheduler.SchedulerConstants;
import org.apache.kafka.streams.processor.ProcessorContext;

import java.util.concurrent.TimeUnit;

final class EveryMinuteTaskHandler extends AbstractTaskHandler {
    protected EveryMinuteTaskHandler(ProcessorContext processorContext, TimeUnit timeUnit) {
        super(processorContext, SchedulerConstants.EVERY_MIN_TASK_STORE, timeUnit);
    }
}
