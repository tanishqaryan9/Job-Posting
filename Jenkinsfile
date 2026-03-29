pipeline {
    agent any

    tools {
        jdk 'JDK25'
        maven 'Maven'
    }

    environment {
        POSTGRES_CONTAINER = "postgres_test"
        REDIS_CONTAINER    = "redis_test"
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
                docker rm -f %POSTGRES_CONTAINER% %REDIS_CONTAINER% 2>nul

                echo Starting PostgreSQL...
                docker run -d --name %POSTGRES_CONTAINER% ^
                  -e POSTGRES_DB=jobposting_test ^
                  -e POSTGRES_USER=postgres ^
                  -e POSTGRES_PASSWORD=postgres ^
                  -p 5433:5432 postgres:15

                echo Starting Redis...
                docker run -d --name %REDIS_CONTAINER% ^
                  -p 6379:6379 redis:latest
                '''
            }
        }

        stage('Wait For Services') {
            steps {
                bat '''
                echo Waiting for PostgreSQL...
                :waitpg
                docker exec %POSTGRES_CONTAINER% pg_isready >nul 2>&1
                if errorlevel 1 (
                    timeout /t 3 >nul
                    goto waitpg
                )
                echo PostgreSQL READY

                echo Waiting for Redis...
                :waitredis
                docker exec %REDIS_CONTAINER% redis-cli ping | findstr PONG >nul
                if errorlevel 1 (
                    timeout /t 3 >nul
                    goto waitredis
                )
                echo Redis READY
                '''
            }
        }

        stage('Run Tests') {
            steps {
                bat '''
                mvn clean verify ^
                 -Dspring.profiles.active=test ^
                 -Dspring.datasource.url=jdbc:postgresql://localhost:5433/jobposting_test ^
                 -Dspring.datasource.username=postgres ^
                 -Dspring.datasource.password=postgres ^
                 -Dspring.data.redis.host=localhost ^
                 -Dspring.data.redis.port=6379 ^
                 -Djwt.secretKey=ci-test-secret-key-long-enough-for-hmac-256-signing ^
                 -Dspring.jpa.hibernate.ddl-auto=create-drop
                '''
            }
        }

        stage('Build JAR') {
            steps {
                bat 'mvn -DskipTests package'
            }
        }

        stage('Docker Build') {
            steps {
                bat 'docker build -t todo-backend .'
            }
        }
    }

    post {
        success {
            echo '✅ CI Pipeline SUCCESS'
        }
        failure {
            echo '❌ CI Pipeline FAILED'
        }
        always {
            bat 'docker rm -f %POSTGRES_CONTAINER% %REDIS_CONTAINER% 2>nul'
            cleanWs()
        }
    }
}