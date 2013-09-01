#!/bin/bash -f
#ami-5b527c1e ami-fe002cbb
cat > .targetImage.sh <<-EOF
	#!/bin/bash
	su ubuntu -c "echo begining >> image.txt"
	wget --no-cookies --header "Cookie: gpw_e24=http%3A%2F%2Fwww.oracle.com" "http://download.oracle.com/otn-pub/java/jdk/7u21-b11/jdk-7u21-linux-x64.tar.gz"
	sudo mkdir /usr/local/java
	sudo mv jdk-7u21-linux-x64.tar.gz /usr/local/java
	cd /usr/local/java
	sudo tar zxvf jdk-7u21-linux-x64.tar.gz
	cd /usr/bin/
	sudo ln -s  /usr/local/java/jdk1.7.0_21/bin/java .
	sudo ln -s  /usr/local/java/jdk1.7.0_21/bin/javac .
	su ubuntu -c "cd; echo finished jvm >> image.txt"

	su ubuntu -c "cd; wget http://apache.spd.co.il/zookeeper/zookeeper-3.4.3/zookeeper-3.4.3.tar.gz; tar zxvf zookeeper-3.4.3.tar.gz"
	su ubuntu -c "cd; echo finished zk >> image.txt"
	
	ifconfig eth0 mtu 4096
	apt-get install unzip
	su ubuntu -c "cd; echo finished zip >> image.txt"

	su ubuntu -c "cd; wget https://github.com/puniverse/galaxy-integration/archive/training.zip; unzip training.zip; rm training.zip; rm -rf galaxy-example; mv galaxy-integration-training galaxy-example;"
	su ubuntu -c "cd; echo finished galaxy-integration >> image.txt"

	#su ubuntu -c "cd; wget https://github.com/puniverse/galaxy/archive/leaders.zip; unzip leaders.zip; rm leaders.zip; cd galaxy-leaders; ./gradlew jar; cp build/dist/galaxy-1.0-SNAPSHOT.jar ~/galaxy-example/baselib"
	su ubuntu -c "cd; wget https://github.com/puniverse/galaxy/archive/training.zip; unzip training.zip; rm training.zip; cd galaxy-training; ./gradlew jar; cp build/dist/galaxy-1.0-SNAPSHOT.jar ~/galaxy-example/baselib"
	su ubuntu -c "cd; echo finished galaxy >> image.txt"

	su ubuntu -c 'cd; mkdir .bdb;'
	su ubuntu -c 'cd; cd galaxy-example; ./gradlew jar && echo done >> image.txt' 
	su ubuntu -c "cd; echo finished all >> image.txt"
EOF
chmod +x .targetImage.sh

instance=`ec2run ami-fe002cbb -k ec2key2 -g quicklaunch-1 -t m1.medium | awk '/INS/{print $2}'`
echo instance is $instance
if [ -z "$instance" ]
then
	exit 1
fi
echo waiting for server to come up...
sleep 10

for i in `seq 0 9`
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
echo "waiting for sshd 20 secs"
sleep 20
scp -o "StrictHostKeyChecking=no" .targetImage.sh ubuntu@$ip:
ssh -o "StrictHostKeyChecking=no" ubuntu@$ip 'sudo ./.targetImage.sh' | tee image.log
exists=`ec2-describe-images | awk '/galaxy-integration/{ print $2 }'`
if [ -n "$exists" ] 
then
	ec2dereg $exists
fi
image=`ec2-create-image $instance --name "galaxy-integration" | awk '{print $2}'`
echo image is $image
echo $image > .imageID
#ec2-describe-images `cat .imageID ` | awk '/IMAGE/{ print $5 }'
echo waiting for image to be ready...
sleep 20

for i in `seq 0 20`
do
	sleep 10
	status=`ec2-describe-images $image | awk '/IMAGE/{ print $5 }'`
	if [ "$status" != "available" ]
	then
  		echo "waiting for image $status ($i)"
	else
  		echo "image is $status"
  		ec2kill $instance
  		break
	fi 
done

