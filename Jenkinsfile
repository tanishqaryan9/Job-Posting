pipeline {
    agent any
    environment {
        DB_URL         = 'jdbc:postgresql://localhost:5432/jobposting_test'
        DB_USERNAME    = 'postgres'
        DB_PASSWORD    = 'postgres'
        JWT_SECRET     = 'ci-test-secret-key-long-enough-for-hmac-256-signing'
        REDIS_HOST     = 'localhost'
        REDIS_PORT     = '6379'
        DDL_AUTO       = 'create-drop'
    }
    tools {
        jdk   'JDK21'
        maven 'Maven'
    }
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        stage('Start Services') {
            steps {
                bat '''
                    docker rm -f postgres_test redis_test 2>nul & exit /b 0
                    docker run -d --name postgres_test -e POSTGRES_DB=jobposting_test -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres -p 5432:5432 postgres:15
                    docker run -d --name redis_test -p 6379:6379 redis:latest
                '''
                bat '''
                    :waitpg
                    docker exec postgres_test pg_isready >nul 2>&1
                    if errorlevel 1 (
                        ping -n 3 127.0.0.1 >nul
                        goto waitpg
                    )
                    echo Postgres is ready
                '''
                bat '''
                    :waitredis
                    docker exec redis_test redis-cli ping 2>nul | findstr /C:"PONG" >nul
                    if errorlevel 1 (
                        ping -n 3 127.0.0.1 >nul
                        goto waitredis
                    )
                    echo Redis is ready
                '''
            }
        }
        stage('Test') {
            steps {
                bat 'mvn test --no-transfer-progress -Dspring.datasource.url=%DB_URL% -Dspring.datasource.username=%DB_USERNAME% -Dspring.datasource.password=%DB_PASSWORD% -Djwt.secretKey=%JWT_SECRET% -Dspring.data.redis.host=%REDIS_HOST% -Dspring.data.redis.port=%REDIS_PORT% -Dspring.jpa.hibernate.ddl-auto=%DDL_AUTO%'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                    archiveArtifacts artifacts: 'target/surefire-reports/**', allowEmptyArchive: true
                }
            }
        }
        stage('Build JAR') {
            steps {
                bat 'mvn package -DskipTests --no-transfer-progress -Dmaven.repo.local=C:\\ProgramData\\Jenkins\\.m2\\repository'
            }
            post {
                success {
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                }
            }
        }
    }
    post {
        always {
            bat 'docker stop postgres_test redis_test 2>nul & exit /b 0'
            bat 'docker rm postgres_test redis_test 2>nul & exit /b 0'
            cleanWs()
        }
    }
}