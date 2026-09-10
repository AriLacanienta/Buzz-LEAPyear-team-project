pipeline {
    agent any
    environment {
        POSTGRES_PASSWORD = credentials('POSTGRES_PASSWORD')
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
        stage('Build Image') {
            steps {
                sh 'docker-compose up \
                    --env POSTGRES_PASSWORD=${POSTGRES_PASSWORD} \
                    -d'
                sh 'docker-compose ps'
            }
            // post {
            //     always {
            //         junit 'target/surefire-reports/*.xml'
            //     }
            // }
        }
        // stage('Smoke Test') {
        //     steps {
        //         sh 'docker-compose up -d'
        //     }
        // }
    }
}
