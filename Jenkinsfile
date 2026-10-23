pipeline {
    agent any

    // NOTE FOR VIVA / OS COMPATIBILITY (Unit 5: CI/CD Pipeline):
    // On Windows Jenkins runners, replace 'sh' with 'bat' (e.g., bat 'npm ci').
    // On Linux / macOS Jenkins runners, use 'sh' as written below.

    stages {
        stage('Backend Install') {
            steps {
                dir('backend') {
                    // Installs Node backend production dependencies cleanly
                    sh 'npm ci'
                    // Windows alternative: bat 'npm ci'
                }
            }
        }

        stage('Frontend Install and Build') {
            steps {
                dir('frontend') {
                    // Installs Vite dependencies and executes production bundle build
                    sh 'npm ci'
                    sh 'npm run build'
                    // Windows alternative:
                    // bat 'npm ci'
                    // bat 'npm run build'
                }
            }
        }

        stage('Tomcat App Package') {
            steps {
                dir('tomcat-app') {
                    // Compiles Java Servlet and packages WAR archive
                    sh 'mvn clean package -DskipTests'
                    // Windows alternative: bat 'mvn clean package -DskipTests'
                }
            }
        }
    }

    post {
        success {
            echo 'CI Pipeline Succeeded: All backend, frontend, and Java components built cleanly!'
        }
        failure {
            echo 'CI Pipeline Failed: Review the console log above to diagnose errors.'
        }
    }
}
