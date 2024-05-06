pipeline {
    agent {
        label "main"
    }

    environment {
        // SERVER CONFIG
        DYN_TEST_MACHINE = "10.1.5.4"
        WEB_SERVER="10.10.1.4"
        // EMAIL CONFIG
        RECIPIENTS = 'lody.devops@gmail.com'
        SENDER_EMAIL = 'jenkins@lodywood.be'
        // GIT CONFIG
        TIMESTAMP = sh(script: 'date +"%Y-%m-%d_%H-%M-%S"', returnStdout: true).trim()
        ARTIFACT_REPO = 'git@github.com:ReC82/ArtefactRepo.git'
        GIT_CREDENTIALS = 'GitJenkins'
        TARGET_BRANCH = 'main'
        // WEB API CONFIG
        WEB_CRENDENTIALS_ID = "Production"
        REMOTE_PATH="/var/www/api/moreless_api.jar"
        // JMETER CONFIG
        JMETER_TEST_PLAN = "FunnyApi.jmx"
        REMOTE_TEST_PLAN_PATH = "/tmp/FunnyApi.jmx"
        JMETER_RESULT_FILE = "jmeter-result.jtl"
        QC_CREDENTIALS_ID = "QualityControl"
        // OWASP CONFIG
        ZAP_HOME = "/usr/bin/owasp-zap"  // Location where OWASP ZAP is installed
        ZAP_PORT = 8080         // Port ZAP will listen on
        ZAP_HOST_URL = "http://lody-funnyapi.centralus.cloudapp.azure.com/"  // URL of the web application to scan
        ZAP_REPORT = "/tmp/zap-report.html"  
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

                                new_jar="MultiToolApi-0.1_\${TIMESTAMP}.jar"

                                # Copy build artifacts
                                cp ${WORKSPACE}/MultiToolApi/target/MultiToolApi-0.1.jar "\${new_jar}"

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

        stage('Deploy to Web Node and restart the service') {
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

                        sh """
                        ssh -o StrictHostKeyChecking=no -i \$SSH_KEY_FILE \${SSH_USER}@\${WEB_SERVER} \\
                            "sudo systemctl restart moreless_api"
                        """
                    }
                }
            }
        }        

        stage('Transfer Test Plan to Quality Control') {
            steps {
                script {
                    withCredentials([sshUserPrivateKey(
                        credentialsId: env.QC_CREDENTIALS_ID,
                        keyFileVariable: 'SSH_KEY_FILE',
                        usernameVariable: 'SSH_USER'
                    )]) {
                        
                        sh """
                            scp -o StrictHostKeyChecking=no -i \$SSH_KEY_FILE ${JMETER_TEST_PLAN} \${SSH_USER}@\${DYN_TEST_MACHINE}:${REMOTE_TEST_PLAN_PATH}
                        """
                    }
                }
            }
        }

        stage('Run JMeter Test on Quality Control') {
            steps {
                script {
                    withCredentials([sshUserPrivateKey(
                        credentialsId: env.QC_CREDENTIALS_ID,
                        keyFileVariable: 'SSH_KEY_FILE',
                        usernameVariable: 'SSH_USER'
                    )]) {
                        
                        sh """
                            ssh -o StrictHostKeyChecking=no -i \$SSH_KEY_FILE \${SSH_USER}@\${DYN_TEST_MACHINE} \\
                            "/var/lib/apache-jmeter/bin/jmeter -n -t ${REMOTE_TEST_PLAN_PATH} -l ${JMETER_RESULT_FILE}"
                        """

                        /* sh """
                            ssh -o StrictHostKeyChecking=no -i \$SSH_KEY_FILE \${SSH_USER}@\${DYN_TEST_MACHINE} \\
                            "/var/lib/apache-jmeter/bin/jmeter -g ${JMETER_RESULT_FILE} -o jmeter_output.html"
                        """*/
                        // ./jmeter.sh -n -t path/test_name -l path/file_name.jtl -e -o path/file_name
                        // Fetch the JMeter result file from the remote machine
                        sh """
                            scp -o StrictHostKeyChecking=no -i \$SSH_KEY_FILE \${SSH_USER}@\${DYN_TEST_MACHINE}:${JMETER_RESULT_FILE} .
                        """
                        
                        if (!fileExists("jmeter-result.jtl")) {
                            error "JMeter result file not found after retrieval."
                        }
                    }
                }
            }
        }

        stage('Run OWASP ZAP Scan') {
            steps {
                script {
                    withCredentials([sshUserPrivateKey(
                        credentialsId: QC_CREDENTIALS_ID,
                        keyFileVariable: 'SSH_KEY_FILE',
                        usernameVariable: 'SSH_USER'
                    )]) {
                        
                        sh """
                            ssh -o StrictHostKeyChecking=no -i \$SSH_KEY_FILE \${SSH_USER}@\${DYN_TEST_MACHINE} \\
                            "owasp-zap -port 8081 -cmd -quickurl \$ZAP_HOST_URL -quickout \$ZAP_REPORT"
                        """
                        
                        sh """
                            scp -o StrictHostKeyChecking=no -i \$SSH_KEY_FILE \${SSH_USER}@\${DYN_TEST_MACHINE}:\${ZAP_REPORT} .
                        """

                        if (!fileExists("zap-report.html")) {
                            error "OWASP ZAP : report not found."
                        }

                        archiveArtifacts artifacts: "zap-report.html", onlyIfSuccessful: true
                    }
                }
            }
        }
  
        stage('Send JMeter Report via Email') {
            steps {
                script {
                    def resultFile = env.JMETER_RESULT_FILE

                    if (!fileExists(resultFile)) {
                        error "JMeter result file not found: ${resultFile}"
                    }

                    emailext(
                        subject: "Jmeter And Zap Report",
                        body: """
                        Tests completed. Please find the attached report.
                        """,
                        to: env.RECIPIENTS, 
                        attachLog: true,
                        attachmentsPattern: "${resultFile},zap-report.html"
                    )
                }
            }
        }   
    }
    post {
        failure {
            emailext(
                subject: "Jenkins Build Failed: ${currentBuild.fullDisplayName} - ${currentBuild.result}",
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
