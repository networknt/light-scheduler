package com.networknt.scheduler.handler;

import com.networknt.client.Http2Client;
import com.networknt.config.Config;
import com.networknt.config.JsonMapper;
import com.networknt.kafka.common.AvroConverter;
import com.networknt.monad.Failure;
import com.networknt.monad.Result;
import com.networknt.monad.Success;
import com.networknt.scheduler.*;
import com.networknt.handler.LightHttpHandler;
import com.networknt.server.Server;
import com.networknt.status.Status;
import com.networknt.utility.NetUtils;
import io.undertow.UndertowOptions;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.errors.InvalidStateStoreException;
import org.apache.kafka.streams.state.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.OptionMap;

import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This is the endpoint to get all the schedule definitions in each TimeUnit stores backed by the
 * Kafka changelog topics. It has three optional parameters to filter the result. They are host,
 * name, unit.
 *
 * In a multi-tenancy mode, a host header must be in the HTTP header to filter the result for a
 * particular host only. Or a host will be retrieved from the JWT token as a custom claim. The
 * final implementation is pending.
 *
 * @author Steve Hu
*/
public class SchedulersGetHandler implements LightHttpHandler {
    private static final Logger logger = LoggerFactory.getLogger(SchedulersGetHandler.class);

    static Http2Client client = Http2Client.getInstance();
    static Map<String, ClientConnection> connCache = new ConcurrentHashMap<>();
    static final String GENERIC_EXCEPTION = "ERR10014";
    static final long WAIT_THRESHOLD = 30000;

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        String host = null;
        Deque<String> hostDeque = exchange.getQueryParameters().get("host");
        if(hostDeque != null)  host = hostDeque.getFirst();

        String name = null;
        Deque<String> nameDeque = exchange.getQueryParameters().get("name");
        if(nameDeque != null)  name = nameDeque.getFirst();

        String unit = null;
        Deque<String> unitDeque = exchange.getQueryParameters().get("unit");
        if(unitDeque != null)  unit = unitDeque.getFirst();

