package com.networknt.scheduler.handler;

import com.networknt.scheduler.service.SchedulersIdDeleteService;
import com.networknt.handler.LightHttpHandler;
import com.networknt.http.HttpMethod;
import com.networknt.http.RequestEntity;
import com.networknt.http.ResponseEntity;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;

import java.util.Deque;
import java.util.Map;

/**
 * Delete a task definition by producing a new entry with the key but empty value.
 *
 * @author Steve Hu
*/
public class SchedulersIdDeleteHandler implements LightHttpHandler {

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        exchange.setStatusCode(201);
        exchange.getResponseSender().send("OK");
    }
}
