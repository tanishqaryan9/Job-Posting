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

                powershell -Command ^
                "for ($i=0; $i -lt 30; $i++) { ^
                    docker exec %POSTGRES_CONTAINER% pg_isready 2>$null; ^
                    if ($LASTEXITCODE -eq 0) { exit 0 } ^
                    Start-Sleep -Seconds 2 ^
                }; exit 1"

                echo PostgreSQL READY

                echo Waiting for Redis...

                powershell -Command ^
                "for ($i=0; $i -lt 30; $i++) { ^
                    docker exec %REDIS_CONTAINER% redis-cli ping 2>$null | findstr PONG; ^
                    if ($LASTEXITCODE -eq 0) { exit 0 } ^
                    Start-Sleep -Seconds 2 ^
                }; exit 1"

                echo Redis READY
                '''
            }
        }

        stage('Run Tests') {
            steps {
                bat '''
                mvn clean verify ^
                 --no-transfer-progress ^
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
                bat '''
                docker build -t todo-backend .
                '''
            }
        }
    }

    post {
        always {
            bat '''
            echo Stopping containers...
            docker rm -f %POSTGRES_CONTAINER% %REDIS_CONTAINER% 2>nul
            '''
            cleanWs()
        }

        success {
            echo '✅ CI Pipeline SUCCESS'
        }

        failure {
            echo '❌ CI Pipeline FAILED'
        }
    }
}