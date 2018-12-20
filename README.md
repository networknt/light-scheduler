# light-scheduler
A scalable event scheduler based on transactional Kafka streams and interactive queries

There are so many schedulers implemented in Java as open source projects, and most of them are Java EE based and cannot scale. Also, most of them contain a task executor to execute services in the same thread synchronously. It further reduces the throughput as a single node can only handle a limited workload. 

### Use cases

Light-scheduler is time-based with both single trigger and repeating Cron like config. 

Here are some of the use cases: 

* Delayed - A job to be executed 30 minutes from now
* Retry - Schedule an exponential backoff retry intervals of 1, 2, 4, 16, 256 and so on.
* Timeout - Schedule a timeout check event to be executed after 1 min
* Polling - Schedule a Cron job to be executed at some specified frequency
* Workflow - Distributed workflow with state suspensions and resumptions


### Design


