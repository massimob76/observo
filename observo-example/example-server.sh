#!/usr/bin/env bash

function startServers {
    docker-compose scale server=$1 zookeeper=1
}

function stopServers {
    docker-compose stop
}

function startSpecificServer {
    instance=$1
    docker start $(getServerName $instance)
}

function stopSpecificServer {
    instance=$1
    docker stop $(getServerName $instance)
}

function retrievePorts {
    serverInstances=$1
    ports=""
    for ((i=1;i<=serverInstances;i++)); do
        server_name=$(getServerName $i)
        port=$(cut -d ":" -f 2 <<< "$(docker port $server_name 8080)")
        ports=$"$ports$port "
    done
    echo "$ports"
}

function getServerName {
    echo "observoexample_server_$1"
}

command=$1

case $command in
start)
    noOfInstances=$2
    echo "starting $noOfInstances servers"
    startServers $noOfInstances
    ;;
stop)
    echo "stopping servers"
    stopServers
    ;;
startSpecificServer)
    serverInstance=$2
    echo "starting server instance $serverInstance"
    startSpecificServer $serverInstance
    ;;
stopSpecificServer)
    serverInstance=$2
    echo "stopping server instance $serverInstance"
    stopSpecificServer $serverInstance
    ;;
ports)
    instances=$2
    echo "$(retrievePorts instances)"
    ;;
*)
    echo "Error, wrong argument!!!"
    echo "$0 [start|stop|startSpecificServer|stopSpecificServer|ports] [instances]"
    ;;
esac