        boolean local = false;
        Deque<String> localDeque = exchange.getQueryParameters().get("local");
        if(localDeque != null && !localDeque.isEmpty()) local = true;
        if(local) {
            exchange.getResponseSender().send(JsonMapper.toJson(getLocalDefinitions(host, name, unit)));
        } else {
            exchange.getResponseSender().send(JsonMapper.toJson(getClusterDefinition(exchange, host, name, unit)));
        }
    }

    public static List<Map<String, Object>> getClusterDefinition(HttpServerExchange exchange, String host, String name, String unit) {
        KafkaStreams kafkaStreams = SchedulerStartupHook.streams.getKafkaStreams();
        Collection<StreamsMetadata> metadataList = kafkaStreams.allMetadata();
        List<Map<String, Object>> definitions = new ArrayList<>();
        for (StreamsMetadata metadata : metadataList) {
            if (logger.isDebugEnabled()) logger.debug("found one address in the collection " + metadata.host() + ":" + metadata.port());
            String url = "https://" + metadata.host() + ":" + metadata.port();
            if (NetUtils.getLocalAddressByDatagram().equals(metadata.host()) && Server.getServerConfig().getHttpsPort() == metadata.port()) {
                definitions.addAll(getLocalDefinitions(host, name, unit));
            } else {
                // remote store through API access.
                Result<String> resultDefinitions = getTaskDefinitions(exchange, url, host, name, unit);
                if (resultDefinitions.isSuccess()) {
                    definitions.addAll(JsonMapper.string2List(resultDefinitions.getResult()));
                }
            }
        }
        return definitions;
    }

    public static List<Map<String, Object>> getLocalDefinitions(String host, String name, String unit) {
        List<Map<String, Object>> definitions = new ArrayList<>();

        // local store access based on the filters.
        KafkaStreams kafkaStreams = SchedulerStartupHook.streams.getKafkaStreams();
        QueryableStoreType<ReadOnlyKeyValueStore<TaskDefinitionKey, TaskDefinition>> queryableStoreType = QueryableStoreTypes.keyValueStore();
        for (String storeName : SchedulerConstants.TASK_STORES) {

            StoreQueryParameters<ReadOnlyKeyValueStore<TaskDefinitionKey, TaskDefinition>> sqp = StoreQueryParameters.fromNameAndType(storeName, queryableStoreType);
            ReadOnlyKeyValueStore<TaskDefinitionKey, TaskDefinition> store = kafkaStreams.store(sqp);
            KeyValueIterator<TaskDefinitionKey, TaskDefinition> iterator = (KeyValueIterator<TaskDefinitionKey, TaskDefinition>) SchedulerStartupHook.streams.getAllKafkaValue(store);
            
            while(iterator.hasNext()) {
                KeyValue<TaskDefinitionKey, TaskDefinition> keyValue = iterator.next();
                TaskDefinitionKey key = keyValue.key;
                TaskDefinition value = keyValue.value;
                if(host != null && !host.equals(key.getHost())) {
                    continue;
                }
                if(name != null && !name.equals(key.getName())) {
                    continue;
                }
                if(unit != null && !value.getFrequency().getTimeUnit().equals(TimeUnit.valueOf(unit))) {
                    continue;
                }
                definitions.add(JsonMapper.string2Map(AvroConverter.toJson(value, false)));
            }
            iterator.close();
        }
        if(logger.isDebugEnabled()) logger.debug("The number of definitions at local is " + definitions.size());
        return definitions;
    }

    /**
     * Get task definitions from other nodes with exchange, url and filters. The result contains a map of task definitions.
     *
     * @param exchange HttpServerExchange
     * @param url of the target server
     * @param host host filter
     * @param name name filter
     * @param unit unit filter
     * @return Result the definitions in JSON
     */
    private static Result<String> getTaskDefinitions(HttpServerExchange exchange, String url, String host, String name, String unit) {
        return callQueryExchangeUrl(exchange, url, host, name, unit);
    }

    private static Result<String> callQueryExchangeUrl(HttpServerExchange exchange, String url, String host, String name, String unit) {
        Result<String> result = null;
        try {
            ClientConnection conn = connCache.get(url);
            if(conn == null || !conn.isOpen()) {
                conn = client.connect(new URI(url), Http2Client.WORKER, client.getDefaultXnioSsl(), Http2Client.BUFFER_POOL, OptionMap.create(UndertowOptions.ENABLE_HTTP2, true)).get();
                connCache.put(url, conn);
            }
            // Create one CountDownLatch that will be reset in the callback function
            final CountDownLatch latch = new CountDownLatch(1);
            // Create an AtomicReference object to receive ClientResponse from callback function
            final AtomicReference<ClientResponse> reference = new AtomicReference<>();
            String message = "/schedulers?local=true";
            if(host != null && host.length() > 0) {
                message = message + "&host=" + host;
            }
            if(name != null && name.length() > 0) {
                message = message + "&name=" + name;
            }
            if(unit != null && unit.length() > 0) {
                message = message + "&unit=" + unit;
            }
            final ClientRequest request = new ClientRequest().setMethod(Methods.GET).setPath(message);
            String token = exchange.getRequestHeaders().getFirst(Headers.AUTHORIZATION);
            if(token != null) request.getRequestHeaders().put(Headers.AUTHORIZATION, token);
            request.getRequestHeaders().put(Headers.HOST, "localhost");
            conn.sendRequest(request, client.createClientCallback(reference, latch));
            latch.await();
            int statusCode = reference.get().getResponseCode();
            String body = reference.get().getAttachment(Http2Client.RESPONSE_BODY);
            if(statusCode != 200) {
                Status status = Config.getInstance().getMapper().readValue(body, Status.class);
                result = Failure.of(status);
            } else result = Success.of(body);
        } catch (Exception e) {
            logger.error("Exception:", e);
            Status status = new Status(GENERIC_EXCEPTION, e.getMessage());
            result = Failure.of(status);
        }
        return result;
    }
}
