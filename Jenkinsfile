pipeline {
  agent any
  environment {
    DOCKER_HUB_REPOSITORY = "$DOCKER_HUB_OWNER"
    DOCKER_IMAGE_TAG = ''
    IMAGE_VERSION = 'dev'
  }
  stages {
    stage('Setup environment') {
      steps {

       if (env.BRANCH_NAME !== 'master') {
            IMAGE_VERSION = env.BRANCH_NAME
            echo "Image version: ${IMAGE_VERSION}"

       }

       DOCKER_IMAGE_TAG = $IMAGE_VERSION + '-canary-alpine'

        echo "Will tag image as ${DOCKER_IMAGE_TAG}"
        dir ("dhis-2/dhis-e2e-test") {
           sh "TAG=$DOCKER_IMAGE_TAG docker-compose up -d --build"
        }
      }
    }

    stage('Run api tests') {
      steps {
      dir("dhis-2/dhis-e2e-test") {
          withMaven {
             sh "mvn clean test -DbaseUrl=http://localhost:8070/api -DsuperUserUsername=taadmin -DsuperUserPsw=Test1212?"
          }
      }
      }
      
    }

    stage('Publish image') {
      steps {
        sh "docker push $DOCKER_HUB_REPOSITORY/dhis2-core:$DOCKER_IMAGE_TAG"
      }
       

    }

  }
}
