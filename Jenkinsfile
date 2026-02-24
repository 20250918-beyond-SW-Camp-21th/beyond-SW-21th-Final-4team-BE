pipeline {
    agent any

    triggers {
        githubPush()
    }

    environment {
        JAVA_TOOL_OPTIONS = '-Dfile.encoding=UTF-8'
        IMAGE_NAME = 'o2ppo/freebrback001'
        DOCKER_CRED_ID = 'dockerhub-credentials'
        CRED_ID_MANIFEST = 'github-manifest-key'
        MANIFEST_REPO_URL = 'git@github.com:20250918-beyond-SW-Camp-21th/beyond-SW-21th-Final-4team-Manifest-file.git'
        GIT_EMAIL = 'lmjayoul@gmail.com'
        DOCKER_BUILDKIT = '0' // buildx 미설치 대응
    }

    stages {
        stage('System Check') {
            steps {
                script {
                    echo "=== [진단] 디스크 및 네트워크 상태 확인 ==="
                    sh 'df -h'         // 디스크 용량 확인
                    sh 'free -m'       // 메모리 여유량 확인
                    sh 'ulimit -a'     // 프로세스 제한 확인
                }
            }
        }

        stage('Checkout Code') {
            steps {
                cleanWs()
                checkout scm
            }
        }

        stage('Setup & Check') {
            steps {
                script {
                    env.GIT_COMMIT_HASH = sh(script: "git rev-parse --short HEAD", returnStdout: true).trim()
                    env.IMAGE_TAG = "${currentBuild.number}-${env.GIT_COMMIT_HASH}"
                    echo "Build Tag: ${env.IMAGE_TAG}"
                }
            }
        }

        stage('Build & Push') {
            steps {
                script {
                    sh 'chmod +x freebridge/gradlew'
                    sh 'cd freebridge && ./gradlew clean bootJar -x test'

                    withCredentials([usernamePassword(credentialsId: 'dockerhub-credentials', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {

                        echo "=== [시작] Docker 빌드 ==="
                        sh "DOCKER_BUILDKIT=0 docker build --no-cache -t ${env.IMAGE_NAME}:${env.IMAGE_TAG} ."

                        echo "=== [진단] Docker 로그인 시도 ==="
                        sh 'echo $DOCKER_PASS | docker login -u $DOCKER_USER --password-stdin'

                        echo "=== [진단] Docker Push 상세 디버그 모드 실행 ==="
                        // --debug 옵션을 통해 내부 통신 과정을 모두 출력합니다.
                        // 타임아웃 방지를 위해 timeout 블록을 설정합니다 (10분)
                        timeout(time: 10, unit: 'MINUTES') {
                            retry(3) {
                                // 상세 로그를 남기기 위해 --debug 사용 (Docker 버전마다 다를 수 있음)
                                // stderr까지 모두 캡처하여 출력
                                sh "docker --debug push ${env.IMAGE_NAME}:${env.IMAGE_TAG}"
                            }
                        }

                        sh "docker tag ${env.IMAGE_NAME}:${env.IMAGE_TAG} ${env.IMAGE_NAME}:latest"
                        sh "docker --debug push ${env.IMAGE_NAME}:latest"
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
                            sed -i "s|image: ${env.IMAGE_NAME}:.*|image: ${env.IMAGE_NAME}:${env.IMAGE_TAG}|g" kube-folder/backend-deployment.yml
                            git add .
                            if ! git diff --cached --quiet; then
                                git commit -m "[Jenkins] Update backend image to ${env.IMAGE_TAG}"
                                git push origin main
                            fi
                        """
                    }
                }
            }
        }

        stage('Deploy') {
            steps {
                script {
                    withCredentials([file(credentialsId: 'k8s-kubeconfig', variable: 'KUBECONFIG')]) {
                        sh '''
                            export KUBECONFIG=$KUBECONFIG
                            chmod 600 $KUBECONFIG
                            cd manifest-repo
                            kubectl apply -f kube-folder/backend-deployment.yml
                            kubectl apply -f kube-folder/backend-service.yml
                            kubectl rollout restart deployment/backend
                        '''
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                echo "=== 빌드 종료 후 도커 프로세스 확인 ==="
                sh 'ps aux | grep docker || true'
            }
            sh 'docker logout || true'
            sh "docker rmi ${env.IMAGE_NAME}:${env.IMAGE_TAG} || true"
            sh 'docker image prune -f || true'
            cleanWs()
        }
    }
}