pipeline {
    agent any

    environment {
        DB_URL         = 'jdbc:postgresql://localhost:5432/jobposting_test?useSSL=false'
        DB_USERNAME    = 'postgres'
        DB_PASSWORD    = 'postgres'
        JWT_SECRET     = 'ci-test-secret-key-long-enough-for-hmac-256-signing'
        REDIS_HOST     = 'localhost'
        REDIS_PORT     = '6379'
        DDL_AUTO       = 'create-drop'
    }

    tools {
        jdk 'JDK21'   // Must match the name configured in Jenkins > Global Tool Config
        maven 'Maven' // Same — must match your Jenkins Maven installation name
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Start Services') {
            steps {
                sh '''
                    docker run -d --name postgres_test \
                        -e POSTGRES_DB=jobposting_test \
                        -e POSTGRES_USER=postgres \
                        -e POSTGRES_PASSWORD=postgres \
                        -p 5432:5432 \
                        --health-cmd="pg_isready" \
                        postgres:15

                    docker run -d --name redis_test \
                        -p 6379:6379 \
                        redis:latest

                    # Wait for Postgres to be ready
                    until docker exec postgres_test pg_isready; do sleep 2; done

                    # Wait for Redis to be ready
                    until docker exec redis_test redis-cli ping | grep PONG; do sleep 2; done
                '''
            }
        }

        stage('Test') {
            steps {
                sh 'mvn test --no-transfer-progress'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml' // Publishes test results in Jenkins UI
                    archiveArtifacts artifacts: 'target/surefire-reports/**', allowEmptyArchive: true
                }
            }
        }

        stage('Build JAR') {
            steps {
                sh 'mvn package -DskipTests --no-transfer-progress'
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
            sh '''
                docker stop postgres_test redis_test || true
                docker rm   postgres_test redis_test || true
            '''
            cleanWs() // Clean workspace after build
        }
    }
}