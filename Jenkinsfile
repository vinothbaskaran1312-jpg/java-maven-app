pipeline {
    agent {
        label 'node2'
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '5'))
    }

    environment {
        DOCKER_IMAGE = 'vinothbaskaran1985/java-maven-app'
        DOCKER_TAG = "${BUILD_NUMBER}"
        NEXUS_URL = 'http://node3:8081'
        SONAR_PROJECT = 'java-maven-app'
    }

    stages {
        stage('Checkout') {
            steps {
                echo "Checking out code..."
                checkout scm
                echo "Commit: ${env.GIT_COMMIT}"
            }
        }

        stage('Maven Compile') {
            steps {
                echo "Compiling Java code..."
                sh 'mvn clean compile -s /home/jenkins/.m2/settings.xml'
            }
        }

        stage('Maven Test') {
            steps {
                echo "Running unit tests..."
                sh 'mvn test -s /home/jenkins/.m2/settings.xml'
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                echo "Running SonarQube code analysis..."
                withSonarQubeEnv('sonarqube') {
                    sh """
                        mvn sonar:sonar \
                            -Dsonar.projectKey=${SONAR_PROJECT} \
                            -Dsonar.projectName='Java Maven App' \
                            -s /home/jenkins/.m2/settings.xml
                    """
                }
            }
        }

        stage('Maven Package') {
            steps {
                echo "Packaging JAR artifact..."
                sh 'mvn package -DskipTests -s /home/jenkins/.m2/settings.xml'
                sh 'ls -la target/*.jar'
            }
        }

        stage('Deploy to Nexus') {
            steps {
                echo "Deploying artifact to Nexus..."
                sh 'mvn deploy -DskipTests -s /home/jenkins/.m2/settings.xml'
            }
        }

        stage('Build Docker Image') {
            steps {
                echo "Building Docker image..."
                sh "docker build -t ${DOCKER_IMAGE}:${DOCKER_TAG} ."
                sh "docker tag ${DOCKER_IMAGE}:${DOCKER_TAG} ${DOCKER_IMAGE}:latest"
            }
        }

        stage('Trivy Security Scan') {
            steps {
                echo "Scanning Docker image for vulnerabilities..."
                sh """
                    mkdir -p /home/jenkins/bin
                    if ! command -v /home/jenkins/bin/trivy &> /dev/null; then
                        curl -sfL https://raw.githubusercontent.com/aquasecurity/trivy/main/contrib/install.sh | sh -s -- -b /home/jenkins/bin
                    fi
                    /home/jenkins/bin/trivy image \
                        --timeout 15m \
                        --severity HIGH,CRITICAL \
                        --exit-code 0 \
                        --no-progress \
                        ${DOCKER_IMAGE}:${DOCKER_TAG}
                """
            }
        }

        stage('Push to DockerHub') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'dockerhub-credentials',
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )]) {
                    sh "echo $DOCKER_PASS | docker login -u $DOCKER_USER --password-stdin"
                    sh "docker push ${DOCKER_IMAGE}:${DOCKER_TAG}"
                    sh "docker push ${DOCKER_IMAGE}:latest"
                }
            }
        }

        stage('Update GitOps Repository') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'github-credentials',
                    usernameVariable: 'GIT_USER',
                    passwordVariable: 'GIT_PASS'
                )]) {
                    sh '''
                        rm -rf gitops-repo

                        git clone https://${GIT_USER}:${GIT_PASS}@github.com/vinothbaskaran1312-jpg/java-maven-app-gitops.git gitops-repo

                        cd gitops-repo

                        echo "Current commit:"
                        git log --oneline -1
                        echo "Current tag value:"
                        grep -n "tag:" values-dev.yaml

                        git checkout main


                        git config user.email "jenkins@node1"
                        git config user.name "Jenkins"

                        sed -i "s/tag: \\".*\\"/tag: \\"${DOCKER_TAG}\\"/" values-dev.yaml

                        echo "Updated values-dev.yaml"
                        grep -A4 "image:" values-dev.yaml

                        git add values-dev.yaml
                        
                        git commit -m "Update image tag to ${DOCKER_TAG} [skip ci]" || true

                        git push origin main
                    
                    '''
                }
            }
        }
        stage('Approve Stage Deployment') {
            steps {
                timeout(time: 1, unit: 'HOURS') {
                    input(
                        message: "Deploy image ${DOCKER_TAG} to STAGE?",
                        ok: "Deploy"
                    )
                }
            }
        }
        stage('Update Stage GitOps') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'github-credentials',
                    usernameVariable: 'GIT_USER',
                    passwordVariable: 'GIT_PASS'
                )]) {
                    sh '''
                        rm -rf gitops-stage

                        git clone https://${GIT_USER}:${GIT_PASS}@github.com/vinothbaskaran1312-jpg/java-maven-app-gitops.git gitops-stage

                        cd gitops-stage

                        git config user.email "jenkins@node1"
                        git config user.name "Jenkins"

                        sed -i "s/tag: \\".*\\"/tag: \\"${DOCKER_TAG}\\"/" values-stage.yaml

                        git add values-stage.yaml

                        git commit -m "Promote image ${DOCKER_TAG} to Stage [skip ci]" || true

                        git push origin main
                    '''
                }
            }
        }
        stage('Approve Production Deployment') {
            steps {
                timeout(time: 2, unit: 'HOURS') {
                    input(
                        message: "Deploy image ${DOCKER_TAG} to PRODUCTION?",
                        ok: "Deploy"
                    )
                }
            }
        }
        stage('Update Production GitOps') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'github-credentials',
                    usernameVariable: 'GIT_USER',
                    passwordVariable: 'GIT_PASS'
                )]) {
                    sh '''
                        rm -rf gitops-prod

                        git clone https://${GIT_USER}:${GIT_PASS}@github.com/vinothbaskaran1312-jpg/java-maven-app-gitops.git gitops-prod

                        cd gitops-prod

                        git config user.email "jenkins@node1"
                        git config user.name "Jenkins"

                        sed -i "s/tag: \\".*\\"/tag: \\"${DOCKER_TAG}\\"/" values-prod.yaml

                        git add values-prod.yaml

                        git commit -m "Promote image ${DOCKER_TAG} to Production [skip ci]" || true

                        git push origin main
                    '''
                }
            }
        }
    }
    post {
        success {
            echo '✅ Enterprise CI/CD Pipeline completed successfully!'
        }
        failure {
            echo '❌ Pipeline failed! Check logs above.'
        }
        always {
            sh 'docker logout || true'
            sh 'docker system prune -f || true'
        }
    }
}
