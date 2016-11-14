# Observo Integration
This is just an example on how the ***Observo*** library can be used.
It creates a **news** server which is capable of:
* receive incoming news
* return the latest news received by this same server or by another server connected to the same zookeeper instance with *observo*
* return all the news it has received since start up (by this or another server)

## Dependencies
You will need to have an instance of zookeeper running

## How to run it
```sh
$ ./gradlew run -DzkConnectionString=localhost:2181
```
replace the **zkConnectionString** as opportune pointing to your running zookeeper instance.
If *zkConnectionString* is omitted it will default to *localhost:2181*

### Publish a news
```sh
curl -H "Content-Type: application/json" -X POST -d '{"title":"title-new","content":"content-new"}' http://localhost:8080/news/publish-async
```

### Retrieve last received news
```sh
curl http://localhost:8080/news/latest
```

### Retrieve all the received news
```sh
curl http://localhost:8080/news/all
```