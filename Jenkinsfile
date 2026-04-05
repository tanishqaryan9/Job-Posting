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
        MAVEN_REPO = "%USERPROFILE%\\.m2\\repository"
        DOCKER_COMPOSE_FILE = 'docker-compose.yml'
        SPRING_PROFILE = 'test'
        COMPOSE_PROJECT_NAME = "ci_${env.BUILD_NUMBER}"
        POSTGRES_PASSWORD = credentials('postgres-ci-password')
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
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
        // Verify Containers (REAL CHECK)
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
                    -Dspring.datasource.password=Aryan@95 ^
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
    }

    // ------------------------------------------------
    // Cleanup
    // ------------------------------------------------
    post {

        always {

            echo 'Skipping containers...'

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