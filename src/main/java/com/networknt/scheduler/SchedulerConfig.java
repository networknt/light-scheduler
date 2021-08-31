package com.networknt.scheduler;

public class SchedulerConfig {
    public static final String CONFIG_NAME = "scheduler";

    private String inputTopic;

    public String getInputTopic() {
        return inputTopic;
    }

    public void setInputTopic(String inputTopic) {
        this.inputTopic = inputTopic;
    }
}
