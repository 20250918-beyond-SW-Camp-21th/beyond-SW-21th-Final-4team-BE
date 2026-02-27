pipeline {
    agent any

    triggers {
        githubPush()
    }

    environment {
        IMAGE_NAME = 'o2ppo/freebrback001'
        AI_IMAGE_NAME = 'o2ppo/freebridge-ai'
        DOCKER_CRED_ID = 'dockerhub-credentials'
        DOCKER_BUILDKIT = '0' // BuildKit 활성화

        // 네트워크 안정성을 위한 타임아웃 설정
        DOCKER_CLIENT_TIMEOUT = '3000'
        COMPOSE_HTTP_TIMEOUT = '3000'

        // Manifest & Git 설정
        CRED_ID_MANIFEST = 'github-manifest-key'
        MANIFEST_REPO_URL = 'git@github.com:20250918-beyond-SW-Camp-21th/beyond-SW-21th-Final-4team-Manifest-file.git'
        GIT_EMAIL = 'lmjayoul@gmail.com'
    }

    stages {
        stage('Checkout & Gradle Build') {
            steps {
                cleanWs()
                checkout scm

                // 실행 권한 부여 및 bootJar 빌드
                sh 'chmod +x freebridge/gradlew'
                sh 'cd freebridge && ./gradlew clean bootJar -x test'

                script {
                    env.GIT_COMMIT_HASH = sh(script: "git rev-parse --short HEAD", returnStdout: true).trim()
                    env.IMAGE_TAG = "${currentBuild.number}-${env.GIT_COMMIT_HASH}"
                    echo "Build Tag 생성 완료: ${env.IMAGE_TAG}"
                }
            }
        }

        stage('Docker Build') {
            steps {
                script {
                    echo "BuildKit을 활성화하여 빌드를 시작합니다."
                    
                    // Backend (Spring Boot) Docker Build
                    sh "DOCKER_BUILDKIT=0 docker build --build-arg APP_JAR=freebridge/app-main/build/libs/app-main-0.0.1-SNAPSHOT.jar -t ${env.IMAGE_NAME}:${env.IMAGE_TAG} ."
                    
                    // Python AI 서버 도커 Build
                    echo "Python AI Docker Image 빌드를 시작합니다."
                    sh "cd freebridge-ai && DOCKER_BUILDKIT=0 docker build -t ${env.AI_IMAGE_NAME}:${env.IMAGE_TAG} ."
                }
            }
        }

        stage('Push Image to Docker Hub') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: 'dockerhub-credentials', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                        echo "Docker Login 및 Push 시도 중: ${env.IMAGE_TAG}..."
                        sh """
                            echo \$DOCKER_PASS | docker login -u \$DOCKER_USER --password-stdin
                            
                            # Backend Image Push
                            docker push ${env.IMAGE_NAME}:${env.IMAGE_TAG}
                            docker tag ${env.IMAGE_NAME}:${env.IMAGE_TAG} ${env.IMAGE_NAME}:latest
                            docker push ${env.IMAGE_NAME}:latest
                            
                            # Python AI Image Push
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
                            mkdir -p ~/.ssh && ssh-keyscan github.com >> ~/.ssh/known_hosts
                            rm -rf manifest-repo
                            git clone ${env.MANIFEST_REPO_URL} manifest-repo

                            cd manifest-repo
                            git config user.name "Jenkins Backend Bot"
                            git config user.email "${env.GIT_EMAIL}"

                            # Deployment YAML 내 이미지 태그 업데이트 (큰따옴표 사용 필수)
                            sed -i "s|image: ${env.IMAGE_NAME}:.*|image: ${env.IMAGE_NAME}:${env.IMAGE_TAG}|g" kube-folder/backend-deployment.yml
                            sed -i "s|image: ${env.AI_IMAGE_NAME}:.*|image: ${env.AI_IMAGE_NAME}:${env.IMAGE_TAG}|g" kube-folder/python-ai-deployment.yml

                            git add .
                            if ! git diff --cached --quiet; then
                                git commit -m "[Jenkins] Update backend & AI image to ${env.IMAGE_TAG}"
                                git push origin main
                                echo "Manifest Repo 업데이트 완료"
                            else
                                echo "변경 사항이 없습니다."
                            fi
                        """
                    }
                }
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                script {
                    withCredentials([file(credentialsId: 'k8s-kubeconfig', variable: 'KUBECONFIG')]) {
                        sh '''
                            export KUBECONFIG=$KUBECONFIG
                            chmod 600 $KUBECONFIG
                            cd manifest-repo

                            kubectl apply -f kube-folder/backend-deployment.yml
                            kubectl apply -f kube-folder/backend-service.yml

                            # Python AI 배포 파일 적용
                            kubectl apply -f kube-folder/python-ai-deployment.yml

                            # 롤아웃 재시작으로 최신 이미지 반영 강제
                            kubectl rollout restart deployment/backend
                            kubectl rollout status deployment/backend --timeout=60s
                            
                            kubectl rollout restart deployment/python-ai-service
                            kubectl rollout status deployment/python-ai-service --timeout=60s
                        '''
                    }
                }
            }
        }
    }

    post {
        always {
            // 자격 증명 로그아웃 및 로컬 이미지 정리
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
                discordSend(
                    description: "**백엔드 및 AI 배포 성공!** :tada:\n**Tag**: ${env.IMAGE_TAG}\n**Result**: SUCCESS",
                    result: 'SUCCESS',
                    title: "${env.JOB_NAME} Build Success", 
                    webhookURL: "$DISCORD"
                )
            }
        }
        failure {
            withCredentials([string(credentialsId: 'discord', variable: 'DISCORD')]) {
                discordSend(
                    description: "**백엔드 및 AI 배포 실패** :x:\n에러 로그를 확인하세요.",
                    result: 'FAILURE',
                    title: "${env.JOB_NAME} Build Failed", 
                    webhookURL: "$DISCORD"
                )
            }
        }
    }
}