FROM java:8-jdk

MAINTAINER Massimo Battestini

LABEL description="News server for observo testing"

ADD build/distributions/observo-integration.tar .

EXPOSE 8080

CMD cd observo-integration; java -DzkConnectionString=$ZK_CONNECTION_STRING -cp observo-integration.jar:lib/* observointegration.NewsServer