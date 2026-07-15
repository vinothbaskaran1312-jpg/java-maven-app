pipeline {
    agent { label 'node2' }

    environment {
        DOCKER_IMAGE = 'vinothbaskaran1985/java-maven-app'
        DOCKER_TAG = "${BUILD_NUMBER}"
        NEXUS_URL = 'http://node3:8081'
        SONAR_PROJECT = 'java-maven-app'
        JAVA_VERSION = '25'
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

        stage('Update Manifests') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'github-credentials',
                    usernameVariable: 'GIT_USER',
                    passwordVariable: 'GIT_PASS'
                )]) {
                    sh """
                        rm -rf jenkins-demo-manifests
                        git clone https://\${GIT_USER}:\${GIT_PASS}@github.com/vinothbaskaran1312-jpg/jenkins-demo-manifests.git
                        cd jenkins-demo-manifests
                        sed -i 's|${DOCKER_IMAGE}:.*|${DOCKER_IMAGE}:${DOCKER_TAG}|g' deployment.yaml
                        git config user.email "jenkins@node1"
                        git config user.name "Jenkins"
                        git add deployment.yaml
                        git commit -m "Update java-maven-app image to ${DOCKER_TAG} [skip ci]"
                        git push https://\${GIT_USER}:\${GIT_PASS}@github.com/vinothbaskaran1312-jpg/jenkins-demo-manifests.git main
                    """
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