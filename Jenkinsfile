pipeline {
    agent any
    environment {
        POSTGRES_PASSWORD = credentials('POSTGRES_PASSWORD')
        HOST_WEB_PORT = 6969
        HOST_APP_PORT = 8082
    }
    tools {
        maven 'Maven3'
    }
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        stage('Build app') {
            steps{
                sh 'cd backend && mvn -B clean package'
            }
        }
        stage('Test app') {
            steps{
                sh 'cd backend && mvn -B test'
            }
            post {
                always {
                    junit 'backend/target/surefire-reports/*.xml'
                }
            }
        }
        stage('Smoke Test') {
            steps {
                sh 'docker-compose up -d --build'
            }
        }
    }
    post {
        always{
            sh 'docker-compose down'
            sh 'docker system prune -f'
        }
    }
}
