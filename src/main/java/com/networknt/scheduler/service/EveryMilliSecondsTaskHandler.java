package com.networknt.scheduler.service;

import com.networknt.scheduler.SchedulerConstants;
import org.apache.kafka.streams.processor.ProcessorContext;

import java.util.concurrent.TimeUnit;

final class EveryMilliSecondsTaskHandler extends AbstractTaskHandler {
    EveryMilliSecondsTaskHandler(ProcessorContext processorContext, TimeUnit timeUnit) {
        super(processorContext, SchedulerConstants.EVERY_MILLISECONDS_TASK_STORE, timeUnit);
    }

}
