@Library('dumbslave-lib') _

pipeline {
        agent { label 'built-in' }
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
                                        def label = "agent-${env.BUILD_NUMBER}"
                                        def instanceId = launchEc2Instance(label)

                                        echo "Waiting for instance ${instanceId} to be in 'running' state..."

                                        sh "aws ec2 wait instance-status-ok --instance-ids ${instanceId}"

                                        def privateIP = sh(script: """
                        aws ec2 describe-instances --instance-ids ${instanceId} \
                        --query "Reservations[0].Instances[0].PrivateIpAddress" --output text
                    """, returnStdout: true).trim()

                                        // Save instance ID and label to env for future stages
                                        env.SLAVE_LABEL = label
                                        env.INSTANCE_ID = instanceId

                                        registerEc2Agent(label, privateIP, 'jenkins-ssh-key')
                                }
                        }
                }

                stage('Check out on EC2 Agent') {
                        agent { label "${env.SLAVE_LABEL}" }
                        steps {
                                script {
                                        def repoUrl = (params.REPO_SELECTION == 'patient-management') ? env.REPO1_URL : env.REPO2_URL
                                        echo "Cloning repo: ${repoUrl} on branch: ${params.BRANCH_NAME}"

                                        // Clean workspace before new checkout
                                        deleteDir()
                                        //ensures app-repo is checking out inside 'app' dir
                                                checkout([
                                                        $class           : 'GitSCM',
                                                        branches         : [[name: "*/${params.BRANCH_NAME}"]],
                                                        userRemoteConfigs: [[url: repoUrl]]
                                                ])
                                                stash name: 'app-code'
                                }
                        }
                }

                stage('Build & Test') {
                        agent { label "${env.SLAVE_LABEL}" }
                        steps {
                                script {
                                        //same app-repo must be used here since its different stage, workspace from previous will be refreshed
                                        sh 'ls -al && pwd'
                                        unstash 'app-code'
                                        sh 'ls -al && pwd'
                                        def mvnHome = tool name: 'Maven3', type: 'maven'
                                        withEnv(["PATH+MAVEN=${mvnHome}/bin"]) {
                                                sh 'mvn clean verify'
                                        }
                                }
                        }
                }
                stage('Install AWS CLI') {
                        steps {
                                sh '''
                                        aws --version
//                                    if ! command -v aws &> /dev/null; then
//                                        echo "Installing AWS CLI..."
//                                        sudo apt-get update -y
//                                        sudo apt-get install -y unzip curl
//                                        curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
//                                        unzip -q awscliv2.zip
//                                        sudo ./aws/install --update
//                                        echo "AWS CLI installed at: $(which aws)"
//                                    else
//                                        echo "AWS CLI already installed: $(which aws)"
//                                    fi
                                '''
                        }
                }
                stage('Docker Build') {
                        agent { label "${env.SLAVE_LABEL}" }
                        steps {
                                script{
                                        def repoUrl = "203918864735.dkr.ecr.us-east-1.amazonaws.com/${env.IMAGE_NAME}-repo"
                                        sh 'ls -al && pwd'
                                        env.IMAGE_NAME = "${params.REPO_SELECTION}"
                                        echo "Using image: ${env.IMAGE_NAME}"
                                        sh " docker build -t ${env.IMAGE_NAME } ."
                                        sh  "docker tag ${env.IMAGE_NAME} 203918864735.dkr.ecr.us-east-1.amazonaws.com/${env.IMAGE_NAME}:latest"
                                        withCredentials([usernamePassword(credentialsId: 'aws-creds', usernameVariable: 'AWS_ACCESS_KEY_ID', passwordVariable: 'AWS_SECRET_ACCESS_KEY')]) {
                                                sh '''
                                                         export AWS_ACCESS_KEY_ID=$AWS_ACCESS_KEY_ID
                                                         export AWS_SECRET_ACCESS_KEY=$AWS_SECRET_ACCESS_KEY
                                                         aws ecr get-login-password --region us-east-1 | \
                                                        docker login --username AWS --password-stdin ${repoUrl}
                                                '''
                                        }

                                }

                        }
                }
        }

        post {
                always {
                        script {
                                if (env.INSTANCE_ID) {
                                        echo "Terminating instance ${env.INSTANCE_ID}"
                                        terminateEc2Instance(env.INSTANCE_ID)
                                }
                        }
                }
        }
}


 def launchEc2Instance(String label) {
        def templateId = "lt-026fe4def668209ae" // Replace with actual Launch Template ID
        def command = """
        aws ec2 run-instances \
        --launch-template LaunchTemplateId=${templateId} \
        --tag-specifications 'ResourceType=instance,Tags=[{Key=Name,Value=jenkins-${label}},{Key=jenkins-label,Value=${label}}]' \
        --query 'Instances[0].InstanceId' \
        --output text
    """
        return sh(script: command, returnStdout: true).trim()

}
def terminateEc2Instance(String instanceId) {
        sh """
            aws ec2 terminate-instances --instance-ids ${instanceId}
        """
}

