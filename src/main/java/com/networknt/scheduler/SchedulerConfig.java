package com.networknt.scheduler;

public class SchedulerConfig {
    public static final String CONFIG_NAME = "scheduler";

    private String inputTopic;
    private boolean multiTenancy;


    public String getInputTopic() {
        return inputTopic;
    }

    public void setInputTopic(String inputTopic) {
        this.inputTopic = inputTopic;
    }

    public boolean isMultiTenancy() {
        return multiTenancy;
    }

    public void setMultiTenancy(boolean multiTenancy) {
        this.multiTenancy = multiTenancy;
    }
}
