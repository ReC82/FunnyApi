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
                        // Checkout repository with credentials and ensure on a branch
                        checkout([
                            $class: 'GitSCM',
                            branches: [[name: "refs/heads/${env.TARGET_BRANCH}"]],
                            userRemoteConfigs: [
                                [url: env.ARTIFACT_REPO, credentialsId: env.GIT_CREDENTIALS]
                            ]
                        ])

                        // Copy build artifacts
                        sh "cp ${WORKSPACE}/MultiToolApi/target/*.jar ${tempDir}/" 
                        
                        // Correct syntax with environment variable references
                        sh '''
                        git add .
                        git config user.email "lloyd.malfliet@gmail.com"
                        git config user.name "ReC82"
                        git commit -m "Add new build artifacts"
                        git push origin HEAD:${env.TARGET_BRANCH}
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
