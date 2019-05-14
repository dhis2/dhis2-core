pipeline {
  agent any
  environment {
    DOCKER_HUB_REPOSITORY = "$DOCKER_HUB_OWNER"
    DOCKER_IMAGE_TAG = ''
    DOCKER_IMAGE_NAME = ''
    IMAGE_VERSION = 'dev'
  }
  stages {
    stage('Setup environment') {
      steps {
        script {
          if (env.BRANCH_NAME != 'master') {
                IMAGE_VERSION = env.BRANCH_NAME
                echo "Image version: ${IMAGE_VERSION}"

           }
          DOCKER_IMAGE_TAG = "${IMAGE_VERSION}-canary-alpine"
            echo "Will tag image as ${DOCKER_IMAGE_TAG}"
            dir ("dhis-2/dhis-e2e-test") {
              sh "TAG=${DOCKER_IMAGE_TAG} docker-compose up -d --build"
            }
          
          DOCKER_IMAGE_NAME = "dhis2-core:${DOCKER_IMAGE_TAG}"
        }
         
      }
    }

    stage('Run api tests') {
      steps {
      dir("dhis-2/dhis-e2e-test") {
       
      }
      }
      
    }

    stage('Publish image') {
      steps {
        sh "docker tag ${DOCKER_IMAGE_NAME} ${DOCKER_HUB_REPOSITORY}/${DOCKER_IMAGE_NAME}"
        
        withDockerRegistry([ credentialsId: "docker-hub-credentials", url: "" ]) {
         sh "docker push ${DOCKER_HUB_REPOSITORY}/${DOCKER_IMAGE_NAME}"
        }
      }
       

    }

  }
}
