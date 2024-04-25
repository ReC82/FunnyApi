pipeline {
    agent any

    environment {
        RECIPIENTS = 'lloyd.malfliet@gmail.com'
        SENDER_EMAIL = 'jenkins@lodywood.be'
        ARTIFACT_REPO = 'git@github.com:ReC82/ArtefactRepo.git'
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
                    // Create a temporary directory for cloning the artifact repo
                    def tempDir = "${WORKSPACE}/temp_artifact_repo"
                    sh "mkdir -p ${tempDir}"
                    
                    // Clone the artifact repo to the temp directory
                    dir(tempDir) {
                        checkout([
                            $class: 'GitSCM',
                            branches: [[name: 'main']], 
                            userRemoteConfigs: [[url: env.ARTIFACT_REPO]],
                            credentialsId: 'GitJenkins' 
                        ])
                        
                        sh "cp ${WORKSPACE}/target/* ${tempDir}/" 
                        
                        sh '''
                        git add .
                        git config user.email "lloyd.malfliet@gmail.com" // Adjust with Jenkins user email
                        git config user.name "ReC82"
                        git commit -m "Add new build artifacts"
                        git push
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
                         Build URL: ${env.BUILD_URL}""",
                to: env.RECIPIENTS,
                from: env.SENDER_EMAIL,
                attachLog: true
            )
            script {
                def reportPath = "${WORKSPACE}/build-report.txt"
                def attachments = [[path: reportPath, name: "BuildReport.txt"]]
                emailext(
                    subject: "Build Report - ${currentBuild.result}",
                    body: "Please find the attached build report.",
                    to: env.RECIPIENTS,
                    from: env.SENDER_EMAIL,
                    attachLog: true,
                    attachments: attachments
                )
            }
        }
    }
}
