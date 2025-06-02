pipeline {
    agent any
    parameters {
        choice(name: 'REPO_SELECTION', choices: ['patient-management', 'schedule-management'], description: 'Choose repo')
        string(name: 'BRANCH_NAME', defaultValue: 'main', description: 'Enter branch')
    }
    environment {
        REPO1_URL = 'https://github.com/Supriyaram/patient-management.git'
        REPO2_URL = 'https://github.com/Supriyaram/schedule-management.git'
    }

    stages {
        stage('Provision EC2 Slave') {
            steps {
                script {
                    env.SLAVE_LABEL = "agent-${BUILD_NUMBER}"
                    // Ensure scripts are executable
                    sh 'chmod +x ./jenkins/launch_slave_from_template.sh'
                    sh 'chmod +x ./jenkins/terminate_slave.sh'

                    // Run the script to launch EC2
                    sh "pwd"
                    sh "whoami"
                    sh "./jenkins/launch_slave_from_template.sh ${env.SLAVE_LABEL}"
                    env.INSTANCE_ID = readFile('slave_instance_id.txt').trim()
                    timeout(time: 3, unit: 'MINUTES') {
                        waitUntil {
                            nodeExists(env.SLAVE_LABEL)
                        }
                    }
                }
            }
        }

        stage('Run on EC2 Agent') {
            agent { label "${env.SLAVE_LABEL}" }
            stages {
                stage('Checkout') {
                    steps {
                        script {
                            def repoUrl = params.REPO_SELECTION == 'patient-management' ? env.REPO1_URL : env.REPO2_URL
                            git url: repoUrl, branch: params.BRANCH_NAME
                        }
                    }
                }

                stage('Build & Test') {
                    steps {
                        sh 'mvn clean verify'
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                if (env.INSTANCE_ID) {
                    echo "Cleaning up EC2 instance: ${env.INSTANCE_ID}"
                    sh "./jenkins/terminate_slave.sh ${env.INSTANCE_ID}"
                }
            }
        }
    }
}
