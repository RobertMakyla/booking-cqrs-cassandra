# booking-cqrs-cassandra
CQRS with Event Sourcing, Scala, Akka and dockerized Cassandra

## run cassandra
```bash
cd /home/robert/sandbox/projects/booking-cqrs-cassandra
sudo docker-compose up
```
## check cassandra tables (in other terminal)

```bash
cd /home/robert/sandbox/projects/booking-cqrs-cassandra

sudo docker ps
sudo docker exec -it <container_name> cqlsh

cqlsh> describe tables;
cqlsh> exit;
```