package com.networknt.scheduler.service;

import com.networknt.scheduler.SchedulerConstants;
import org.apache.kafka.streams.processor.ProcessorContext;

import java.util.concurrent.TimeUnit;

final class EverySecondsTaskHandler extends AbstractTaskHandler {
    protected EverySecondsTaskHandler(ProcessorContext processorContext, TimeUnit timeUnit) {
        super(processorContext, SchedulerConstants.EVERY_SECONDS_TASK_STORE, timeUnit);
    }
}
