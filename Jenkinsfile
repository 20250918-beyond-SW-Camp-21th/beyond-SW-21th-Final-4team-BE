pipeline {
    agent any

    environment {
        IMAGE_NAME = 'o2ppo/freebrback001'
        DOCKER_CRED_ID = 'dockerhub-credentials'
        DOCKER_BUILDKIT = '0'

        // 네트워크 타임아웃 강제 확장 (5분)
        DOCKER_CLIENT_TIMEOUT = '300'
        COMPOSE_HTTP_TIMEOUT = '300'
    }

    stages {
        stage('Checkout & Build') {
            steps {
                cleanWs()
                checkout scm

                // Gradle Build
                sh 'chmod +x freebridge/gradlew'
                sh 'cd freebridge && ./gradlew clean bootJar -x test'

                script {
                    env.GIT_COMMIT_HASH = sh(script: "git rev-parse --short HEAD", returnStdout: true).trim()
                    env.IMAGE_TAG = "${currentBuild.number}-${env.GIT_COMMIT_HASH}"
                }

                // Docker Build
                sh "DOCKER_BUILDKIT=0 docker build --no-cache -t ${env.IMAGE_NAME}:${env.IMAGE_TAG} ."
            }
        }

        stage('Push Image (Stabilized)') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: 'dockerhub-credentials', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                        sh 'echo $DOCKER_PASS | docker login -u $DOCKER_USER --password-stdin'

                        //
                        // 네트워크 부하를 줄이기 위해 짧은 주기로 재시도하며 푸시
                        retry(3) {
                            timeout(time: 7, unit: 'MINUTES') {
                                echo "Pushing image tag: ${env.IMAGE_TAG}..."
                                // 안정적인 전송을 위해 stdout을 유지하며 실행
                                sh "docker push ${env.IMAGE_NAME}:${env.IMAGE_TAG}"
                            }
                        }

                        // latest 태그 처리
                        sh "docker tag ${env.IMAGE_NAME}:${env.IMAGE_TAG} ${env.IMAGE_NAME}:latest"
                        retry(2) {
                            sh "docker push ${env.IMAGE_NAME}:latest"
                        }
                    }
                }
            }
        }

        stage('Update Manifest & Deploy') {
            steps {
                script {
                    // Manifest 업데이트 (기존과 동일)
                    sshagent(credentials: ['github-manifest-key']) {
                        sh """
                            mkdir -p ~/.ssh && ssh-keyscan github.com >> ~/.ssh/known_hosts
                            rm -rf manifest-repo
                            git clone git@github.com:20250918-beyond-SW-Camp-21th/beyond-SW-21th-Final-4team-Manifest-file.git manifest-repo
                            cd manifest-repo
                            git config user.name "Jenkins Backend Bot"
                            git config user.email "lmjayoul@gmail.com"
                            sed -i "s|image: ${env.IMAGE_NAME}:.*|image: ${env.IMAGE_NAME}:${env.IMAGE_TAG}|g" kube-folder/backend-deployment.yml
                            git add .
                            if ! git diff --cached --quiet; then
                                git commit -m "[Jenkins] Update backend image to ${env.IMAGE_TAG}"
                                git push origin main
                            fi
                        """
                    }

                    // 배포 실행
                    withCredentials([file(credentialsId: 'k8s-kubeconfig', variable: 'KUBECONFIG')]) {
                        sh '''
                            export KUBECONFIG=$KUBECONFIG
                            cd manifest-repo
                            kubectl apply -f kube-folder/backend-deployment.yml
                            kubectl rollout restart deployment/backend
                        '''
                    }
                }
            }
        }
    }

    post {
        always {
            sh 'docker logout || true'
            sh "docker rmi ${env.IMAGE_NAME}:${env.IMAGE_TAG} || true"
            cleanWs()
        }
    }
}