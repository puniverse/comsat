#!/bin/bash -f
#ami-5b527c1e ami-fe002cbb
cat > .targetServer.sh <<-EOF
	#!/bin/bash
	pubIP=\`wget -q -O - http://169.254.169.254/latest/meta-data/public-hostname\`
	instId=\`wget -q -O - http://169.254.169.254/latest/meta-data/instance-id\`
	su ubuntu -c "cd; echo \$instId \$pubIP 0 > nodeData.txt"
#	su ubuntu -c 'cd; zookeeper-3.4.3/bin/zkServer.sh start galaxy-example/src/main/resources/config/zoo.cfg > galaxy.log'
#	su ubuntu -c 'cd; echo galaxy.zkServers=127.0.0.1:2181 >> galaxy-example/src/main/resources/config/server.properties'
#	su ubuntu -c 'cd; cd galaxy-example; ./gradlew server &>> ../galaxy.log' &
EOF
chmod +x .targetServer.sh
image=`cat .imageID`
#instance="m1.xlarge"
#instance="m1.medium"
instance="c1.xlarge"
instance=`ec2run $image -k ec2key2 -g quicklaunch-1 -t $instance --user-data-file .targetServer.sh | awk '/INS/{print $2}'`
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
