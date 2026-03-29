pipeline {
    agent any

    tools {
        jdk 'JDK25'     // MUST match Global Tool name
        maven 'Maven'
    }

    environment {
        DB_URL      = 'jdbc:postgresql://localhost:5432/jobposting_test'
        DB_USERNAME = 'postgres'
        DB_PASSWORD = 'postgres'

        JWT_SECRET  = 'ci-test-secret-key-long-enough-for-hmac-256-signing'

        REDIS_HOST  = 'localhost'
        REDIS_PORT  = '6379'

        DDL_AUTO    = 'create-drop'

        MAVEN_REPO  = 'C:\\ProgramData\\Jenkins\\.m2\\repository'
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Verify Java & Maven') {
            steps {
                bat 'java -version'
                bat 'mvn -version'
            }
        }

        stage('Start Services') {
            steps {
                bat '''
                echo Cleaning old containers...
                docker rm -f postgres_test redis_test 2>nul

                echo Starting Postgres...
                docker run -d --name postgres_test ^
                  -e POSTGRES_DB=jobposting_test ^
                  -e POSTGRES_USER=postgres ^
                  -e POSTGRES_PASSWORD=postgres ^
                  -p 5432:5432 postgres:15

                echo Starting Redis...
                docker run -d --name redis_test ^
                  -p 6379:6379 redis:latest
                '''

                bat '''
                echo Waiting for Postgres...
                set retries=30

                :pgloop
                docker exec postgres_test pg_isready >nul 2>&1
                if %errorlevel%==0 goto pgready
                set /a retries-=1
                if %retries% LEQ 0 exit /b 1
                ping -n 3 127.0.0.1 >nul
                goto pgloop

                :pgready
                echo Postgres READY
                '''

                bat '''
                echo Waiting for Redis...
                set retries=30

                :redisloop
                docker exec redis_test redis-cli ping | findstr PONG >nul
                if %errorlevel%==0 goto redisready
                set /a retries-=1
                if %retries% LEQ 0 exit /b 1
                ping -n 3 127.0.0.1 >nul
                goto redisloop

                :redisready
                echo Redis READY
                '''
            }
        }

        stage('Test') {
            steps {
                bat '''
                mvn test --no-transfer-progress ^
                "-Dspring.profiles.active=test" ^
                "-Dspring.datasource.url=%DB_URL%" ^
                "-Dspring.datasource.username=%DB_USERNAME%" ^
                "-Dspring.datasource.password=%DB_PASSWORD%" ^
                "-Djwt.secretKey=%JWT_SECRET%" ^
                "-Dspring.data.redis.host=%REDIS_HOST%" ^
                "-Dspring.data.redis.port=%REDIS_PORT%" ^
                "-Dspring.jpa.hibernate.ddl-auto=%DDL_AUTO%" ^
                "-Dmaven.repo.local=%MAVEN_REPO%"
                '''
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
                bat '''
                mvn package -DskipTests --no-transfer-progress ^
                "-Dmaven.repo.local=%MAVEN_REPO%"
                '''
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
            bat '''
            echo Cleaning containers...
            docker rm -f postgres_test redis_test 2>nul
            '''
            cleanWs()
        }
    }
}