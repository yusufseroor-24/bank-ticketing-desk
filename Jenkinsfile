pipeline {
    agent any

    options {
        skipDefaultCheckout(true)
    }

    parameters {
        booleanParam(name: 'PUSH_TO_ECR', defaultValue: false, description: 'Push the images to AWS ECR')
        booleanParam(name: 'DEPLOY_TO_EC2', defaultValue: false, description: 'Deploy the pushed images on the EC2 server (needs PUSH_TO_ECR)')
    }

    environment {
        AWS_REGION     = 'eu-north-1'
        AWS_ACCOUNT_ID = '113420227617'
        EC2_HOST       = 'ec2-user@13.61.217.119'

        REGISTRY  = "${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"
        IMAGE_TAG = "${BUILD_NUMBER}"
        SERVICES  = 'identity-service ticket-service fraud-service notification-service ai-service api-gateway'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        stage('Test') {
            steps {
                dir('ticket-service') {
                    sh 'sh ./mvnw -B test'
                }
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'ticket-service/target/surefire-reports/*.xml'
                    archiveArtifacts artifacts: 'ticket-service/target/cucumber-report.html', allowEmptyArchive: true
                }
            }
        }
        stage('Build JARs') {
            steps {
                script {
                    for (svc in env.SERVICES.split(' ')) {
                        dir(svc) {
                            sh 'sh ./mvnw -B clean package -Dmaven.test.skip=true'
                        }
                    }
                }
            }
        }

        stage('Build Docker images') {
            steps {
                script {
                    for (svc in env.SERVICES.split(' ')) {
                        sh "docker build -t ${env.REGISTRY}/bankdesk/${svc}:${env.IMAGE_TAG} -t ${env.REGISTRY}/bankdesk/${svc}:latest ${svc}"
                    }
                }
            }
        }

        stage('Push to ECR') {
            when { expression { params.PUSH_TO_ECR } }
            steps {
                withCredentials([
                    string(credentialsId: 'aws-access-key-id', variable: 'AWS_ACCESS_KEY_ID'),
                    string(credentialsId: 'aws-secret-access-key', variable: 'AWS_SECRET_ACCESS_KEY')
                ]) {
                    sh 'aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $REGISTRY'
                    script {
                        for (svc in env.SERVICES.split(' ')) {
                            sh "aws ecr describe-repositories --region ${env.AWS_REGION} --repository-names bankdesk/${svc} || aws ecr create-repository --region ${env.AWS_REGION} --repository-name bankdesk/${svc}"
                            sh "docker push ${env.REGISTRY}/bankdesk/${svc}:${env.IMAGE_TAG}"
                            sh "docker push ${env.REGISTRY}/bankdesk/${svc}:latest"
                        }
                    }
                }
            }
        }

        stage('Deploy to EC2') {
            when { expression { params.DEPLOY_TO_EC2 } }
            steps {
                withCredentials([sshUserPrivateKey(credentialsId: 'ec2-ssh-key', keyFileVariable: 'SSH_KEY')]) {
                    sh '''
                        ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no $EC2_HOST \
                          "cd ~/bankdesk && \
                           export REGISTRY=$REGISTRY IMAGE_TAG=$IMAGE_TAG && \
                           aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $REGISTRY && \
                           docker compose -f docker-compose.prod.yml pull && \
                           docker compose -f docker-compose.prod.yml up -d && \
                           docker image prune -f"
                    '''
                }
            }
        }
    }

    post {
        success { echo "Pipeline finished. Images tagged ${env.IMAGE_TAG}" }
        failure { echo 'Pipeline failed. Open the failed stage above to see why.' }
    }
}