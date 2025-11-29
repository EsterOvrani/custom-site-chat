pipeline {
    agent any
    
    environment {
        // תיקון בעיית ה-API version
        DOCKER_API_VERSION = '1.41'

        // Docker Registry
        DOCKER_REGISTRY = 'esterovrani'
        
        // Git commit message (מנוקה מתווים מיוחדים)
        GIT_COMMIT_MESSAGE = sh(
            script: "git log -1 --pretty=format:'%s' | sed 's/[^a-zA-Z0-9]/-/g' | tr '[:upper:]' '[:lower:]' | sed 's/--*/-/g' | sed 's/^-//' | sed 's/-\$//' | cut -c1-50",
            returnStdout: true
        ).trim()
        
        // Git commit hash קצר (לשילוב)
        GIT_COMMIT_SHORT = sh(
            script: "git rev-parse --short=7 HEAD",
            returnStdout: true
        ).trim()
        
        // Tag format: commit-message-hash (לייחודיות)
        IMAGE_TAG = "${GIT_COMMIT_MESSAGE}-${GIT_COMMIT_SHORT}"
        
        // Temporary build directory
        BUILD_DIR = "${WORKSPACE}/build"
    }
    
    stages {
        stage('📋 Display Build Info') {
            steps {
                script {
                    echo '📋 ====== BUILD INFORMATION ======'
                    sh '''
                        echo "Git Commit Message: $(git log -1 --pretty=format:'%s')"
                        echo "Git Commit Hash:    ${GIT_COMMIT_SHORT}"
                        echo "Sanitized Message:  ${GIT_COMMIT_MESSAGE}"
                        echo "Image Tag:          ${IMAGE_TAG}"
                        echo "Git Branch:         $(git rev-parse --abbrev-ref HEAD)"
                        echo "Git Author:         $(git log -1 --pretty=format:'%an')"
                        echo "Docker Registry:    ${DOCKER_REGISTRY}"
                        echo "=================================="
                    '''
                }
            }
        }
        
        stage('🧹 Cleanup Old Containers') {
            steps {
                script {
                    echo '🧹 Cleaning up old containers and images (preserving Jenkins)...'
                    sh '''
                        # שמור את ID של קונטיינר Jenkins
                        JENKINS_CONTAINER_ID=$(hostname)
                        
                        echo "Jenkins Container ID: $JENKINS_CONTAINER_ID (will be preserved)"
                        
                        # עצור docker-compose containers (אם יש)
                        docker-compose -f docker-compose.test.yml down -v 2>/dev/null || true
                        docker-compose down -v 2>/dev/null || true
                        
                        # עצור כל הcontainers חוץ מJenkins
                        docker ps -aq | grep -v ${JENKINS_CONTAINER_ID} | xargs -r docker stop 2>/dev/null || true
                        docker ps -aq | grep -v ${JENKINS_CONTAINER_ID} | xargs -r docker rm -f 2>/dev/null || true
                        
                        # נקה images ישנים (לא של Jenkins!)
                        docker images --format "{{.Repository}}:{{.Tag}} {{.ID}}" | grep -v "jenkins-jenkins" | awk '{print $2}' | xargs -r docker rmi -f 2>/dev/null || true
                        
                        # נקה volumes
                        docker volume prune -f || true
                        
                        echo "✅ Cleanup completed (Jenkins container preserved)"
                    '''
                }
            }
        }
        
        stage('📥 Checkout') {
            steps {
                echo '📥 Checking out code from Git...'
                checkout scm
            }
        }
        
        stage('🔐 Create TEST .env') {
            steps {
                script {
                    echo '🔐 Copying TEST .env file from secret file credential...'
                    
                    // שימוש ב-Secret File במקום Secret Text מרובים
                    withCredentials([file(credentialsId: 'env-file-test', variable: 'ENV_FILE')]) {
                        sh '''
                            # העתק את קובץ ה-.env מה-credential
                            cp "${ENV_FILE}" .env
                            
                            # וודא שהקובץ הועתק
                            if [ -f .env ]; then
                                echo "✅ TEST .env copied successfully from secret file"
                                echo "📋 Environment variables loaded:"
                                grep -E "^[A-Z_]+=" .env | cut -d'=' -f1 | while read var; do
                                    echo "   - $var"
                                done
                            else
                                echo "❌ ERROR: Failed to copy .env file"
                                exit 1
                            fi
                            
                            # וודא ש-TEST_MODE מופעל
                            if grep -q "TEST_MODE_ENABLED=true" .env; then
                                echo "✅ Confirmed: TEST_MODE_ENABLED=true"
                            else
                                echo "⚠️ WARNING: TEST_MODE_ENABLED=true not found in .env"
                            fi
                        '''
                    }
                }
            }
        }
        
        stage('🏗️ Build TEST Environment') {
            steps {
                echo '🏗️ Building TEST docker-compose images...'
                sh '''
                    # בנה את כל הimages (כולל Newman)
                    docker-compose -f docker-compose.test.yml build --no-cache
                    
                    echo "✅ TEST environment images built"
                '''
            }
        }
        
        stage('🚀 Start TEST Environment & Run Tests') {
            steps {
                script {
                    echo '🚀 Starting TEST environment...'
                    sh '''
                        # הרץ את כל השירותים וחכה שיהיו healthy
                        echo "⏳ Starting services and waiting for health checks..."
                        docker-compose -f docker-compose.test.yml up -d postgres qdrant backend frontend nginx
                        
                        # חכה שהבקנד יהיה healthy (docker-compose עושה את זה בשבילנו!)
                        echo "⏳ Waiting for backend to be healthy..."
                        docker-compose -f docker-compose.test.yml up -d --wait backend
                        
                        if [ $? -eq 0 ]; then
                            echo "✅ Backend is healthy and ready!"
                        else
                            echo "❌ Backend health check failed!"
                            docker-compose -f docker-compose.test.yml logs backend
                            exit 1
                        fi
                        
                        echo "🧪 Running Newman tests..."
                        # הרץ את Newman service
                        docker-compose -f docker-compose.test.yml up newman
                        
                        # בדוק exit code של Newman
                        NEWMAN_EXIT_CODE=$(docker inspect newman-tests --format='{{.State.ExitCode}}')
                        
                        echo "Newman exit code: $NEWMAN_EXIT_CODE"
                        
                        if [ "$NEWMAN_EXIT_CODE" != "0" ]; then
                            echo "❌ Newman tests failed!"
                            docker-compose -f docker-compose.test.yml logs newman
                            exit 1
                        fi
                        
                        echo "✅ All Newman tests passed!"
                    '''
                }
            }
            post {
                always {
                    sh 'docker-compose -f docker-compose.test.yml logs newman > newman-output.log 2>&1 || true'
                    archiveArtifacts artifacts: 'newman-output.log', allowEmptyArchive: true
                }
            }
        }
        
        stage('🗑️ Cleanup TEST Environment') {
            steps {
                script {
                    echo '🗑️ Stopping and removing TEST containers...'
                    sh '''
                        # עצור והסר את כל containers של הטסט כולל volumes
                        docker-compose -f docker-compose.test.yml down -v
                        
                        echo "✅ TEST environment cleaned up"
                    '''
                }
            }
        }
        
        stage('🔐 Create PRODUCTION .env') {
            steps {
                script {
                    echo '🔐 Copying PRODUCTION .env file from secret file credential...'
                    
                    // שימוש ב-Secret File לפרודקשן (ללא TEST_MODE)
                    withCredentials([file(credentialsId: 'env-file-prod', variable: 'ENV_FILE')]) {
                        sh '''
                            # מחק את .env הישן (של הטסט)
                            rm -f .env
                            
                            # העתק את קובץ ה-.env של פרודקשן
                            cp "${ENV_FILE}" .env
                            
                            # וודא שהקובץ הועתק
                            if [ -f .env ]; then
                                echo "✅ PRODUCTION .env copied successfully from secret file"
                            else
                                echo "❌ ERROR: Failed to copy .env file"
                                exit 1
                            fi
                            
                            # וודא ש-TEST_MODE לא מופעל בפרודקשן!
                            if grep -q "TEST_MODE_ENABLED=true" .env; then
                                echo "❌ CRITICAL ERROR: TEST_MODE_ENABLED=true found in PRODUCTION .env!"
                                echo "❌ This is a security risk! Please fix the env-file-prod credential."
                                exit 1
                            else
                                echo "✅ Confirmed: TEST_MODE_ENABLED is NOT true in production .env"
                            fi
                            
                            # וודא ש-BYPASS_EMAIL_VERIFICATION לא מופעל
                            if grep -q "BYPASS_EMAIL_VERIFICATION=true" .env; then
                                echo "❌ CRITICAL ERROR: BYPASS_EMAIL_VERIFICATION=true found in PRODUCTION .env!"
                                exit 1
                            else
                                echo "✅ Confirmed: Email verification is enabled in production"
                            fi
                        '''
                    }
                }
            }
        }
        
        stage('🏗️ Build PRODUCTION Images') {
            steps {
                echo '🏗️ Building PRODUCTION images (WITHOUT TEST_MODE)...'
                sh '''
                    # בנה רק backend ו-frontend (לא nginx או newman)
                    docker-compose build --no-cache backend frontend
                    
                    echo "✅ PRODUCTION images built successfully"
                    
                    # רשימת images
                    docker images | grep -E "backend|frontend"
                '''
            }
        }
        
        stage('🔍 Verify Production Images') {
            steps {
                script {
                    echo '🔍 Verifying production images do NOT contain TEST_MODE=true...'
                    sh '''
                        # בדוק שbackend-prod image לא מכיל TEST_MODE=true
                        docker run --rm --entrypoint env backend-prod:latest > /tmp/backend-env.txt || true
                        
                        if grep -q "TEST_MODE_ENABLED=true" /tmp/backend-env.txt; then
                            echo "❌ CRITICAL ERROR: TEST_MODE_ENABLED=true found in production image!"
                            exit 1
                        else
                            echo "✅ Confirmed: Production image is clean (TEST_MODE_ENABLED=false)"
                        fi
                        
                        rm -f /tmp/backend-env.txt
                    '''
                }
            }
        }
        
        stage('📦 Tag Production Images') {
            steps {
                script {
                    echo '📦 Tagging production images with Git commit message...'
                    sh '''
                        echo "Original commit message: $(git log -1 --pretty=format:'%s')"
                        echo "Sanitized tag: ${IMAGE_TAG}"
                        
                        # Tag backend with commit message and latest
                        docker tag backend-prod:latest ${DOCKER_REGISTRY}/custom-site-chat-backend:${IMAGE_TAG}
                        docker tag backend-prod:latest ${DOCKER_REGISTRY}/custom-site-chat-backend:latest
                        
                        # Tag frontend with commit message and latest
                        docker tag frontend-prod:latest ${DOCKER_REGISTRY}/custom-site-chat-frontend:${IMAGE_TAG}
                        docker tag frontend-prod:latest ${DOCKER_REGISTRY}/custom-site-chat-frontend:latest
                        
                        echo "✅ Images tagged for production deployment"
                        echo "   Backend:  ${DOCKER_REGISTRY}/custom-site-chat-backend:${IMAGE_TAG}"
                        echo "   Frontend: ${DOCKER_REGISTRY}/custom-site-chat-frontend:${IMAGE_TAG}"
                        echo "   (Also tagged as 'latest')"
                    '''
                }
            }
        }
        
        stage('🚢 Deploy to Registry') {
            steps {
                script {
                    echo '🚢 Pushing PRODUCTION images to registry...'
                    withCredentials([usernamePassword(
                        credentialsId: 'docker-registry-credentials',
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASS'
                    )]) {
                        sh '''
                            echo "${DOCKER_PASS}" | docker login -u "${DOCKER_USER}" --password-stdin
                            
                            # Push backend with commit message tag and latest
                            echo "📤 Pushing backend with tag: ${IMAGE_TAG}..."
                            docker push ${DOCKER_REGISTRY}/custom-site-chat-backend:${IMAGE_TAG}
                            docker push ${DOCKER_REGISTRY}/custom-site-chat-backend:latest
                            
                            # Push frontend with commit message tag and latest
                            echo "📤 Pushing frontend with tag: ${IMAGE_TAG}..."
                            docker push ${DOCKER_REGISTRY}/custom-site-chat-frontend:${IMAGE_TAG}
                            docker push ${DOCKER_REGISTRY}/custom-site-chat-frontend:latest
                            
                            docker logout
                            
                            echo "✅ Production images deployed successfully!"
                            echo ""
                            echo "📦 DEPLOYED IMAGES:"
                            echo "   Backend:  ${DOCKER_REGISTRY}/custom-site-chat-backend:${IMAGE_TAG}"
                            echo "   Backend:  ${DOCKER_REGISTRY}/custom-site-chat-backend:latest"
                            echo "   Frontend: ${DOCKER_REGISTRY}/custom-site-chat-frontend:${IMAGE_TAG}"
                            echo "   Frontend: ${DOCKER_REGISTRY}/custom-site-chat-frontend:latest"
                        '''
                    }
                }
            }
        }
    }
    
    post {
        always {
            script {
                echo '📊 Collecting logs...'
                sh 'docker-compose logs > docker-logs.txt 2>&1 || true'
                archiveArtifacts artifacts: 'docker-logs.txt', allowEmptyArchive: true
            }
        }
        
        success {
            script {
                echo '✅ ====== PIPELINE SUCCESS ======'
                sh '''
                    echo "📦 Production images deployed!"
                    echo ""
                    echo "📝 Git Commit Info:"
                    echo "   Message: $(git log -1 --pretty=format:'%s')"
                    echo "   Author:  $(git log -1 --pretty=format:'%an')"
                    echo "   Hash:    ${GIT_COMMIT_SHORT}"
                    echo ""
                    echo "🎯 Image Tag: ${IMAGE_TAG}"
                    echo ""
                    echo "🐳 Deployed Images:"
                    echo "   ${DOCKER_REGISTRY}/custom-site-chat-backend:${IMAGE_TAG}"
                    echo "   ${DOCKER_REGISTRY}/custom-site-chat-frontend:${IMAGE_TAG}"
                    echo ""
                    echo "✅ Pipeline completed successfully!"
                '''
            }
        }
        
        failure {
            echo '❌ Pipeline failed!'
            sh '''
                echo "📋 Current containers:"
                docker ps -a
                
                echo "📋 Recent logs:"
                docker-compose -f docker-compose.test.yml logs --tail=100 || true
            '''
        }
        
        cleanup {
            echo '🧹 ====== FINAL DEEP CLEANUP ======'
            sh '''
                echo "🛑 Step 1: Stopping all Docker Compose services with volumes..."
                docker-compose -f docker-compose.test.yml down -v 2>/dev/null || true
                docker-compose down -v 2>/dev/null || true
                
                echo "🗑️ Step 2: Removing all project images (preserving jenkins-jenkins)..."
                # מחק את כל ה-images של הפרויקט (לא jenkins-jenkins!)
                docker images --format "{{.Repository}}:{{.Tag}}" | grep -v "jenkins-jenkins" | grep -E "backend|frontend|postgres|qdrant|nginx|newman" | xargs -r docker rmi -f 2>/dev/null || true
                
                # מחק dangling images (לא jenkins-jenkins!)
                docker images -f "dangling=true" -q | xargs -r docker rmi -f 2>/dev/null || true
                
                echo "🧹 Step 3: Cleaning Docker builder cache..."
                docker builder prune -a -f
                
                echo "🗑️ Step 4: Removing unused volumes..."
                docker volume prune -f
                
                echo "🗑️ Step 5: Removing unused networks..."
                docker network prune -f
                
                echo "🧹 Step 6: Final system cleanup..."
                docker system prune -f
                
                echo "🗂️ Step 7: Removing .env file..."
                rm -f .env || true
                
                echo ""
                echo "📊 ====== CLEANUP SUMMARY ======"
                echo "Remaining containers:"
                docker ps -a
                echo ""
                echo "Remaining images:"
                docker images
                echo ""
                echo "Remaining volumes:"
                docker volume ls
                echo ""
                echo "✅ DEEP CLEANUP COMPLETED (jenkins-jenkins preserved)"
            '''
        }
    }
}