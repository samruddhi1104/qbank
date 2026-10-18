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

        stage('UI Tests') {
            steps {
                // start a private copy of the app on port 9091 for the Selenium tests
                withEnv(['JENKINS_NODE_COOKIE=dontKillMe']) {
                    powershell '''
                        Get-NetTCPConnection -LocalPort 9091 -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }
                        Copy-Item target/qbank-0.0.1-SNAPSHOT.war qbank-test-run.war -Force
                        Start-Process -FilePath java -ArgumentList '-jar','qbank-test-run.war','--server.port=9091' -RedirectStandardOutput app.log -RedirectStandardError app-err.log -WindowStyle Hidden
                        $ready = $false
                        for ($i = 0; $i -lt 40; $i++) {
                            try {
                                Invoke-WebRequest -Uri http://localhost:9091/questions -UseBasicParsing -TimeoutSec 3 | Out-Null
                                $ready = $true
                                break
                            } catch {
                                Start-Sleep -Seconds 3
                            }
                        }
                        if (-not $ready) { Write-Error 'App did not start on port 9091'; exit 1 }
                    '''
                }
                // a failing test makes this command fail, which stops the pipeline before Deploy
                bat 'mvn -B test -Dtest=*UiTest -Dheadless=true -Dbase.url=http://localhost:9091'
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
                    archiveArtifacts artifacts: 'target/screenshots/*.png, app.log, app-err.log', allowEmptyArchive: true
                    powershell '''
                        Get-NetTCPConnection -LocalPort 9091 -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }
                    '''
                }
            }
        }

        stage('Deploy') {
            steps {
                echo "Deploying to ${params.DEPLOY_ENV} at ${params.TOMCAT_URL}${env.CONTEXT_PATH}"
                withCredentials([usernamePassword(credentialsId: 'tomcat-creds',
                                 usernameVariable: 'TC_USER',
                                 passwordVariable: 'TC_PASS')]) {
                    bat "curl.exe --fail -u %TC_USER%:%TC_PASS% -T \"%WAR_FILE%\" \"${params.TOMCAT_URL}/manager/text/deploy?path=${env.CONTEXT_PATH}&update=true\""
                }
            }
        }

        stage('Verify') {
            steps {
                bat "curl.exe --fail -I \"${params.TOMCAT_URL}${env.CONTEXT_PATH}/\""
            }
        }
    }

    post {
        success { echo "Deployed: ${params.TOMCAT_URL}${env.CONTEXT_PATH}/" }
        failure { echo 'Pipeline failed, check the stage logs.' }
    }
}
