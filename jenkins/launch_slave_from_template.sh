#!/bin/bash

LABEL=$1
TEMPLATE_ID="lt-026fe4def668209ae"        # Update this

INSTANCE_ID=$(aws ec2 run-instances \
  --launch-template "LaunchTemplateId=$TEMPLATE_ID,Version=2" \
  --tag-specifications "ResourceType=instance,Tags=[{Key=Name,Value=jenkins-${LABEL}}, {Key=jenkins-label,Value=${LABEL}}]" \
  --query 'Instances[0].InstanceId' \
  --output text)

echo "$INSTANCE_ID" > slave_instance_id.txt
