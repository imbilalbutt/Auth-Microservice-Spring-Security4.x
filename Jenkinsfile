pipeline {
    agent any

    // Environment variables
    environment {
        // Registry configuration (if pushing to Docker registry)
        DOCKER_REGISTRY = 'your-registry.com'
        DOCKER_IMAGE = 'auth-service'
        DOCKER_TAG = "${BUILD_NUMBER}"

        // Maven settings
        MAVEN_HOME = tool name: 'maven-3.8.4', type: 'maven'
        JAVA_HOME = tool name: 'jdk-17', type: 'jdk'

        // SonarQube configuration
        SONAR_HOST_URL = 'http://sonarqube:9000'
        SONAR_TOKEN = credentials('sonar-token')

        // Deployment environment
        DEPLOY_ENV = 'development'

        // Docker Compose file
        DOCKER_COMPOSE_FILE = 'docker-compose.yml'
    }

    // Tools configuration
    tools {
        maven 'maven-3.8.4'
        jdk 'jdk-17'
    }

    options {
        // Discard old builds
        buildDiscarder(logRotator(numToKeepStr: '10'))

        // Timeout after 30 minutes
        timeout(time: 30, unit: 'MINUTES')

        // Retry on failure
        retry(3)

        // AnsiColor plugin for colored console output
        ansiColor('xterm')

        // Timestamps in console output
        timestamps()

        // GitHub integration
        githubProjectProperty(projectUrlStr: 'https://github.com/yourusername/spring-auth-dev')
    }

    triggers {
        // Poll SCM every 5 minutes
        pollSCM('H/5 * * * *')

        // GitHub webhook trigger
        githubPush()
    }

    parameters {
        choice(
            name: 'DEPLOY_ENVIRONMENT',
            choices: ['development', 'staging', 'production'],
            description: 'Select deployment environment'
        )

        booleanParam(
            name: 'RUN_TESTS',
            defaultValue: true,
            description: 'Run unit and integration tests'
        )

        booleanParam(
            name: 'SONAR_ANALYSIS',
            defaultValue: true,
            description: 'Run SonarQube analysis'
        )

        booleanParam(
            name: 'PUSH_TO_REGISTRY',
            defaultValue: false,
            description: 'Push Docker image to registry'
        )

        booleanParam(
            name: 'DEPLOY',
            defaultValue: false,
            description: 'Deploy to environment after build'
        )
    }

    stages {
        // Stage 1: Checkout
        stage('Checkout') {
            steps {
                script {
                    echo 'Checking out source code from SCM...'
                    checkout scm

                    // Get Git information
                    COMMIT_HASH = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                    BRANCH_NAME = sh(script: 'git rev-parse --abbrev-ref HEAD', returnStdout: true).trim()
                    COMMIT_MESSAGE = sh(script: 'git log -1 --pretty=%B', returnStdout: true).trim()

                    echo "Branch: ${BRANCH_NAME}"
                    echo "Commit: ${COMMIT_HASH}"
                    echo "Message: ${COMMIT_MESSAGE}"
                }
            }
        }

        // Stage 2: Dependency Check
        stage('Dependency Check') {
            steps {
                script {
                    echo 'Checking dependencies and security vulnerabilities...'

                    // Run OWASP Dependency Check
                    sh '''
                        mvn org.owasp:dependency-check-maven:check -Dformat=HTML -Dformat=XML
                    '''

                    // Archive dependency check reports
                    dependencyCheckAnalyzer datadir: 'dependency-check-data', isFailOnError: false
                    dependencyCheckPublisher pattern: '**/dependency-check-report.xml'
                }
            }
        }

        // Stage 3: Build
        stage('Build') {
            steps {
                script {
                    echo 'Building the application...'

                    // Clean and compile
                    sh '''
                        mvn clean compile
                    '''

                    // Show build info
                    sh '''
                        echo "=== Build Information ==="
                        echo "Project: Spring Auth Service"
                        echo "Version: 1.0.0"
                        echo "Java Version: 17"
                        echo "Spring Boot: 3.x"
                        echo "========================="
                    '''
                }
            }
        }

        // Stage 4: Unit Tests
        stage('Unit Tests') {
            when {
                expression { params.RUN_TESTS == true }
            }
            steps {
                script {
                    echo 'Running unit tests...'

                    // Run tests with coverage
                    sh '''
                        mvn test -DskipITs
                    '''

                    // Archive test results
                    junit 'target/surefire-reports/**/*.xml'

                    // Archive coverage reports (if using Jacoco)
                    jacoco(
                        execPattern: 'target/jacoco.exec',
                        classPattern: 'target/classes',
                        sourcePattern: 'src/main/java',
                        exclusionPattern: '**/test/**'
                    )
                }
            }
            post {
                always {
                    // Publish test results
                    publishHTML(
                        target: [
                            reportName: 'Unit Test Report',
                            reportDir: 'target/site/jacoco',
                            reportFiles: 'index.html',
                            keepAll: true
                        ]
                    )
                }
            }
        }

        // Stage 5: Integration Tests
        stage('Integration Tests') {
            when {
                expression { params.RUN_TESTS == true }
            }
            steps {
                script {
                    echo 'Running integration tests...'

                    // Start test containers
                    sh '''
                        docker-compose -f docker-compose.test.yml up -d
                        sleep 30  # Wait for services to be ready
                    '''

                    // Run integration tests
                    sh '''
                        mvn verify -Dit.test=*IT -DskipUTs
                    '''

                    // Stop test containers
                    sh '''
                        docker-compose -f docker-compose.test.yml down
                    '''

                    // Archive integration test results
                    junit 'target/failsafe-reports/**/*.xml'
                }
            }
        }

        // Stage 6: Code Quality
        stage('Code Quality') {
            when {
                expression { params.SONAR_ANALYSIS == true }
            }
            steps {
                script {
                    echo 'Running SonarQube analysis...'

                    // Run SonarQube scanner
                    withSonarQubeEnv('sonarqube') {
                        sh '''
                            mvn sonar:sonar \
                                -Dsonar.projectKey=spring-auth-service \
                                -Dsonar.projectName="Spring Auth Service" \
                                -Dsonar.projectVersion=1.0.0 \
                                -Dsonar.java.source=17 \
                                -Dsonar.sources=src/main/java \
                                -Dsonar.tests=src/test/java \
                                -Dsonar.junit.reportPaths=target/surefire-reports \
                                -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
                        '''
                    }
                }
            }
        }

        // Stage 7: Package
        stage('Package') {
            steps {
                script {
                    echo 'Packaging the application...'

                    // Create jar with tests skipped for faster build
                    sh '''
                        mvn package -DskipTests
                    '''

                    // Verify jar was created
                    sh '''
                        ls -la target/*.jar
                        echo "Jar file size:"
                        du -h target/*.jar
                    '''
                }
            }
        }

        // Stage 8: Build Docker Image
        stage('Build Docker Image') {
            steps {
                script {
                    echo 'Building Docker image...'

                    // Build Docker image
                    sh """
                        docker build \
                            --build-arg BUILD_NUMBER=${BUILD_NUMBER} \
                            --build-arg COMMIT_HASH=${COMMIT_HASH} \
                            --build-arg BRANCH_NAME=${BRANCH_NAME} \
                            -t ${DOCKER_IMAGE}:${DOCKER_TAG} \
                            -t ${DOCKER_IMAGE}:latest \
                            .
                    '''

                    // List images
                    sh '''
                        docker images | grep ${DOCKER_IMAGE}
                    '''

                    // Save image to tar (for backup)
                    sh '''
                        docker save ${DOCKER_IMAGE}:${DOCKER_TAG} -o ${DOCKER_IMAGE}-${DOCKER_TAG}.tar
                    '''

                    archiveArtifacts artifacts: '*.tar', fingerprint: true
                }
            }
        }

        // Stage 9: Push to Registry
        stage('Push to Registry') {
            when {
                expression { params.PUSH_TO_REGISTRY == true }
            }
            steps {
                script {
                    echo 'Pushing Docker image to registry...'

                    // Login to Docker registry
                    withCredentials([usernamePassword(
                        credentialsId: 'docker-registry-creds',
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASS'
                    )]) {
                        sh '''
                            echo ${DOCKER_PASS} | docker login ${DOCKER_REGISTRY} -u ${DOCKER_USER} --password-stdin
                        '''
                    }

                    // Tag and push
                    sh """
                        docker tag ${DOCKER_IMAGE}:${DOCKER_TAG} ${DOCKER_REGISTRY}/${DOCKER_IMAGE}:${DOCKER_TAG}
                        docker tag ${DOCKER_IMAGE}:latest ${DOCKER_REGISTRY}/${DOCKER_IMAGE}:latest

                        docker push ${DOCKER_REGISTRY}/${DOCKER_IMAGE}:${DOCKER_TAG}
                        docker push ${DOCKER_REGISTRY}/${DOCKER_IMAGE}:latest
                    '''

                    // Logout from registry
                    sh '''
                        docker logout ${DOCKER_REGISTRY}
                    '''
                }
            }
        }

        // Stage 10: Deploy
        stage('Deploy') {
            when {
                expression { params.DEPLOY == true }
            }
            steps {
                script {
                    echo "Deploying to ${params.DEPLOY_ENVIRONMENT} environment..."

                    // Select environment-specific configuration
                    def deployEnv = params.DEPLOY_ENVIRONMENT
                    env.DEPLOY_ENV = deployEnv

                    // Create environment-specific compose file
                    sh """
                        cp ${DOCKER_COMPOSE_FILE} docker-compose-${deployEnv}.yml

                        # Update environment variables based on deployment environment
                        if [ "${deployEnv}" = "production" ]; then
                            sed -i 's/SPRING_PROFILES_ACTIVE=.*/SPRING_PROFILES_ACTIVE=prod/' docker-compose-${deployEnv}.yml
                            sed -i 's/SERVER_SERVLET_SESSION_COOKIE_SECURE=.*/SERVER_SERVLET_SESSION_COOKIE_SECURE=true/' docker-compose-${deployEnv}.yml
                        elif [ "${deployEnv}" = "staging" ]; then
                            sed -i 's/SPRING_PROFILES_ACTIVE=.*/SPRING_PROFILES_ACTIVE=staging/' docker-compose-${deployEnv}.yml
                        else
                            sed -i 's/SPRING_PROFILES_ACTIVE=.*/SPRING_PROFILES_ACTIVE=dev/' docker-compose-${deployEnv}.yml
                        fi
                    '''

                    // Stop existing containers
                    sh '''
                        docker-compose -f docker-compose-${DEPLOY_ENV}.yml down --remove-orphans
                    '''

                    // Deploy new version
                    sh '''
                        docker-compose -f docker-compose-${DEPLOY_ENV}.yml up -d

                        # Wait for services to be healthy
                        sleep 30

                        # Check service status
                        docker-compose -f docker-compose-${DEPLOY_ENV}.yml ps
                    '''

                    // Run health check
                    sh '''
                        # Wait for application to be ready
                        max_attempts=30
                        attempt=1

                        while [ $attempt -le $max_attempts ]; do
                            if curl -f http://localhost:8081/actuator/health; then
                                echo "Application is healthy!"
                                break
                            fi

                            echo "Attempt $attempt: Application not ready yet..."
                            sleep 10
                            attempt=$((attempt + 1))
                        done

                        if [ $attempt -gt $max_attempts ]; then
                            echo "ERROR: Application failed to start within 5 minutes"
                            exit 1
                        fi
                    '''

                    // Run smoke tests
                    sh '''
                        echo "Running smoke tests..."

                        # Test registration endpoint
                        curl -X POST http://localhost:8081/api/v1/auth/register \
                            -H "Content-Type: application/json" \
                            -d '{"firstname":"Test","lastname":"User","email":"test@example.com","password":"password123"}' \
                            -w "\\nHTTP Status: %{http_code}\\n"

                        # Test health endpoint
                        curl -f http://localhost:8081/actuator/health

                        echo "Smoke tests passed!"
                    '''
                }
            }
        }

        // Stage 11: Notifications
        stage('Notifications') {
            steps {
                script {
                    echo 'Sending notifications...'

                    // Determine build status
                    def buildStatus = currentBuild.currentResult
                    def color = buildStatus == 'SUCCESS' ? 'good' :
                               buildStatus == 'UNSTABLE' ? 'warning' : 'danger'

                    def message = """
                        *Build #${BUILD_NUMBER} - ${buildStatus}*
                        *Project:* Spring Auth Service
                        *Branch:* ${BRANCH_NAME}
                        *Commit:* ${COMMIT_HASH}
                        *Duration:* ${currentBuild.durationString}
                        *Build URL:* ${env.BUILD_URL}
                    '''

                    // Send Slack notification
                    slackSend(
                        channel: '#build-notifications',
                        color: color,
                        message: message,
                        failOnError: false
                    )

                    // Send email notification
                    emailext(
                        to: 'dev-team@example.com',
                        subject: "Build ${buildStatus}: Spring Auth Service #${BUILD_NUMBER}",
                        body: """
                            Build: ${BUILD_NUMBER}
                            Status: ${buildStatus}
                            Branch: ${BRANCH_NAME}
                            Commit: ${COMMIT_HASH}
                            Duration: ${currentBuild.durationString}

                            Console Output: ${env.BUILD_URL}console
                            Artifacts: ${env.BUILD_URL}artifact/

                            ${COMMIT_MESSAGE}
                        ''',
                        attachLog: buildStatus != 'SUCCESS'
                    )
                }
            }
        }
    }

    post {
        always {
            // Clean up workspace
            cleanWs()

            // Stop any running containers
            sh '''
                docker-compose -f docker-compose.yml down --remove-orphans 2>/dev/null || true
                docker system prune -f
            '''

            // Archive artifacts
            archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            archiveArtifacts artifacts: 'docker-compose*.yml', fingerprint: true
        }

        success {
            echo 'Build completed successfully!'

            // Update deployment dashboard
            sh '''
                echo "SUCCESS: Build #${BUILD_NUMBER}" > build-status.txt
            '''
        }

        failure {
            echo 'Build failed!'

            // Collect failure diagnostics
            sh '''
                echo "FAILURE: Build #${BUILD_NUMBER}" > build-status.txt
                docker logs auth-service 2>/dev/null || true > service-logs.txt
                docker-compose -f docker-compose.yml logs 2>/dev/null || true > compose-logs.txt
            '''

            archiveArtifacts artifacts: '*.txt', fingerprint: true
        }

        unstable {
            echo 'Build is unstable!'
        }

        changed {
            echo 'Build status changed!'
        }
    }
}