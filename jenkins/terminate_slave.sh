#!/bin/bash
INSTANCE_ID=$1
aws ec2 terminate-instances --instance-ids "$INSTANCE_ID"
