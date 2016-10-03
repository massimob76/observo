#!/usr/bin/env bash

function startServers {
    noOfServers=$1
    docker-compose up -d
    docker-compose scale server=$noOfServers

    ports=""
    for ((i=1;i<=noOfServers;i++)); do
        ports=$"$ports$(retrievePort $i) "
    done
    echo "$ports"
}

function stopServers {
    docker-compose stop
}

function startSpecificServer {
    instance=$1
    docker start $(getServerName $instance)
    echo "$(retrievePort $instance)"
}

function stopSpecificServer {
    instance=$1
    docker stop $(getServerName $instance)
}

function retrievePort {
    server_name=$(getServerName $1)
    port=$(cut -d ":" -f 2 <<< "$(docker port $server_name 8080)")
    echo $port
}

function getServerName {
    echo "observointegration_server_$1"
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
*)
    echo "Error, wrong argument!!!"
    echo "$0 [start|stop|startSpecificServer|stopSpecificServer] [instances]"
    ;;
esac
