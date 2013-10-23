#!/bin/bash
pubIP=`wget -q -O - http://169.254.169.254/latest/meta-data/public-hostname`
instId=`wget -q -O - http://169.254.169.254/latest/meta-data/instance-id`
su ubuntu -c "cd; echo $instId $pubIP 0 > nodeData.txt"
#	su ubuntu -c 'cd; zookeeper-3.4.3/bin/zkServer.sh start galaxy-example/src/main/resources/config/zoo.cfg > galaxy.log'
#	su ubuntu -c 'cd; echo galaxy.zkServers=127.0.0.1:2181 >> galaxy-example/src/main/resources/config/server.properties'
#	su ubuntu -c 'cd; cd galaxy-example; ./gradlew server &>> ../galaxy.log' &
