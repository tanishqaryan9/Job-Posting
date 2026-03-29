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
        MAVEN_REPO = 'C:\\ProgramData\\Jenkins\\.m2\\repository'
        DB_NAME = 'jobposting_test'
        DB_USER = 'postgres'
        DB_PASS = 'postgres'
        DB_PORT = '5433'
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Start Test Services') {
            steps {
                bat '''
                echo Cleaning old containers...
                docker rm -f postgres_test redis_test 2>nul

                echo Starting PostgreSQL...
                docker run -d --name postgres_test ^
                    -e POSTGRES_DB=%DB_NAME% ^
                    -e POSTGRES_USER=%DB_USER% ^
                    -e POSTGRES_PASSWORD=%DB_PASS% ^
                    -p %DB_PORT%:5432 postgres:15

                echo Starting Redis...
                docker run -d --name redis_test ^
                    -p 6379:6379 redis:latest
                '''
            }
        }

        stage('Wait For Services') {
            steps {
                bat '''
                echo Waiting for PostgreSQL...
                for /l %%i in (1,1,30) do (
                    docker exec postgres_test pg_isready >nul 2>&1 && goto :pgready
                    timeout /t 2 >nul
                )
                exit /b 1
                :pgready
                echo PostgreSQL READY

                echo Waiting for Redis...
                for /l %%i in (1,1,30) do (
                    docker exec redis_test redis-cli ping | findstr PONG >nul && goto :redisready
                    timeout /t 2 >nul
                )
                exit /b 1
                :redisready
                echo Redis READY
                '''
            }
        }

        stage('Run Tests') {
            steps {
                bat '''
                mvn -T 1C clean verify ^
                 -Dmaven.repo.local=%MAVEN_REPO% ^
                 -Dspring.profiles.active=test ^
                 -Dspring.datasource.url=jdbc:postgresql://localhost:%DB_PORT%/%DB_NAME% ^
                 -Dspring.datasource.username=%DB_USER% ^
                 -Dspring.datasource.password=%DB_PASS% ^
                 -Dspring.data.redis.host=localhost ^
                 -Dspring.data.redis.port=6379 ^
                 -Djwt.secretKey=ci-test-secret-key-long-enough-for-hmac-256-signing ^
                 --no-transfer-progress
                '''
            }
        }

        stage('Build JAR') {
            steps {
                bat '''
                mvn -T 1C package -DskipTests ^
                 -Dmaven.repo.local=%MAVEN_REPO% ^
                 --no-transfer-progress
                '''
            }
        }

        stage('Docker Build') {
            steps {
                bat '''
                docker build -t jobposting-app:latest .
                '''
            }
        }
    }

    post {
        always {
            bat '''
            echo Stopping containers...
            docker rm -f postgres_test redis_test 2>nul
            '''
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