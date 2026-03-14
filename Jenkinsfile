pipeline {
    agent any

    triggers {
        githubPush()
    }

    environment {
        AWS_REGION = 'ap-northeast-2'
        EKS_CLUSTER_NAME = 'freebridge-eks'
        AWS_CREDENTIALS_ID = 'aws-eks-jenkins'
        EKS_TOOL_IMAGE = 'dtzar/helm-kubectl:latest'

        IMAGE_NAME = 'o2ppo/freebrback001'
        AI_IMAGE_NAME = 'o2ppo/freebridge-ai'
        DOCKER_CRED_ID = 'dockerhub-credentials'
        DOCKER_BUILDKIT = '0'

        DOCKER_CLIENT_TIMEOUT = '3000'
        COMPOSE_HTTP_TIMEOUT = '3000'

        CRED_ID_MANIFEST = 'github-manifest-key'
        MANIFEST_REPO_URL = 'git@github.com:20250918-beyond-SW-Camp-21th/beyond-SW-21th-Final-4team-Manifest-file.git'
        GIT_EMAIL = 'lmjayoul@gmail.com'
    }

    stages {
        stage('EKS Preflight') {
            steps {
                script {
                    withCredentials([[
                        $class: 'AmazonWebServicesCredentialsBinding',
                        credentialsId: "${env.AWS_CREDENTIALS_ID}",
                        accessKeyVariable: 'AWS_ACCESS_KEY_ID',
                        secretKeyVariable: 'AWS_SECRET_ACCESS_KEY'
                    ]]) {
                        sh """
                            set -euxo pipefail
                            export AWS_DEFAULT_REGION=${env.AWS_REGION}
                            if ! command -v aws > /dev/null 2>&1; then
                                echo 'aws CLI is not installed.'
                                exit 1
                            fi

                            aws --version
                            kubectl version --client=true
                            aws sts get-caller-identity
                            aws eks update-kubeconfig --region ${env.AWS_REGION} --name ${env.EKS_CLUSTER_NAME}
                            kubectl cluster-info
                            kubectl get nodes
                            kubectl get ns
                        """
                    }
                }
            }
        }

        stage('Checkout & Gradle Build') {
            steps {
                cleanWs()
                checkout scm

                sh 'chmod +x freebridge/gradlew'
                sh 'cd freebridge && ./gradlew clean bootJar -x test'

                script {
                    env.GIT_COMMIT_HASH = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                    env.IMAGE_TAG = "${currentBuild.number}-${env.GIT_COMMIT_HASH}"
                    echo "Build Tag 생성 완료: ${env.IMAGE_TAG}"
                }
            }
        }

        stage('Docker Build') {
            steps {
                script {
                    echo 'BuildKit을 활성화하여 빌드를 시작합니다.'

                    sh "DOCKER_BUILDKIT=${env.DOCKER_BUILDKIT} docker build --build-arg APP_JAR=freebridge/app-main/build/libs/app-main-0.0.1-SNAPSHOT.jar -t ${env.IMAGE_NAME}:${env.IMAGE_TAG} ."

                    echo 'Python AI Docker Image 빌드를 시작합니다.'
                    sh "cd freebridge-ai && DOCKER_BUILDKIT=${env.DOCKER_BUILDKIT} docker build -t ${env.AI_IMAGE_NAME}:${env.IMAGE_TAG} ."
                }
            }
        }

        stage('Push Image to Docker Hub') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: "${env.DOCKER_CRED_ID}", usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                        echo "Docker Login 및 Push 시도 중: ${env.IMAGE_TAG}..."
                        sh """
                            set -eux
                            echo \$DOCKER_PASS | docker login -u \$DOCKER_USER --password-stdin

                            docker push ${env.IMAGE_NAME}:${env.IMAGE_TAG}
                            docker tag ${env.IMAGE_NAME}:${env.IMAGE_TAG} ${env.IMAGE_NAME}:latest
                            docker push ${env.IMAGE_NAME}:latest

                            docker push ${env.AI_IMAGE_NAME}:${env.IMAGE_TAG}
                            docker tag ${env.AI_IMAGE_NAME}:${env.IMAGE_TAG} ${env.AI_IMAGE_NAME}:latest
                            docker push ${env.AI_IMAGE_NAME}:latest
                        """
                    }
                }
            }
        }

        stage('Update Manifest Repo') {
            steps {
                script {
                    sshagent(credentials: ["${env.CRED_ID_MANIFEST}"]) {
                        sh """
                            set -eux
                            mkdir -p ~/.ssh
                            ssh-keyscan github.com >> ~/.ssh/known_hosts
                            rm -rf manifest-repo
                            git clone ${env.MANIFEST_REPO_URL} manifest-repo

                            cd manifest-repo
                            git config user.name "Jenkins Backend Bot"
                            git config user.email "${env.GIT_EMAIL}"

                            test -f kube-folder/backend-deployment.yml
                            test -f kube-folder/backend-service.yml
                            test -f kube-folder/python-ai-deployment.yml
                            test -f kube-folder/python-ai-service.yml

                            sed -i "s|image: ${env.IMAGE_NAME}:.*|image: ${env.IMAGE_NAME}:${env.IMAGE_TAG}|g" kube-folder/backend-deployment.yml
                            sed -i "s|image: ${env.AI_IMAGE_NAME}:.*|image: ${env.AI_IMAGE_NAME}:${env.IMAGE_TAG}|g" kube-folder/python-ai-deployment.yml

                            git add .
                            if ! git diff --cached --quiet; then
                                git commit -m "[Jenkins] Update backend & AI image to ${env.IMAGE_TAG}"
                                git push origin main
                                echo 'Manifest Repo 업데이트 완료'
                            else
                                echo '변경 사항이 없습니다.'
                            fi
                        """
                    }
                }
            }
        }
        stage('Install Ingress Nginx If Missing') {
            steps {
                script {
                    withCredentials([[
                        $class: 'AmazonWebServicesCredentialsBinding',
                        credentialsId: "${env.AWS_CREDENTIALS_ID}",
                        accessKeyVariable: 'AWS_ACCESS_KEY_ID',
                        secretKeyVariable: 'AWS_SECRET_ACCESS_KEY'
                    ]]) {
                        sh """
                            set -euxo pipefail
                            export AWS_DEFAULT_REGION=${env.AWS_REGION}
                            if ! command -v aws > /dev/null 2>&1; then
                                echo 'aws CLI is not installed.'
                                exit 1
                            fi
                            if ! command -v helm > /dev/null 2>&1; then
                                echo 'helm is not installed.'
                                exit 1
                            fi

                            aws eks update-kubeconfig --region ${env.AWS_REGION} --name ${env.EKS_CLUSTER_NAME}

                            if kubectl get ingressclass nginx >/dev/null 2>&1; then
                                echo 'ingress-nginx is already installed. Skipping.'
                                exit 0
                            fi

                            kubectl create namespace ingress-nginx --dry-run=client -o yaml | kubectl apply -f -
                            helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx || true
                            helm repo update

                            helm upgrade --install ingress-nginx ingress-nginx/ingress-nginx \
                              --namespace ingress-nginx \
                              --set controller.service.type=LoadBalancer \
                              --set controller.ingressClassResource.name=nginx \
                              --set controller.ingressClass=nginx \
                              --set controller.ingressClassResource.default=true

                            kubectl rollout status deployment/ingress-nginx-controller -n ingress-nginx --timeout=300s
                            kubectl get svc -n ingress-nginx
                            kubectl get ingressclass
                        """
                    }
                }
            }
        }

        stage('Deploy to EKS') {
            steps {
                script {
                    withCredentials([[
                        $class: 'AmazonWebServicesCredentialsBinding',
                        credentialsId: "${env.AWS_CREDENTIALS_ID}",
                        accessKeyVariable: 'AWS_ACCESS_KEY_ID',
                        secretKeyVariable: 'AWS_SECRET_ACCESS_KEY'
                    ]]) {
                        sh """
                            set -euxo pipefail
                            export AWS_DEFAULT_REGION=${env.AWS_REGION}
                            if ! command -v aws > /dev/null 2>&1; then
                                echo 'aws CLI is not installed.'
                                exit 1
                            fi

                            aws eks update-kubeconfig --region ${env.AWS_REGION} --name ${env.EKS_CLUSTER_NAME}
                            cd manifest-repo

                            kubectl apply -f kube-folder/backend-secret.yml
                            kubectl apply -f kube-folder/backend-deployment.yml
                            kubectl apply -f kube-folder/backend-service.yml

                            kubectl apply -f kube-folder/python-ai-configmap.yml
                            kubectl apply -f kube-folder/python-ai-secret.yml
                            kubectl apply -f kube-folder/python-ai-deployment.yml
                            kubectl apply -f kube-folder/python-ai-service.yml

                            if ! kubectl rollout status deployment/backend --timeout=600s; then
                                kubectl get pods -o wide
                                kubectl get rs -l app=backend
                                kubectl describe deployment/backend
                                kubectl describe pods -l app=backend
                                exit 1
                            fi

                            if ! kubectl rollout status deployment/python-ai-service --timeout=600s; then
                                kubectl get pods -o wide
                                kubectl get rs -l app=python-ai-service
                                kubectl describe deployment/python-ai-service
                                kubectl describe pods -l app=python-ai-service
                                exit 1
                            fi
                        """
                    }
                }
            }
        }
    }

    post {
        always {
            sh 'docker logout || true'
            sh "docker rmi ${env.IMAGE_NAME}:${env.IMAGE_TAG} || true"
            sh "docker rmi ${env.IMAGE_NAME}:latest || true"
            sh "docker rmi ${env.AI_IMAGE_NAME}:${env.IMAGE_TAG} || true"
            sh "docker rmi ${env.AI_IMAGE_NAME}:latest || true"
            sh 'docker image prune -f || true'
            cleanWs()
        }
        success {
            withCredentials([string(credentialsId: 'discord', variable: 'DISCORD')]) {
                script {
                    try {
                        discordSend(
                            description: "**백엔드 및 AI 배포 성공!** :tada:\n**Tag**: ${env.IMAGE_TAG}\n**Result**: SUCCESS",
                            result: 'SUCCESS',
                            title: "${env.JOB_NAME} Build Success",
                            webhookURL: "$DISCORD"
                        )
                    } catch (err) {
                        echo "Discord success notification failed: ${err.message}"
                    }
                }
            }
        }
        failure {
            withCredentials([string(credentialsId: 'discord', variable: 'DISCORD')]) {
                script {
                    try {
                        discordSend(
                            description: "**백엔드 및 AI 배포 실패** :x:\n에러 로그를 확인하세요.",
                            result: 'FAILURE',
                            title: "${env.JOB_NAME} Build Failed",
                            webhookURL: "$DISCORD"
                        )
                    } catch (err) {
                        echo "Discord failure notification failed: ${err.message}"
                    }
                }
            }
        }
    }
}


