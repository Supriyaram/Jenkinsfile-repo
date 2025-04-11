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
        stage('Build Service') {
            steps {
                    echo "Selected Service: ${params.SERVICE_NAME}"
                    git url: "https://github.com/Supriyaram/${params.SERVICE_NAME}", branch: 'main'
                  }
                    
         }
        stage('Build Service'){
            steps{
                script {
                        echo "Selected Service: ${params.SERVICE_NAME}"
    
                        def jenkinsfilePath = "${params.SERVICE_NAME}/Jenkinsfile"
    
                        build job: 'microservice-pipeline',
                            parameters: [
                                string(name: 'SERVICE_NAME', value: params.SERVICE_NAME),
                                string(name: 'JENKINSFILE_PATH', value: jenkinsfilePath)
                            ]
                    }
            }
        stage('Verify Clone') {
            steps {
                    sh 'ls -la Jenkinsfile'
                }
            }

        }
    }
