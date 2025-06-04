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
                                        unstash 'app-code'
                                        def mvnHome = tool name: 'Maven3', type: 'maven'
                                        withEnv(["PATH+MAVEN=${mvnHome}/bin"]) {
                                                sh 'mvn clean verify'
                                        }
                                }
                        }
                }

                stage('Docker Build') {
                        agent { label "${env.SLAVE_LABEL}" }
                        steps {
                                script {
                                        env.IMAGE_NAME = "${params.REPO_SELECTION}"
                                        def repoUrl = "203918864735.dkr.ecr.us-east-1.amazonaws.com/${env.IMAGE_NAME}-repo"
                                        echo "Using image: ${env.IMAGE_NAME}"

                                        // Build the Docker image
                                        sh "docker build -t ${env.IMAGE_NAME} ."

                                        // Tag the image for ECR
                                        sh "docker tag ${env.IMAGE_NAME} ${repoUrl}:latest"

                                        // Use AWS credentials securely
                                        withCredentials([
                                                usernamePassword(credentialsId: 'aws-creds', usernameVariable: 'AWS_ACCESS_KEY_ID', passwordVariable: 'AWS_SECRET_ACCESS_KEY')
                                        ]) {
                                                sh """
                                                    export AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID}
                                                    export AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY}
                                                      
                                                    # generates a temporary authentication token (password) for ECR valid for 12 hours, 
                                                    aws ecr get-login-password --region us-east-1 | \

                                                    # Tells Docker to read the password from stdin
                                                    docker login --username AWS --password-stdin ${repoUrl}

                                                     # pushes docker image
                                                     docker push ${repoUrl}:latest
                                                    
                                                   # Remove any existing container
                                                     #docker rm -f test-app || true

                                                   # Run a test container from ECR image
                                                    # docker run -d --name test-app -p 8080:8080 ${repoUrl}:latest
                                        
                                                   # Wait a few seconds for app to start
                                                    #   sleep 50
                                        
                                                   # Perform health check, if its fail do, echo
                                                   # curl --fail http://localhost:8080/api/patients
                                                
                                                  # Clean up test container
                                                     #docker stop test-app && docker rm test-app

                                                """
                                        }
                                }
                        }
                }

                stage('Deploy to prod') {
                        agent { label "${env.SLAVE_LABEL}" }
                        steps {
                                script {
                                        // Set image name based on the selected repo (e.g., patient-management or schedule-management)
                                        env.IMAGE_NAME = "${params.REPO_SELECTION}"

                                        // Construct the full ECR image repository URL
                                        def repoUrl = "203918864735.dkr.ecr.us-east-1.amazonaws.com/${env.IMAGE_NAME}-repo"
                                        def deploymentFile = 'deploymentFile.yaml'

                                        // Define the full image reference with tag `latest`
                                        env.ECR_IMAGE = "${repoUrl}:latest"


                                        // Prompt user for confirmation before deploying to production
                                        input message: 'Proceed to deploy to Production?', ok: 'Deploy'

                                        // Replace ${IMAGE} placeholder in deployment YAML using envsubst and save to rendered.yaml
                                        sh """
                                                export IMAGE=${env.ECR_IMAGE}
                                                envsubst < ${deploymentFile} > rendered.yaml
                                            """

                                        // Apply the rendered deployment file to the Kubernetes cluster
                                        sh 'kubectl apply -f rendered.yaml'
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

