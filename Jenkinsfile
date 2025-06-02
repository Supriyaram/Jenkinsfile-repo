@Library('dumbslave-lib') _

pipeline {
    agent any

    parameters {
        choice(name: 'REPO_SELECTION', choices: ['patient-management', 'schedule-management'], description: 'Choose the repo')
        string(name: 'BRANCH_NAME', defaultValue: 'main', description: 'Branch to checkout')
    }

    environment {
        REPO1_URL = 'https://github.com/Supriyaram/patient-management.git'
        REPO2_URL = 'https://github.com/Supriyaram/schedule-management.git'
    }

    stages {
        stage('Provision EC2 & Register Agent') {
            steps {
                script {
                    env.SLAVE_LABEL = "agent-${BUILD_NUMBER}"

                    def templateId = "lt-xxxxxxxxxxxxx"

                    // Launch EC2
                    env.INSTANCE_ID = sh(script: """
                        aws ec2 run-instances \
                          --launch-template LaunchTemplateId=${templateId},Version=2 \
                          --tag-specifications 'ResourceType=instance,Tags=[{Key=Name,Value=jenkins-${env.SLAVE_LABEL}}]' \
                          --query 'Instances[0].InstanceId' --output text
                    """, returnStdout: true).trim()

                    // Wait until instance is running
                    waitUntil {
                        sh(script: """
                            aws ec2 describe-instances --instance-ids ${env.INSTANCE_ID} \
                            --query "Reservations[0].Instances[0].State.Name" --output text
                        """, returnStdout: true).trim() == "running"
                    }

                    // Get private IP
                    def privateIP = sh(script: """
                        aws ec2 describe-instances --instance-ids ${env.INSTANCE_ID} \
                        --query "Reservations[0].Instances[0].PrivateIpAddress" --output text
                    """, returnStdout: true).trim()

                    // Wait for SSH port to be open
                    waitUntil {
                        sh(script: "nc -z ${privateIP} 22", returnStatus: true) == 0
                    }

                    // Register EC2 as Jenkins agent using shared lib function
                    registerEc2Agent(env.SLAVE_LABEL, privateIP, 'jenkins-ssh-key')
                }
            }
        }

        stage('Run on EC2 Agent') {
            agent { label "${env.SLAVE_LABEL}" }
            steps {
                script {
                    def repoUrl = (params.REPO_SELECTION == 'patient-management') ?
                                  env.REPO1_URL : env.REPO2_URL

                    checkout([
                        $class: 'GitSCM',
                        branches: [[name: "*/${params.BRANCH_NAME}"]],
                        userRemoteConfigs: [[url: repoUrl]]
                    ])
                }
            }
        }

        stage('Build & Test') {
            agent { label "${env.SLAVE_LABEL}" }
            steps {
                script {
                    def mvnHome = tool name: 'Maven 3', type: 'maven'
                    withEnv(["PATH+MAVEN=${mvnHome}/bin"]) {
                        sh 'mvn clean verify'
                    }
                }
            }
        }
    }

    // post {
    //     always {
    //         script {
    //             if (env.INSTANCE_ID) {
    //                 sh "aws ec2 terminate-instances --instance-ids ${env.INSTANCE_ID}"
    //             }

    //             def node = Jenkins.instance.getNode(env.SLAVE_LABEL)
    //             if (node != null) {
    //                 Jenkins.instance.removeNode(node)
    //             }
    //         }
    //     }
    // }
}
