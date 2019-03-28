pipeline {
  agent any
  stages {
    stage('Integration tests ') {
      steps {
        sh 'mvn -f dhis-2/pom.xml install -Pintegration'
      }
    }
  }
}