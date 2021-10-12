# light-scheduler
A scalable event scheduler based on Kafka Streams and Interactive Queries

There are so many schedulers implemented in Java as open-source projects, and most of them are Java EE-based and cannot scale. Also, most of them contain a task executor to execute services in the same thread synchronously. It further reduces the throughput as a single node can only handle a limited workload. To archive high availability and fault tolerance, a centralized database will be used to persist the scheduled tasks, and the database will be s single point failure and bottleneck.
 

The light-scheduler is not a task schedule but an event scheduler. The scheduler cluster is not responsible for executing the tasks but just send events to an output Kafka topic which can be partitioned with up to thousands. Each partition can be handled by a task executor that can handle thousands of tasks per second. The limitation is the network card throughput of 10GB (over 1 million events assuming 1kb per event) in most cases per node. 

### Use cases

Light-scheduler is time-based with repeating Cron-like config. It can be used by a service to schedule an event that needs to be processed in the future. The service registers a TaskDefinition event in the scheduler, and the scheduler will periodically push a task execution event to a target topic based on the frequency. 

A task executor service will process the target topic with Kafka streams and execute the task in the streams processing. 

Here are some of the use cases: 

* Delayed - A job to be executed 30 minutes from now
* Retry - Schedule exponential backoff retry intervals of 1, 2, 4, 16, 256 and so on.
* Timeout - Schedule a timeout check event to be executed after 1 min
* Polling - Schedule a Cron job to be executed at some specified frequency
* Workflow - Distributed workflow with state suspensions and resumptions
* Batch jobs - This is the basic functionality of the enterprise scheduler
* Execution Window - Event must be scheduled in the next valid window. For example, from 9 to 5. If retry falls out of the execution window, it must be executed in the next execution window. 

### Requirement

* Master/Master cluster. Each node works independently on its partition, and this gives us the capability to scale linearly limited only by Kafka partitions. 
* Extremely fault-tolerant. If a node is down, another node will automatically pick up the partition in the same consumer group.
* Uniform Partitioning. Round-robin distribution of events to all partitions. 
* Uniform Execution. All nodes share a similar load in quantity.
* Multi-tenancy. Each event belongs to one tenant, and each tenant can define the rule for event distribution. 


### Design

The task definition events will be injected into a Kafka topic light-scheduler which is partitioned. Each event has a key (host and name combination) to decide which partition to injected to. You need to pick the key name from the entity id so that all event-related for one entity can be pushed to the same partition and handled in order if necessary. 

You can have many injection nodes as event producers, and usually, these are light-scheduler microservices instances. If you have applications that can access the Kafka cluster directly, you can directly produce the task definition events to the light-scheduler topic. 

For every partition, we can start a microservice instance to process the streams of the task definition events and put the task execution events into 1 millisecond, 1 second, 1 minute, 1 hour or 1 day bucket based on the scheduled time. The events will be pushed to a Kafka task execution topic.

The consumer node also constantly processes the task execution topic to check if any event needs to be executed. If there are, process the task execution event. 


##### Event Injection and Event Execution

By using Kafka, we have a distinct separation of event injection and execution so that each side can be scaled independently given the use cases and workload. 

There are two interfaces for the event injection: Kafka producer and REST API of light-scheduler.

For the event injection, the limitation is the throughput of Kafka producer, and it is linearly scalable. Of course, the limitation is the network throughput in the end. 

For the event execution, we have a light controller to execute the health check for registered services, as an example. We will provide a standalone service in the future as another example. 

We can push the execution event to one or more topics that some microservices subscribed with standard task executors. 

##### Chaining of Event

In some of the use cases, several related jobs must be executed in sequence or a pre-defined order. It might be too much for the scheduler to handle this at the injection phase; however, it would be easy to create microservices or just a customized executor to handle this kind of particular requirement. 

##### Logging, Tracing and Auditing

The entire inject/execute chain is transactional, and the status must be updated once the job is executed to close the loop. We also need to provide a dashboard to enable the user to monitor and report the scheduler's activities. 

##### File Watcher Injector

For most enterprise schedulers based on batch jobs, we need to support smooth migration. That means we need to have a special injector that can monitor specific directories in the filesystem and pick up a file containing a list of tasks. We need to define a standard format for the file and provide examples for convertor implementation that can be easily injected with service.yml config. Eventually, the events will be injected into the Kafka input topic through Kafka producer or a REST API. This particular injector can be built as a light-4j microservice or a daemon process. 

### Build and Start


To start the server locally, you need to have Kafka Confluent locally with input and output topics created. For more info about starting Confluent, please refer to this [doc](https://doc.networknt.com/tutorial/kafka-sidecar/confluent-local/). 

The project contains a single module. A fat jar server.jar will be generated in the target directory after running the build command below.

```
./mvnw clean install -Prelease
```

With the fatjar in the target directory, you can start the server with the following command.

```
java -jar target/light-scheduler.jar
```

To speed up the test, you can avoid the fat jar generation and start the server from Maven.

```
./mvnw clean install exec:exec
```

The above command line will start a single node with the default configuration. To start a three-node cluster locally, you can run the following three command lines in three terminals. 

Node1 

```
java -Dlight-4j-config-dir=config/node1 -jar target/light-scheduler.jar

```

Node2

```
java -Dlight-4j-config-dir=config/node2 -jar target/light-scheduler.jar

```

Node3

```
java -Dlight-4j-config-dir=config/node3 -jar target/light-scheduler.jar

```

### Test

Before runing the test, you need to start the confluent platform locally. You can use the docker-compose from the kafka-sidecar repository. 

```
cd ~/networknt/kafka-sidecar
docker-compose up -d
```

Create the following topics for the scheduler from the control center.

```
light-scheduler 16 partitions
controller-health-check 16 partitions
```

By default, the OAuth2 JWT security verification is disabled, so you can use Curl or Postman to test your service right after the server is started. For example, the petstore API has the following endpoint.

To add a new task definition. 

```
curl -k --location --request POST 'https://localhost:8443/schedulers' \
--header 'Content-Type: application/json' \
--data-raw '{
    "host": "networknt.com",
    "name": "market-192.168.1.1-health-check",
    "action": "INSERT",
    "frequency": {
      "timeUnit": "MINUTES",
      "time": 2
    },
    "topic": "controller-health-check",
    "data": {
      "key1": "value1",
      "key2": "value2"
    }
}'
```

To update an existing task definition

```
curl -k --location --request POST 'https://localhost:8443/schedulers' \
--header 'Content-Type: application/json' \
--data-raw '{
    "host": "networknt.com",
    "name": "market-192.168.1.1-health-check",
    "action": "UPDATE",
    "frequency": {
      "timeUnit": "MINUTES",
      "time": 2
    },
    "topic": "controller-health-check",
    "data": {
      "key1": "value1",
      "key2": "value2"
    }
}'

```

To delete an existing task definition

```
curl -k --location --request POST 'https://localhost:8443/schedulers' \
--header 'Content-Type: application/json' \
--data-raw '{
    "host": "networknt.com",
    "name": "market-192.168.1.1-health-check",
    "action": "DELETE",
    "frequency": {
      "timeUnit": "MINUTES",
      "time": 2
    },
    "topic": "controller-health-check",
    "data": {
      "key1": "value1",
      "key2": "value2"
    }
}'

```
For your API, you need to change the path to match your specifications.

### Tutorial

To explore more features, please visit the [light-scheduler tutorial](https://doc.networknt.com/tutorial/scheduler/).

