pipeline {
    agent any

    environment {
        RECIPIENTS = 'lloyd.malfliet@gmail.com ' // Change to your email address
        SENDER_EMAIL = 'jenkins@lodywood.be' // The sender's email address
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('SonarQube Analysis') {
            steps {
                script {
                    def mvn = tool 'maven3.9.6'; // Make sure this is a valid Maven tool in Jenkins
                    withSonarQubeEnv('mysonar') {
                        sh "${mvn}/bin/mvn -f MultiToolApi/pom.xml clean verify sonar:sonar -Dsonar.projectKey=FunnyApi"
                    }
                }
            }
        }
    }

    post {
        always {
            emailext(
                subject: "Jenkins Build: ${currentBuild.fullDisplayName} - ${currentBuild.result}",
                body: """Build Result: ${currentBuild.result}
                        Build Number: ${currentBuild.number}
                        Build URL: ${env.BUILD_URL}""",
                to: env.RECIPIENTS,
                from: env.SENDER_EMAIL
            )
        }
    }
}
