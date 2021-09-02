package com.networknt.scheduler.service;

import com.networknt.scheduler.SchedulerConstants;
import org.apache.kafka.streams.processor.ProcessorContext;

import java.util.concurrent.TimeUnit;

public class EveryDayTaskHandler extends AbstractTaskHandler {
    protected EveryDayTaskHandler(ProcessorContext processorContext, TimeUnit timeUnit) {
        super(processorContext, SchedulerConstants.EVERY_DAY_TASK_STORE, timeUnit);
    }
}
