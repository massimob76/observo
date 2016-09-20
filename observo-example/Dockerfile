FROM java:8-jdk

MAINTAINER Massimo Battestini

LABEL description="News server for observo testing"

ADD build/distributions/observo-example.tar .

EXPOSE 8080

CMD cd observo-example; java -DzkConnectionString=$ZK_CONNECTION_STRING -cp observo-example.jar:lib/* observoexample.NewsServer