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
                    if (params.REPO_SELECTION == 'patient-management') {
                        repoUrl = env.REPO1_URL
                    } else if (params.REPO_SELECTION == 'schedule-management') {
                        repoUrl = env.REPO2_URL
                    }
                    git url: repoUrl, branch: params.BRANCH_NAME, 
                }
            }
        }
        stage('Build') {
            steps {
                sh 'mvn --version'
                sh 'mvn clean verify'
            }
        }
    }
}
