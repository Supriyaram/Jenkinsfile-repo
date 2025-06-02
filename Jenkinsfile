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
        stage('Checkout') {
            steps {
                script {
                    def repoUrl = ''
                    if (params.REPO_SELECTION == 'repo1') {
                        repoUrl = env.REPO1_URL
                    } else if (params.REPO_SELECTION == 'repo2') {
                        repoUrl = env.REPO2_URL
                    } else if (params.REPO_SELECTION == 'repo3') {
                        repoUrl = env.REPO3_URL
                    }
                    echo repoUrl
                }
            }
        }
        stage('Build') {
            steps {
                sh 'mvn --version'
                sh './mvn clean verify'
            }
        }
    }
}
