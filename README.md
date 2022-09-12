# booking-cqrs-cassandra
CQRS with Event Sourcing, Scala, Akka and dockerized Cassandra

## Run cassandra on Docker Compose
```bash
cd /home/robert/sandbox/projects/booking-cqrs-cassandra
sudo docker-compose up
```

## Check cassandra tables (in other terminal)
```bash
cd /home/robert/sandbox/projects/booking-cqrs-cassandra

sudo docker ps
sudo docker exec -it <container_name_from_the_ps_command_above> cqlsh

cqlsh> describe tables;
cqlsh> exit;
```