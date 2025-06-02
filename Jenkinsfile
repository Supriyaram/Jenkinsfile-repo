pipeline {
    agent any
    parameters {
        choice(name: 'REPO_SELECTION', choices: ['repo1', 'repo2', 'repo3'], description: 'Choose repo')
        string(name: 'BRANCH_NAME', defaultValue: 'main', description: 'Enter branch')
    }
    environment {
        REPO1_URL = 'https://github.com/example/repo1.git'
        REPO2_URL = 'https://github.com/example/repo2.git'
        REPO3_URL = 'https://github.com/example/repo3.git'
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
                    git url: repoUrl, branch: params.BRANCH_NAME, credentialsId: 'your-git-cred-id'
                }
            }
        }
        stage('Build') {
            steps {
                sh './mvnw clean verify'
            }
        }
    }
}
