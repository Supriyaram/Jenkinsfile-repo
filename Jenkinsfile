pipeline {
    agent {
      label  'inventory'
    }

    parameters {
        choice(
            name: 'SERVICE_NAME',
            choices: ['inventory-service', 'order-service'],
            description: 'Choose the service to build'
        )
    }

    stages {
        stage('Pull Service') {
            steps {
                    echo "Selected Service: ${params.SERVICE_NAME}"
                    git url: "https://github.com/Supriyaram/${params.SERVICE_NAME}", branch: 'main'
                  }
                    
         }
        stage('Build Service'){
            steps{
                script {
                        echo "Selected Service: ${params.SERVICE_NAME}"
                        def JENKINSFILE_PATH = "https://github.com/Supriyaram/${params.SERVICE_NAME}/Jenkinsfile"
                        def microservicePipeline = load "${JENKINSFILE_PATH}"
                        microservicePipeline()
                }    
            }
        }
        stage('Verify Clone') {
            steps {
                    sh 'ls -la Jenkinsfile'
                }
            }

        }
    }

