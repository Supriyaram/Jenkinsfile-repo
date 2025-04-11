pipeline {
    agent {
      label: inventory
    }

    parameters {
        choice(
            name: 'SERVICE_NAME',
            choices: ['inventory-service', 'order-service'],
            description: 'Choose the service to build'
        )
    }

    stages {
        stage('Build Dispatcher') {
            steps {
                    echo "Selected Service: ${params.SERVICE_NAME}"
                    git url: 'https://github.com/Supriyaram/${params.SERVICE_NAME}', branch: 'main'
                  }
                    
                }
          }
    }
