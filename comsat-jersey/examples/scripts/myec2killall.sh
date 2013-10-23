#!/bin/bash
ec2kill `ec2din -F "instance-state-name=running" | awk '/INS/{print $2}'`
