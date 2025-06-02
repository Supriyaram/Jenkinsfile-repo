#!/bin/bash

LABEL=$1
TEMPLATE_ID="lt-026fe4def668209ae"        # Update this
SUBNET_ID="subnet-0659d444315ed93b5"          # Update this
SECURITY_GROUP_ID="sg-0e752d73280a90ee5"      # Update this

INSTANCE_ID=$(aws ec2 run-instances \
  --launch-template "LaunchTemplateId=$TEMPLATE_ID,Version=2" \
  --tag-specifications "ResourceType=instance,Tags=[{Key=Name,Value=jenkins-${LABEL}}, {Key=jenkins-label,Value=${LABEL}}]" \
  --query 'Instances[0].InstanceId' \
  --output text)

echo "$INSTANCE_ID" > slave_instance_id.txt
