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
                    env.INSTANCE_ID = launchEc2Instance(env.SLAVE_LABEL)

                    // Wait until EC2 is running
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

                                    sh "aws ec2 wait instance-status-ok --instance-ids ${env.INSTANCE_ID}"

                    // Register EC2 as Jenkins agent using shared lib function
                    registerEc2Agent(env.SLAVE_LABEL, privateIP, 'jenkins-ssh-key')
                }
            }
        }

        stage('Check out on EC2 Agent') {
            agent { label "${env.SLAVE_LABEL}" }
            steps {
                script {
                    // Choose the correct repo URL based on parameter
                    def repoUrl = (params.REPO_SELECTION == 'patient-management') ? env.REPO1_URL : env.REPO2_URL

                    echo "Cloning repo: ${repoUrl} on branch: ${params.BRANCH_NAME}"

                    // Checkout the repository
                    checkout([
                            $class           : 'GitSCM',
                            branches         : [[name: "*/${params.BRANCH_NAME}"]],
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
}


//    post {
//        always {
//            script {
//                if (env.INSTANCE_ID) {
//                    // Terminate the EC2 instance using a method or shared library
//                    terminateEc2Instance(env.INSTANCE_ID)
//                }
//            }
//        }
//    }
//}
def launchEc2Instance(String templateId, String label) {
    // Construct the command and capture output directly
    def command = """
        aws ec2 run-instances \
        --launch-template LaunchTemplateId=${templateId},Version=2 \
        --tag-specifications 'ResourceType=instance,Tags=[{Key=Name,Value=jenkins-${label}},{Key=jenkins-label,Value=${label}}]' \
        --query 'Instances[0].InstanceId' \
        --output text
    """

    // Execute and capture output
    def instanceId = sh(script: command, returnStdout: true).trim()
    echo "EC2 instance launched with ID: ${instanceId}"

    return instanceId
}
