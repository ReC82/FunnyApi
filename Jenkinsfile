pipeline {
    agent {
        label "main"
    }

    environment {
        // EMAIL CONFIG
        RECIPIENTS = 'lloyd.malfliet@gmail.com'
        SENDER_EMAIL = 'jenkins@lodywood.be'
        // GIT CONFIG
        ARTIFACT_REPO = 'git@github.com:ReC82/ArtefactRepo.git'
        GIT_CREDENTIALS = 'GitJenkins'
        TARGET_BRANCH = 'main'
        // WEB API CONFIG
        WEB_CRENDENTIALS_ID = "Production"
        // JMETER CONFIG
        JMETER_TEST_PLAN = "MoreLessApi.jmx"
        REMOTE_TEST_PLAN_PATH = "/tmp/MoreLessApi.jmx"
        JMETER_RESULT_FILE = "/tmp/jmeter-result.jtl"
        REMOTE_MACHINE = "10.1.5.4"
        QC_CREDENTIALS_ID = "QualityControl"
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
                    def mvn = tool 'maven3.9.6'
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
                        withCredentials([
                            sshUserPrivateKey(
                                credentialsId: GIT_CREDENTIALS,
                                keyFileVariable: 'SSH_KEY'
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
                                cp ${WORKSPACE}/MultiToolApi/target/*.jar .

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

        stage('Deploy to Remote Server') {
            steps {
                script {
                    withCredentials([sshUserPrivateKey(
                        credentialsId: env.WEB_CRENDENTIALS_ID,
                        keyFileVariable: 'SSH_KEY_FILE',                        
                        usernameVariable: 'SSH_USER'
                    )]) {
                        sh 'ssh-keyscan \$REMOTE_MACHINE >> ~/.ssh/known_hosts'
                        sh 'pwd'
                        echo "Another tRY"
                        sh """
                        scp -o StrictHostKeyChecking=no -i \$SSH_KEY_FILE target/morelessapi.jar \${SSH_USER}@\${REMOTE_MACHINE}:\${REMOTE_PATH}
                        """

                    }
                }
            }
        }        

        stage('Transfer Test Plan to Remote Machine') {
            steps {
                script {
                    withCredentials([sshUserPrivateKey(
                        credentialsId: env.QC_CREDENTIALS_ID,
                        keyFileVariable: 'SSH_KEY_FILE',
                        usernameVariable: 'SSH_USER'
                    )]) {
                        // Copy the JMeter test plan to the remote machine
                        sh """
                            scp -o StrictHostKeyChecking=no -i \$SSH_KEY_FILE ${JMETER_TEST_PLAN} \${SSH_USER}@\${REMOTE_MACHINE}:${REMOTE_TEST_PLAN_PATH}
                        """
                    }
                }
            }
        }

        stage('Run JMeter Test on Remote Machine') {
            steps {
                script {
                    withCredentials([sshUserPrivateKey(
                        credentialsId: env.QC_CREDENTIALS_ID,
                        keyFileVariable: 'SSH_KEY_FILE',
                        usernameVariable: 'SSH_USER'
                    )]) {
                        // Run the JMeter test on the remote machine
                        sh """
                            ssh -o StrictHostKeyChecking=no -i \$SSH_KEY_FILE \${SSH_USER}@\${REMOTE_MACHINE} \\
                            "jmeter -n -t ${REMOTE_TEST_PLAN_PATH} -l ${JMETER_RESULT_FILE}"
                        """

                        // Fetch the JMeter result file from the remote machine
                        sh """
                            scp -o StrictHostKeyChecking=no -i \$SSH_KEY_FILE \${SSH_USER}@\${REMOTE_MACHINE}:${JMETER_RESULT_FILE} .
                        """

                        // Check if the result file was retrieved
                        if (!fileExists("jmeter-result.jtl")) {
                            error "JMeter result file not found after retrieval."
                        }
                    }
                }
            }
        }
    }

    post {
        always {
            emailext(
                subject: "Jenkins Build: ${currentBuild.fullDisplayName} - ${currentBuild.result}",
                body: """
Build Result: ${currentBuild.result}
Build Number: ${currentBuild.number}
Build URL: ${env.BUILD_URL}
You can download the build report [here](${env.BUILD_URL}artifact/build-report.txt).
""",
                to: env.RECIPIENTS,
                from: env.SENDER_EMAIL,
                attachLog: true
            )
        }
    }
}
