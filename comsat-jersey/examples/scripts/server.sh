#!/bin/bash -f
#ami-5b527c1e ami-fe002cbb
cat > .targetServer.sh <<-EOF
	#!/bin/bash
	pubIP=\`wget -q -O - http://169.254.169.254/latest/meta-data/public-hostname\`
	instId=\`wget -q -O - http://169.254.169.254/latest/meta-data/instance-id\`
	#ifconfig eth0 mtu 4096
	#apt-get install unzip
	#su ubuntu -c "cd; wget https://github.com/puniverse/galaxy-integration/archive/training.zip; unzip training.zip; rm training.zip; rm -rf galaxy-example; mv galaxy-integration-training galaxy-example;"
	#su ubuntu -c "cd; wget https://github.com/puniverse/galaxy/archive/training.zip; unzip training.zip; rm training.zip; cd galaxy-training; ./gradlew jar &> ../galaxy.log; cp build/dist/galaxy-1.0-SNAPSHOT.jar ~/galaxy-example/baselib"
	su ubuntu -c "cd; echo \$instId \$pubIP 0 > nodeData.txt"
	#su ubuntu -c 'cd; mkdir .bdb;'
	su ubuntu -c 'cd; zookeeper-3.4.3/bin/zkServer.sh start galaxy-example/src/main/resources/config/zoo.cfg > galaxy.log'
	su ubuntu -c 'cd; echo galaxy.zkServers=127.0.0.1:2181 >> galaxy-example/src/main/resources/config/server.properties'
	#su ubuntu -c "cd; cd galaxy-example; sed 's/src\/config/src\/main\/resources\/config/g' build.gradle > temp; mv temp build.gradle"
	#su ubuntu -c 'cd; echo co.paralleluniverse.galaxy.core.MainMemory.level=FINE >> galaxy-example/src/main/resources/config/jul.properties'
	#su ubuntu -c 'cd; echo co.paralleluniverse.galaxy.netty.UDPComm.level=FINE >> galaxy-example/src/main/resources/config/jul.properties'
	su ubuntu -c 'cd; cd galaxy-example; ./gradlew server &>> ../galaxy.log' &
EOF
chmod +x .targetServer.sh
image=`cat .imageID`
#instance=`ec2run $image -k ec2key2 -g quicklaunch-1 -t m1.medium --user-data-file .targetServer.sh | awk '/INS/{print $2}'`
instance=`ec2run $image -k ec2key2 -g quicklaunch-1 -t c1.xlarge --user-data-file .targetServer.sh | awk '/INS/{print $2}'`
echo instance is $instance
if [ -z "$instance" ]
then
	exit 1
fi
echo waiting for server to come up...
sleep 10

for i in `seq 0 39`
do
	sleep 2
	ip=`ec2din $instance -F "instance-state-name=running" | awk '/INS/{print $4 }'`
	if [ -z "$ip" ]
	then
  		echo "waiting for server to come up ($i)"
	else
  		echo "Server is up. IP is $ip"
		echo ec2.instance=$instance > .serverIP
		echo galaxy.zkServers=$ip:2181 >> .serverIP
  		break
	fi 
done
