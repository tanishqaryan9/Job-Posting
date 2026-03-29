pipeline {
    agent any

    /**********************
     * TOOLS
     **********************/
    tools {
        jdk   'JDK25'      // must match Jenkins Global Tool name
        maven 'Maven'
    }

    /**********************
     * ENVIRONMENT
     **********************/
    environment {
        JAVA_HOME = tool('JDK25')
        PATH = "${JAVA_HOME}\\bin;${env.PATH}"

        DB_URL      = 'jdbc:postgresql://localhost:5433/jobposting_test'
        DB_USERNAME = 'postgres'
        DB_PASSWORD = 'postgres'

        REDIS_HOST  = 'localhost'
        REDIS_PORT  = '6379'

        JWT_SECRET  = 'ci-test-secret-key-long-enough-for-hmac-256-signing'
        DDL_AUTO    = 'create-drop'

        MAVEN_REPO  = 'C:\\ProgramData\\Jenkins\\.m2\\repository'
    }

    stages {

        /**********************
         * CHECKOUT
         **********************/
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        /**********************
         * START DATABASES
         **********************/
        stage('Start Test Services') {
            steps {
                bat '''
                echo Cleaning old containers...
                docker rm -f postgres_test redis_test 2>nul

                echo Starting PostgreSQL...
                docker run -d --name postgres_test ^
                  -e POSTGRES_DB=jobposting_test ^
                  -e POSTGRES_USER=postgres ^
                  -e POSTGRES_PASSWORD=postgres ^
                  -p 5433:5432 postgres:15

                echo Starting Redis...
                docker run -d --name redis_test ^
                  -p 6379:6379 redis:latest
                '''

                bat '''
                echo Waiting for PostgreSQL...
                :waitpg
                docker exec postgres_test pg_isready >nul 2>&1
                if errorlevel 1 (
                    timeout /t 3 >nul
                    goto waitpg
                )
                echo PostgreSQL READY
                '''

                bat '''
                echo Waiting for Redis...
                :waitredis
                docker exec redis_test redis-cli ping 2>nul | findstr PONG >nul
                if errorlevel 1 (
                    timeout /t 3 >nul
                    goto waitredis
                )
                echo Redis READY
                '''
            }
        }

        /**********************
         * RUN TESTS
         **********************/
        stage('Run Tests') {
            steps {
                bat '''
                mvn clean verify --no-transfer-progress ^
                  -Dmaven.repo.local=%MAVEN_REPO% ^
                  -Dspring.profiles.active=test ^
                  -Dspring.datasource.url=%DB_URL% ^
                  -Dspring.datasource.username=%DB_USERNAME% ^
                  -Dspring.datasource.password=%DB_PASSWORD% ^
                  -Dspring.data.redis.host=%REDIS_HOST% ^
                  -Dspring.data.redis.port=%REDIS_PORT% ^
                  -Djwt.secretKey=%JWT_SECRET% ^
                  -Dspring.jpa.hibernate.ddl-auto=%DDL_AUTO%
                '''
            }

            post {
                always {
                    junit allowEmptyResults: true,
                          testResults: 'target/surefire-reports/*.xml'

                    archiveArtifacts artifacts: 'target/surefire-reports/**',
                                      allowEmptyArchive: true
                }
            }
        }

        /**********************
         * BUILD ARTIFACT
         **********************/
        stage('Build JAR') {
            steps {
                bat '''
                mvn package -DskipTests --no-transfer-progress ^
                  -Dmaven.repo.local=%MAVEN_REPO%
                '''
            }

            post {
                success {
                    archiveArtifacts artifacts: 'target/*.jar',
                                      fingerprint: true
                }
            }
        }
    }

    /**********************
     * CLEANUP
     **********************/
    post {
        always {
            bat '''
            echo Stopping containers...
            docker stop postgres_test redis_test 2>nul
            docker rm postgres_test redis_test 2>nul
            '''

            cleanWs()
        }
    }
}