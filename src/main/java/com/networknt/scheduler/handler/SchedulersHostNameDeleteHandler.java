package com.networknt.scheduler.handler;

import com.networknt.body.BodyHandler;
import com.networknt.config.Config;
import com.networknt.scheduler.TaskDefinitionKey;
import com.networknt.scheduler.service.SchedulersHostNameDeleteService;
import com.networknt.handler.LightHttpHandler;
import com.networknt.http.HttpMethod;
import com.networknt.http.RequestEntity;
import com.networknt.http.ResponseEntity;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;

import java.util.Deque;
import java.util.Map;

/**
For more information on how to write business handlers, please check the link below.
https://doc.networknt.com/development/business-handler/rest/
*/
public class SchedulersHostNameDeleteHandler implements LightHttpHandler {
    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        String host = exchange.getPathParameters().get("host").getFirst();
        String name = exchange.getPathParameters().get("name").getFirst();
        TaskDefinitionKey taskDefinitionKey = TaskDefinitionKey.newBuilder()
                .setName(name)
                .setHost(host)
                .build();

        exchange.setStatusCode(200);
        exchange.getResponseSender().send("");
    }
}
