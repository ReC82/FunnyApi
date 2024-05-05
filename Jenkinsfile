pipeline {
    agent {
        label "main"
    }

    environment {
        // SERVER CONFIG
        DYN_TEST_MACHINE = "10.1.5.4"
        WEB_SERVER="10.10.1.4"
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
        JMETER_RESULT_FILE = "jmeter-result.jtl"
        QC_CREDENTIALS_ID = "QualityControl"
        // OWASP CONFIG
        ZAP_HOME = "/usr/bin/owasp-zap"  // Location where OWASP ZAP is installed
        ZAP_PORT = 80            // Port ZAP will listen on
        TARGET_URL = "http://www.lodywood.be"  // URL of the web application to scan
        ZAP_REPORT = "zap-report.html"  // Report file name
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
                                # Remove any existing local dir
                                rm -rf MultiToolApi

                                # Clone the repository and check out the branch
                                git clone ${ARTIFACT_REPO} .
                                git checkout ${TARGET_BRANCH} || git checkout -b ${TARGET_BRANCH}

                                # Copy build artifacts
                                cp ${WORKSPACE}/MultiToolApi/target/MultiToolApi-0.1.jar .

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
                        sh 'ssh-keyscan \$WEB_SERVER >> ~/.ssh/known_hosts'
                        sh 'pwd'
                        echo "Another tRY"
                        sh """
                        scp -o StrictHostKeyChecking=no -i \$SSH_KEY_FILE ${WORKSPACE}/MultiToolApi/target/MultiToolApi-0.1.jar \${SSH_USER}@\${WEB_SERVER}:\${REMOTE_PATH}
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
                            scp -o StrictHostKeyChecking=no -i \$SSH_KEY_FILE ${JMETER_TEST_PLAN} \${SSH_USER}@\${DYN_TEST_MACHINE}:${REMOTE_TEST_PLAN_PATH}
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
                            ssh -o StrictHostKeyChecking=no -i \$SSH_KEY_FILE \${SSH_USER}@\${DYN_TEST_MACHINE} \\
                            "/var/lib/apache-jmeter/bin/jmeter -n -t ${REMOTE_TEST_PLAN_PATH} -l ${JMETER_RESULT_FILE}"
                        """

                        // Fetch the JMeter result file from the remote machine
                        sh """
                            scp -o StrictHostKeyChecking=no -i \$SSH_KEY_FILE \${SSH_USER}@\${DYN_TEST_MACHINE}:${JMETER_RESULT_FILE} .
                        """

                        // Check if the result file was retrieved
                        if (!fileExists("jmeter-result.jtl")) {
                            error "JMeter result file not found after retrieval."
                        }
                    }
                }
            }
        }

    
        stage('Run OWASP ZAP Scan on Remote Server') {
            steps {
                script {
                    withCredentials([
                        sshUserPrivateKey(
                            credentialsId: env.QC_CREDENTIALS_ID,
                            keyFileVariable: 'SSH_KEY',
                            usernameVariable: 'SSH_USER'
                        )
                    ]) {
                        // Start OWASP ZAP in daemon mode on the remote server
                        sh """
                            ssh -o StrictHostKeyChecking=no -i \$SSH_KEY \${SSH_USER}@\${DYN_TEST_MACHINE} \\
                            "\${ZAP_PATH}/zap.sh -daemon -port \${ZAP_PORT}"
                        """

                        // Wait for ZAP to be ready
                        sh """
                            ssh -o StrictHostKeyChecking=no -i \$SSH_KEY \${SSH_USER}@\${DYN_TEST_MACHINE} \\
                            "zap-cli -p \${ZAP_PORT} status -t 120"
                        """

                        // Run a ZAP quick scan and generate a report
                        sh """
                            ssh -o StrictHostKeyChecking=no -i \$SSH_KEY \${SSH_USER}@\${DYN_TEST_MACHINE} \\
                            "zap-cli -p \${ZAP_PORT} quick-scan --spider --ajax-spider \${ZAP_TARGET_URL} && \\
                             zap-cli -p \${ZAP_PORT} report -o \${ZAP_REPORT_PATH} -f html"
                        """

                        // Fetch the ZAP report from the remote server
                        sh """
                            scp -o StrictHostKeyChecking=no -i \$SSH_KEY \${SSH_USER}@\${DYN_TEST_MACHINE}:\${ZAP_REPORT_PATH} .
                        """

                        // Check if the report file was retrieved
                        if (!fileExists("zap-report.html")) {
                            error "OWASP ZAP report not found after retrieval."
                        }

                        // Archive the report in Jenkins
                        archiveArtifacts artifacts: "zap-report.html", onlyIfSuccessful: true
                    }
                }
            }
        }

        stage('Send ZAP Report via Email') {
            steps {
                emailext(
                    subject: "OWASP ZAP Security Report",
                    body: """
                        OWASP ZAP security scan completed. Please find the attached report.
                    """,
                    to: "env.RECIPIENTS",  // Adjust to the correct recipient
                    attachmentsPattern: "zap-report.html",
                    attachLog: true
                )
            }
        }               

        stage('Send JMeter Report via Email') {
            steps {
                script {
                    def resultFile = env.JMETER_RESULT_FILE  // Ensure the variable is set correctly

                    if (!fileExists(resultFile)) {
                        error "JMeter result file not found: ${resultFile}"
                    }

                    emailext(
                        subject: "JMeter Test Report",
                        body: """
                        JMeter test completed. Please find the attached report.
                        """,
                        to: env.RECIPIENTS, 
                        attachLog: true,
                        attachmentsPattern: env.resultFile
                    )
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
