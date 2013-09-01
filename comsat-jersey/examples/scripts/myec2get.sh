#!/bin/bash
ec2din -F "instance-state-name=running" | awk '/INS/{ print $4 }' | parallel scp -o "StrictHostKeyChecking=no" ubuntu@{}:log.gz {}.log.gz
