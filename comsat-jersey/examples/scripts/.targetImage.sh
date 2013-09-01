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
