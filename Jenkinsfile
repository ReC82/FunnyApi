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
                        // Use SSH credentials from Jenkins
                        withCredentials([
                            sshUserPrivateKey(
                                credentialsId: GIT_CREDENTIALS,
                                keyFileVariable: 'SSH_KEY'  // Variable to hold the SSH key
                            )
                        ]) {
                            sh """
                                # Start SSH agent and add the SSH key
                                eval \$(ssh-agent -s)
                                ssh-add \$SSH_KEY
                                ssh-keyscan github.com >> ~/.ssh/known_hosts

                                # Clone the repository and check out the branch
                                git clone ${ARTIFACT_REPO} .
                                git checkout ${TARGET_BRANCH} || git checkout -b ${TARGET_BRANCH}

                                # Copy build artifacts
                                cp ${WORKSPACE}/MultiToolApi/target/*.jar ${tempDir}/
                                
                                # Add, commit, and push changes
                                git add .
                                git commit -m "Add new build artifacts"
                                git push origin ${TARGET_BRANCH}
                            """
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
