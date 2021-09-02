package com.networknt.scheduler.service;

import com.networknt.scheduler.SchedulerConstants;
import org.apache.kafka.streams.processor.ProcessorContext;

import java.util.concurrent.TimeUnit;

public class EveryHourTaskHandler extends AbstractTaskHandler {
    protected EveryHourTaskHandler(ProcessorContext processorContext, TimeUnit timeUnit) {
        super(processorContext, SchedulerConstants.EVERY_HOUR_TASK_STORE, timeUnit);
    }
}
