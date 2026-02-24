pipeline {
    agent any

    triggers {
        githubPush()
    }

    environment {
        JAVA_TOOL_OPTIONS = '-Dfile.encoding=UTF-8'

        // [Manifest Repo]
        CRED_ID_MANIFEST = 'github-manifest-key'
        MANIFEST_REPO_URL = 'git@github.com:20250918-beyond-SW-Camp-21th/beyond-SW-21th-Final-4team-Manifest-file.git'

        // Docker
        IMAGE_NAME = 'o2ppo/freebrback001'
        DOCKER_CRED_ID = 'dockerhub-credentials'

        // Git Config
        GIT_EMAIL = 'lmjayoul@gmail.com'

        // BuildKit 문제 발생 시 비활성화 설정 (0으로 설정)
        DOCKER_BUILDKIT = '0'
    }

    stages {
        stage('Checkout Code') {
            steps {
                cleanWs()
                checkout scm
                echo "Source Code Checkout Complete"
            }
        }

        stage('Setup & Check') {
            steps {
                script {
                    env.GIT_COMMIT_HASH = sh(script: "git rev-parse --short HEAD", returnStdout: true).trim()
                    env.IMAGE_TAG = "${currentBuild.number}-${env.GIT_COMMIT_HASH}"

                    def rawBranch = env.BRANCH_NAME ?: (env.GIT_BRANCH ?: 'main')
                    env.TARGET_BRANCH = rawBranch.replace('origin/', '')

                    echo "Build Tag: ${env.IMAGE_TAG}"
                    echo "Target Branch: ${env.TARGET_BRANCH}"
                }
            }
        }

        stage('Build & Push') {
            steps {
                script {
                    // 1. Gradle Build (bootJar만 빌드)
                    sh 'chmod +x freebridge/gradlew'
                    sh 'cd freebridge && ./gradlew clean bootJar -x test'

                    // 2. Docker 빌드 및 로그인 (직접 ID 입력 방식으로 안정성 확보)
                    withCredentials([usernamePassword(credentialsId: 'dockerhub-credentials', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {

                        // 핵심 수정: DOCKER_BUILDKIT=0 명시하여 buildx 미설치 에러 해결
                        sh "DOCKER_BUILDKIT=0 docker build --no-cache -t ${env.IMAGE_NAME}:${env.IMAGE_TAG} ."

                        sh 'echo $DOCKER_PASS | docker login -u $DOCKER_USER --password-stdin'

                        // 3. Docker Push (네트워크 불안정 대비 retry 로직)
                        retry(3) {
                            sh "docker push ${env.IMAGE_NAME}:${env.IMAGE_TAG}"
                        }

                        sh "docker tag ${env.IMAGE_NAME}:${env.IMAGE_TAG} ${env.IMAGE_NAME}:latest"
                        retry(2) {
                            sh "docker push ${env.IMAGE_NAME}:latest"
                        }
                    }
                }
            }
        }

        stage('Update Manifest Repo') {
            steps {
                script {
                    sshagent(credentials: ["${env.CRED_ID_MANIFEST}"]) {
                        sh """
                            # 1. Setup SSH
                            mkdir -p ~/.ssh && ssh-keyscan github.com >> ~/.ssh/known_hosts

                            # 2. Clone Manifest Repository
                            rm -rf manifest-repo
                            git clone ${env.MANIFEST_REPO_URL} manifest-repo

                            cd manifest-repo

                            # Configure Git
                            git config user.name "Jenkins Backend Bot"
                            git config user.email "${env.GIT_EMAIL}"

                            # 3. Check for Manifest Files
                            if [ ! -f kube-folder/backend-deployment.yml ]; then
                                echo "Error: kube-folder/backend-deployment.yml not found!"
                                exit 1
                            fi

                            # 4. Update Image Tag (큰따옴표를 사용하여 변수 치환 보장)
                            sed -i "s|image: ${env.IMAGE_NAME}:.*|image: ${env.IMAGE_NAME}:${env.IMAGE_TAG}|g" kube-folder/backend-deployment.yml

                            # 5. Commit & Push
                            git add .
                            if ! git diff --cached --quiet; then
                                git commit -m "[Jenkins] Update backend image to ${env.IMAGE_TAG}"
                                git push origin main
                                echo "Manifest Repo Updated!"
                            else
                                echo "No changes to push."
                            fi
                        """
                    }
                }
            }
        }

        stage('Deploy to Server Eric pc') {
            steps {
                script {
                    withCredentials([file(credentialsId: 'k8s-kubeconfig', variable: 'KUBECONFIG')]) {
                        sh '''
                            export KUBECONFIG=$KUBECONFIG
                            chmod 600 $KUBECONFIG
                            export PATH=$PATH:/usr/local/bin:$HOME/bin

                            cd manifest-repo

                            # 쿠버네티스 리소스 적용
                            kubectl apply -f kube-folder/backend-deployment.yml
                            kubectl apply -f kube-folder/backend-service.yml

                            # 최신 이미지를 즉시 반영하기 위한 재시작
                            kubectl rollout restart deployment/backend
                            kubectl rollout status deployment/backend --timeout=60s
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
            sh "docker rmi ${env.IMAGE_NAME}:latest || true"
            sh 'docker image prune -f || true'
            cleanWs()
        }
        success {
            withCredentials([string(credentialsId: 'discord', variable: 'DISCORD')]) {
                discordSend(
                    description: "**백엔드 배포 성공!** :tada:\n**Tag**: ${env.IMAGE_TAG}",
                    result: 'SUCCESS',
                    title: "${env.JOB_NAME} Build Success",
                    webhookURL: "$DISCORD"
                )
            }
        }
        failure {
            withCredentials([string(credentialsId: 'discord', variable: 'DISCORD')]) {
                discordSend(
                    description: "**백엔드 배포 실패** :x:\nJenkins 콘솔 로그를 확인하세요.",
                    result: 'FAILURE',
                    title: "${env.JOB_NAME} Build Failed",
                    webhookURL: "$DISCORD"
                )
            }
        }
    }
}