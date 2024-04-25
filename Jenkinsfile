node {
  stage('SCM') {
    checkout scm
  }
  stage('SonarQube Analysis') {
    def mvn = tool 'maven3.9.6';
    withSonarQubeEnv('mysonar') {
      sh "cd ./MultiToolApi"
      sh "${mvn}/bin/mvn clean verify sonar:sonar -Dsonar.projectKey=funnyapi"
    }
  }
}