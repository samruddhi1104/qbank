pipeline {
    agent any

    parameters {
        choice(name: 'DEPLOY_ENV', choices: ['dev', 'test', 'prod'],
               description: 'Target environment')
        string(name: 'TOMCAT_URL', defaultValue: 'http://localhost:8081',
               description: 'Tomcat base URL')
    }

    environment {
        WAR_FILE     = 'target\\qbank-0.0.1-SNAPSHOT.war'
        CONTEXT_PATH = "/qbank-${params.DEPLOY_ENV}"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                bat 'mvn -B clean compile'
            }
        }

        stage('Package') {
            steps {
                bat 'mvn -B package -DskipTests'
                archiveArtifacts artifacts: 'target/*.war', fingerprint: true
            }
        }

        stage('Deploy') {
            steps {
                echo "Deploying to ${params.DEPLOY_ENV} at ${params.TOMCAT_URL}${env.CONTEXT_PATH}"
                withCredentials([usernamePassword(credentialsId: 'tomcat-creds',
                                 usernameVariable: 'TC_USER',
                                 passwordVariable: 'TC_PASS')]) {
                    bat '''
                    curl.exe --fail -u %TC_USER%:%TC_PASS% -T "%WAR_FILE%" "%TOMCAT_URL%/manager/text/deploy?path=%CONTEXT_PATH%&update=true"
                    '''
                }
            }
        }

        stage('Verify') {
            steps {
                bat 'curl.exe --fail -I "%TOMCAT_URL%%CONTEXT_PATH%/"'
            }
        }
    }

    post {
        success { echo "Deployed: ${params.TOMCAT_URL}${env.CONTEXT_PATH}/" }
        failure { echo 'Pipeline failed, check the stage logs.' }
    }
}
