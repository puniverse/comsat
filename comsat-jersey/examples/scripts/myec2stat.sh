#!/bin/bash
ec2din -F "instance-state-name=running" | awk '/INS/{ print $4 }' | parallel ssh -o "StrictHostKeyChecking=no" ubuntu@{} 'echo +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++\; cat nodeData.txt \; grep -niE \"going online\|SWITCHING TO\" galaxy.log \| tail -10'
