pipeline {
    agent any
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
                sh 'mvn -B clean package'
                sh 'docker build -t buzz-leapyear:latest .'
            }
            // post {
            //     always {
            //         junit 'target/surefire-reports/*.xml'
            //     }
            // }
        }
        stage('Smoke Test') {
            steps {
                sh 'docker run --rm -d buzz-leapyear:latest'
            }
        }
    }
    // post {
    //     always {
    //                 step([ $class: 'GitHubCommitStatusSetter'])
    //     }
    // }
}
