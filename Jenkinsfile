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
        stage('Trigger Service Pipeline') {
            steps {
                script {
                    def targetJob = "${params.SERVICE_NAME}-pipeline"
                    echo "Triggering job: ${targetJob}"
                    build job: targetJob
                }
            }
        }
    }
    }

