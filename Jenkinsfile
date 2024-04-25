pipeline {
    agent any

    environment {
        RECIPIENTS = 'lloyd.malfliet@gmail.com'
        SENDER_EMAIL = 'jenkins@lodywood.be'
        ARTIFACT_REPO = 'git@github.com:ReC82/ArtefactRepo.git'
        GIT_CREDENTIALS = 'GitJenkins'
        TARGET_BRANCH = 'main'
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
                    def mvn = tool 'maven3.9.6'; 
                    withSonarQubeEnv('mysonar') {
                        sh "${mvn}/bin/mvn -f MultiToolApi/pom.xml clean verify sonar:sonar -Dsonar.projectKey=FunnyApi"
                    }
                }
            }
        }

        stage('Quality Gate') {
            steps {
                script {
                    def qg = waitForQualityGate()
                    if (qg.status != 'OK') {
                        error "Pipeline aborted due to quality gate failure: ${qg.status}"
                    }
                }
            }
        }        

        stage('Push to Artifact Repo') {
            steps {
                script {
                    def tempDir = "${WORKSPACE}/temp_artifact_repo"
                    sh "mkdir -p ${tempDir}"

                    dir(tempDir) {
                        // Ensure the repository is checked out correctly
                        checkout([
                            $class: 'GitSCM',
                            branches: [[name: 'main']],
                            userRemoteConfigs: [
                                [url: env.ARTIFACT_REPO, credentialsId: env.GIT_CREDENTIALS]
                            ]
                        ])

                        // Verify and switch to the correct branch
                        sh """
                        set -e
                        git fetch --all
                        git checkout main || git checkout origin/main || git checkout -b main
                        """

                        // Copy build artifacts
                        sh "cp -r ${WORKSPACE}/MultiToolApi/target/*.jar ${tempDir}/" 
                        
                        // Commit and push changes
                        sh '''
                        git add .
                        git config user.email "lloyd.malfliet@gmail.com"
                        git config user.name "ReC82"
                        git commit -m "Add new build artifacts"
                        git push origin main
                        '''
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
                         Build URL: ${env.BUILD_URL}
                         You can download the build report [here](${env.BUILD_URL}artifact/build-report.txt).""",
                to: env.RECIPIENTS,
                from: env.SENDER_EMAIL,
                attachLog: true
            )
        }
    }
}
