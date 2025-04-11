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
                       
                        def jenkinsfilePath = "${pwd}/workspace/app_pipeline/Jenkinsfile"
                        echo "jenkinsfilePath: ${jenkinsfilePath}"
                        def microservicePipeline = load jenkinsfilePath
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

