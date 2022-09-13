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

## How this works ?

- HotelDemo - is acting as Command provider (Write part of CQRS). Commands are translated into Events and persisted on Cassandra (Akka Persistence)

- HotelEventReader - should be running constantly in the background. This is Read part of CQRS. It is constantly streaming events to a new DB model (just once each event) and then any client might run as many read from Cassandra as they want - it won't slow down the Writing because these are different tables (might be event different DB). 
