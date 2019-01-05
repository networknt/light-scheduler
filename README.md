# light-scheduler
A scalable event scheduler based on transactional Kafka streams and interactive queries

There are so many schedulers implemented in Java as open source projects, and most of them are Java EE based and cannot scale. Also, most of them contain a task executor to execute services in the same thread synchronously. It further reduces the throughput as a single node can only handle a limited workload. 

The light-scheduler is not a task schedule but an event scheduler. The scheduler cluster is not responsible for executing the tasks but just send events to an output Kafka topic which can be partitioned with up to thousands. Each partition can be handled by a task executor that can handle thousands of tasks per second. The limitation is the network card throughput 10GB (over 1 million events assuming 1kb per event) most of the cases per node. 


### Use cases

Light-scheduler is time-based with both single trigger and repeating Cron like config. It can be used by a service to schedule a request that needs to be processed in the future. The service registers an event in the scheduler and suspends the processing of the current request. 

When the time arrives the requesting service is notified by the scheduler and former can resume processing of the suspended request.

Here are some of the use cases: 

* Delayed - A job to be executed 30 minutes from now
* Retry - Schedule an exponential backoff retry intervals of 1, 2, 4, 16, 256 and so on.
* Timeout - Schedule a timeout check event to be executed after 1 min
* Polling - Schedule a Cron job to be executed at some specified frequency
* Workflow - Distributed workflow with state suspensions and resumptions
* Batch jobs - This is the basic functionality of enterprise scheduler


### Requirement

* Master/Master cluster. Each node works independently on its own partition. This gives us the capability to scale linearly limited only by Kafka partitions. 
* Extremely fault tolerant. If a node is down, the partition will be picked up by another node in the same consumer group automatically.
* Uniform Partitioning. Round-robin distribution of events to all partitions. 
* Uniform Execution. All nodes share a similar load in quantity.
* Multi-tenancy. Each event belongs to one tenant, and each tenant can define the rule for event distribution. 
* Exact once delivery. All events are processed in a transactional context and guarantee to delivery exactly one. 



### Design

The events will be injected to a Kafka topic which is partitioned. Each event has a key to decide which partition to injected to. You need to pick the key from entity id or so that all event-related for one entity can be pushed to the same partition and handled in order if necessary. 

You can have many injection nodes as event producers, and usually, these are microservices instances. 

For every partition, we start a microservice instance to process the streams of the event and put these events into a 1-minute bucket based on the scheduled time. The events will be saved in a key/value store on each node with the time is the key and a list of the events as value. 

For the consumer node, it also constantly scan the key/value store per minute to check if there is any event that needs to be delivered. If there are send all the event schedule at that minute to the delivery topic. 

If delivery fails, the scanner will reschedule the delivery again in the next minute. It is configurable on how many minutes to scan in the past. 


