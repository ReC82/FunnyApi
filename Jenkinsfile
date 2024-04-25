node {
  stage('SCM') {
    checkout scm
  }
  stage('SonarQube Analysis') {
    def mvn = tool 'maven3.9.6';
    withSonarQubeEnv('mysonar') {
      sh "${mvn}/bin/mvn -f MultiToolApi/pom.xml clean verify sonar:sonar -Dsonar.projectKey=funnyapi"
    }
  }
}