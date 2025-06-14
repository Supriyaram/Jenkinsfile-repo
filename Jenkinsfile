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
                    sh "cd ~/.aws"
                    def label = "agent-${env.BUILD_NUMBER}"
                    def instanceId = launchEc2Instance(label)

                    echo "Waiting for instance ${instanceId} to be in 'running' state..."
                    sh "aws ec2 wait instance-status-ok --instance-ids ${instanceId}"

                    def privateIP = sh(script: """
                        aws ec2 describe-instances --instance-ids ${instanceId} \
                        --query "Reservations[0].Instances[0].PrivateIpAddress" --output text
                    """, returnStdout: true).trim()

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
                    checkout([
                        $class           : 'GitSCM',
                        branches         : [[name: "*/${params.BRANCH_NAME}"]],
                        userRemoteConfigs: [[url: repoUrl]]
                    ])
                    // Capture the checked-out repo's commit hash
                    def COMMIT_HASH = sh(script: "git rev-parse --short HEAD", returnStdout: true).trim()
                    echo "Selected Repo Commit Hash: ${COMMIT_HASH}"

                    // Store in environment variable to use later in Docker stage
                    env.APP_COMMIT_HASH = COMMIT_HASH
                    
                    stash name: 'app-code'
                }
            }
        }

        stage('Build & Test') {
            agent { label "${env.SLAVE_LABEL}" }
            steps {
                script {
                    unstash 'app-code'
                    def mvnHome = tool name: 'Maven3', type: 'maven'
                    withEnv(["PATH+MAVEN=${mvnHome}/bin"]) {
                        sh 'mvn clean verify'
                    }
                }
            }
        }

        stage('Docker Build & Push') {
            agent { label "${env.SLAVE_LABEL}" }
            steps {
                script {
                    env.IMAGE_NAME = "${params.REPO_SELECTION}"
                    def repoUrl = "203918864735.dkr.ecr.us-east-1.amazonaws.com/${env.IMAGE_NAME}-repo"
                    def IMAGE_TAG = env.APP_COMMIT_HASH
                    env.ECR_IMAGE = "${repoUrl}:${IMAGE_TAG}"

                    withCredentials([
                        usernamePassword(credentialsId: 'aws-creds', usernameVariable: 'AWS_ACCESS_KEY_ID', passwordVariable: 'AWS_SECRET_ACCESS_KEY')
                    ]) {
                        sh """
                            export AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID}
                            export AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY}

                            docker build -t ${env.IMAGE_NAME} .
                            docker tag ${env.IMAGE_NAME} ${env.ECR_IMAGE}

                            aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin ${repoUrl}
                            docker push ${env.ECR_IMAGE}
                        """
                    }
                }
            }
        }

        stage('Deploy to prod') {
            agent { label "${env.SLAVE_LABEL}" }
            steps {
                script {
                    def deploymentFile = 'deploymentFile.yaml'

                    withCredentials([
                        usernamePassword(
                            credentialsId: 'aws-creds',
                            usernameVariable: 'AWS_ACCESS_KEY_ID',
                            passwordVariable: 'AWS_SECRET_ACCESS_KEY'
                        )
                    ]) {
                        sh """
                            export AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID}
                            export AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY}
                            export IMAGE=${env.ECR_IMAGE}

                            echo ${deploymentFile}
                            envsubst < ${deploymentFile} > rendered.yaml
                            cat rendered.yaml                             

                            aws eks update-kubeconfig --region us-east-1 --name observability
                            kubectl get nodes
                            kubectl create namespace dev
                            kubectl apply -f rendered.yaml -n dev
                        """
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
    def templateId = "lt-026fe4def668209ae"
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
    sh "aws ec2 terminate-instances --instance-ids ${instanceId}"
}
