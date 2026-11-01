pipeline {
    agent any

    options {
        timeout(time: 20, unit: 'MINUTES')
        disableConcurrentBuilds()
    }

    triggers {
        pollSCM('H/2 * * * *')  // Polls every 2 minutes
    }

    environment {
        DOCKERHUB_USER = 'kelvinking0318'
        IMAGE_NAME = 'finsecure-pay'
        CLUSTER = 'finsecure-pay'
        REGION = 'ap-southeast-1'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                sh 'chmod +x mvnw'
            }
        }

        stage('Build') {
            steps {
                sh './mvnw -B -DskipTests clean compile'
            }
        }

        stage('Test') {
            steps {
                sh './mvnw -B test'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Package') {
            steps {
                sh './mvnw -B -DskipTests package'
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                script {
                    env.VERSION = sh(
                        script: "./mvnw -q -DforceStdout help:evaluate -Dexpression=project.version",
                        returnStdout: true).trim()
                    env.IMAGE = "${DOCKERHUB_USER}/${IMAGE_NAME}:${env.VERSION}"
                }
                echo "Building image ${env.IMAGE}"
            }
        }

        stage('Docker Build') {
            steps {
                sh 'docker build -t "$IMAGE" .'
            }
        }

        stage('Image Scan (Trivy)') {
            steps {
                sh '''
                    mkdir -p reports
                    set +e
                    docker run --rm \
                      -v /var/run/docker.sock:/var/run/docker.sock \
                      -v trivy-cache:/root/.cache/ \
                      aquasec/trivy:latest image \
                      --severity HIGH,CRITICAL --ignore-unfixed --exit-code 1 \
                      "$IMAGE" > reports/trivy-image.txt 2>&1
                    rc=$?
                    cat reports/trivy-image.txt
                    exit $rc
                '''
            }
            post {
                always { archiveArtifacts artifacts: 'reports/*.txt', allowEmptyArchive: true }
            }
        }

        stage('Push') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'dockerhub',
                        usernameVariable: 'DH_USER', passwordVariable: 'DH_PASS')]) {
                    sh '''
                        echo "$DH_PASS" | docker login -u "$DH_USER" --password-stdin
                        docker push "$IMAGE"
                        docker logout
                    '''
                }
            }
        }

        stage('Deploy') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'aws-deployer',
                        usernameVariable: 'AWS_ACCESS_KEY_ID', passwordVariable: 'AWS_SECRET_ACCESS_KEY')]) {
                    sh '''
                        aws eks update-kubeconfig --name "$CLUSTER" --region "$REGION"
                        sed "s|^\( *image: \).*|\1$IMAGE|" k8s/finsecure-pay.yaml | kubectl apply -f -
                        if ! kubectl rollout status deployment/finsecure-pay --timeout=180s; then
                            echo "Rollout failed, rolling back to the previous version"
                            kubectl rollout undo deployment/finsecure-pay
                            kubectl rollout status deployment/finsecure-pay --timeout=180s
                            exit 1
                        fi
                        kubectl get pods -o wide
                    '''
                }
            }
        }
    }

    post {
        success { echo "Deployed ${env.IMAGE}" }
        failure { echo 'Pipeline failed' }
    }
}