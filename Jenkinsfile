pipeline {
    agent any

    tools {
        jdk 'JDK25'
        maven 'Maven'
    }

    options {
        durabilityHint('PERFORMANCE_OPTIMIZED')
        skipDefaultCheckout()
        timestamps()
    }

    environment {
        MAVEN_REPO          = "%USERPROFILE%\\.m2\\repository"
        DOCKER_COMPOSE_FILE = 'docker-compose.yml'
        SPRING_PROFILE      = 'test'
        COMPOSE_PROJECT_NAME = "ci_${env.BUILD_NUMBER}"

        // ── credentials (stored in Jenkins Credentials store) ──
        POSTGRES_PASSWORD   = credentials('postgres-ci-password')
        DOCKER_CREDENTIALS  = credentials('docker-hub-credentials')  // Fix 5: dockerhub user/pass

        // Fix 5: image name + release tag derived from the Git tag or BUILD_NUMBER
        // When you cut a release, push a Git tag (e.g. v1.0.0) and that becomes
        // the Docker image tag.  Falls back to build number for non-tagged builds.
        DOCKER_IMAGE        = "yourorg/posting"
        IMAGE_TAG           = "${env.TAG_NAME ?: "build-${env.BUILD_NUMBER}"}"
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        // ------------------------------------------------
        // Fix 5: Set a release version in pom.xml
        // On a tagged build (e.g. tag = v1.2.3) strip the leading "v"
        // and use that as the Maven version so SNAPSHOT never reaches prod.
        // On a non-tagged build we leave the version unchanged (dev only).
        // ------------------------------------------------
        stage('Set Release Version') {
            when {
                expression { env.TAG_NAME != null }
            }
            steps {
                script {
                    def releaseVersion = env.TAG_NAME.replaceFirst(/^v/, '')
                    echo "Setting Maven version to ${releaseVersion}"
                    bat "mvn versions:set -DnewVersion=${releaseVersion} -DgenerateBackupPoms=false"
                }
            }
        }

        // ------------------------------------------------
        // Start Infrastructure
        // ------------------------------------------------
        stage('Start Services') {
            steps {
                echo 'Starting Docker services...'
                bat """
                docker compose -p %COMPOSE_PROJECT_NAME% ^
                    -f %DOCKER_COMPOSE_FILE% ^
                    up -d --build
                """
            }
        }

        // ------------------------------------------------
        // Verify Containers
        // ------------------------------------------------
        stage('Verify Services') {
            steps {
                echo 'Listing running containers...'
                bat "docker compose -p %COMPOSE_PROJECT_NAME% ps"

                echo 'Waiting for services to stabilize...'
                bat "ping -n 25 127.0.0.1 > nul"
            }
        }

        // ------------------------------------------------
        // Run Tests
        // ------------------------------------------------
        stage('Run Tests') {
            steps {
                bat """
                mvn clean verify ^
                    -T 1C ^
                    -Dmaven.repo.local=%MAVEN_REPO% ^
                    -Dspring.profiles.active=%SPRING_PROFILE% ^
                    -Dspring.datasource.url=jdbc:postgresql://localhost:5432/jobposting ^
                    -Dspring.datasource.username=postgres ^
                    -Dspring.datasource.password=%POSTGRES_PASSWORD% ^
                    -Dspring.data.redis.host=localhost ^
                    -Dspring.data.redis.port=6379 ^
                    -Djwt.secretKey=ci-test-secret-key ^
                    -Dspring.jpa.properties.hibernate.jdbc.time_zone=UTC ^
                    -Duser.timezone=UTC
                """
            }

            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                    archiveArtifacts artifacts: 'target/surefire-reports/**', allowEmptyArchive: true
                }
            }
        }

        // ------------------------------------------------
        // Build JAR
        // ------------------------------------------------
        stage('Build JAR') {
            steps {
                bat """
                mvn package -DskipTests ^
                    -Dmaven.repo.local=%MAVEN_REPO%
                """
            }

            post {
                success {
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                }
            }
        }

        // ────────────────────────────────────────────────────────
        // Fix 5: Build & push Docker image
        // Only runs on tagged builds (release pipeline).
        // ────────────────────────────────────────────────────────
        stage('Build & Push Docker Image') {
            when {
                expression { env.TAG_NAME != null }
            }
            steps {
                script {
                    echo "Building Docker image ${DOCKER_IMAGE}:${IMAGE_TAG}"
                    bat "docker build -t %DOCKER_IMAGE%:%IMAGE_TAG% -t %DOCKER_IMAGE%:latest ."

                    echo "Pushing to Docker Hub..."
                    bat """
                    docker login -u %DOCKER_CREDENTIALS_USR% -p %DOCKER_CREDENTIALS_PSW%
                    docker push %DOCKER_IMAGE%:%IMAGE_TAG%
                    docker push %DOCKER_IMAGE%:latest
                    docker logout
                    """
                }
            }
        }
    }

    // ------------------------------------------------
    // Cleanup
    // ------------------------------------------------
    post {

        always {
            bat """
            docker compose -p %COMPOSE_PROJECT_NAME% ^
                -f %DOCKER_COMPOSE_FILE% ^
                down
            """
            cleanWs()
        }

        success {
            echo 'CI Pipeline SUCCESS'
        }

        failure {
            echo 'CI Pipeline FAILED'
        }
    }
}