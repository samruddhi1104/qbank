pipeline {
    agent any

    options {
        timeout(time: 30, unit: 'MINUTES')
    }

    parameters {
        choice(name: 'DEPLOY_ENV', choices: ['dev', 'test', 'prod'],
               description: 'Target environment for the Tomcat deployment')
        string(name: 'TOMCAT_URL', defaultValue: 'http://localhost:8081',
               description: 'Tomcat base URL')
    }

    environment {
        WAR_FILE      = 'target\\qbank-0.0.1-SNAPSHOT.war'
        CONTEXT_PATH  = "/qbank-${params.DEPLOY_ENV}"
        REGISTRY      = 'localhost:5000'
        IMAGE_NAME    = 'qbank'
        APP_CONTAINER = 'qbank-app'
        APP_PORT      = '9093'
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
                // One single step: start a private copy of the app on port 9091, run the
                // Selenium tests, then stop the app again before the step ends.
                // A failing test makes this step fail, so nothing below this stage runs.
                powershell '''
                    $ErrorActionPreference = 'Continue'
                    Get-NetTCPConnection -LocalPort 9091 -State Listen -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }
                    Copy-Item target/qbank-0.0.1-SNAPSHOT.war qbank-test-run.war -Force

                    $java = 'java'
                    if ($env:JAVA_HOME) { $java = $env:JAVA_HOME + '/bin/java.exe' }
                    $app = Start-Process -FilePath $java -ArgumentList '-jar','qbank-test-run.war','--server.port=9091' -RedirectStandardOutput app.log -RedirectStandardError app-err.log -WindowStyle Hidden -PassThru

                    $code = 1
                    try {
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
                        if (-not $ready) {
                            Write-Output 'App did not start on port 9091'
                            $code = 1
                        } else {
                            Write-Output 'App is up on port 9091, running Selenium tests'
                            mvn -B test "-Dtest=*UiTest" "-Dheadless=true" "-Dbase.url=http://localhost:9091"
                            $code = $LASTEXITCODE
                        }
                    } finally {
                        if ($app) { Stop-Process -Id $app.Id -Force -ErrorAction SilentlyContinue }
                        Get-NetTCPConnection -LocalPort 9091 -State Listen -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }
                    }
                    exit $code
                '''
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
                    archiveArtifacts artifacts: 'target/screenshots/*.png, app.log, app-err.log', allowEmptyArchive: true
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

        stage('Docker Build') {
            steps {
                script {
                    env.GIT_SHORT = bat(returnStdout: true, script: '@git rev-parse --short HEAD').trim()
                    env.IMAGE_TAG = "1.0.${env.BUILD_NUMBER}"
                }
                echo "Building image ${env.REGISTRY}/${env.IMAGE_NAME}:${env.IMAGE_TAG} from commit ${env.GIT_SHORT}"
                bat "docker build --label git.commit=%GIT_SHORT% -t %REGISTRY%/%IMAGE_NAME%:%IMAGE_TAG% -t %REGISTRY%/%IMAGE_NAME%:latest ."
            }
        }

        stage('Docker Push') {
            steps {
                bat "docker push %REGISTRY%/%IMAGE_NAME%:%IMAGE_TAG%"
                bat "docker push %REGISTRY%/%IMAGE_NAME%:latest"
                // registry evidence in the console log
                bat "curl.exe -s http://localhost:5000/v2/%IMAGE_NAME%/tags/list"
            }
        }

        stage('Docker Deploy') {
            steps {
                // remove the previous container, pull the new versioned image from the registry, start a fresh container
                bat "docker rm -f %APP_CONTAINER% || exit /b 0"
                bat "docker pull %REGISTRY%/%IMAGE_NAME%:%IMAGE_TAG%"
                bat "docker run -d --name %APP_CONTAINER% -p %APP_PORT%:8080 %REGISTRY%/%IMAGE_NAME%:%IMAGE_TAG%"
            }
        }

        stage('Docker Verify') {
            steps {
                powershell '''
                    $ErrorActionPreference = 'Continue'
                    $ready = $false
                    for ($i = 0; $i -lt 30; $i++) {
                        try {
                            $r = Invoke-WebRequest -Uri http://localhost:9093/questions -UseBasicParsing -TimeoutSec 3
                            if ($r.StatusCode -eq 200) { $ready = $true; break }
                        } catch {
                            Start-Sleep -Seconds 3
                        }
                    }
                    if (-not $ready) {
                        Write-Output 'Container did not respond on port 9093'
                        docker logs qbank-app
                        exit 1
                    }
                    Write-Output 'Container is serving the portal on port 9093'
                '''
                bat "docker ps --filter name=%APP_CONTAINER%"
            }
        }
    }

    post {
        success { echo "Pipeline OK. Tomcat: ${params.TOMCAT_URL}${env.CONTEXT_PATH}/  Docker: http://localhost:${env.APP_PORT}/questions (image ${env.IMAGE_TAG})" }
        failure { echo 'Pipeline failed, check the stage logs.' }
    }
}
