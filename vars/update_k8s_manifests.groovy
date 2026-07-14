#!/usr/bin/env groovy

def call(Map config = [:]) {

    def imageTag = config.imageTag ?: error("Image tag is required")
    def appImage = config.appImage ?: error("App image is required")
    def migrationImage = config.migrationImage ?: error("Migration image is required")

    def manifestsPath = config.manifestsPath ?: 'kubernetes'
    def gitCredentials = config.gitCredentials ?: 'github-credentials'
    def gitUserName = config.gitUserName ?: 'Jenkins CI'
    def gitUserEmail = config.gitUserEmail ?: 'jenkins@ci.local'

    echo "Updating Kubernetes manifests with image tag: ${imageTag}"

    withCredentials([
        usernamePassword(
            credentialsId: gitCredentials,
            usernameVariable: 'GIT_USERNAME',
            passwordVariable: 'GIT_PASSWORD'
        )
    ]) {

        sh """
        git config user.name "${gitUserName}"
        git config user.email "${gitUserEmail}"

        sed -i "s|image: .*qbshop-app:.*|image: ${appImage}:${imageTag}|g" ${manifestsPath}/08-qbshop-deployment.yaml

        if [ -f "${manifestsPath}/12-migration-job.yaml" ]; then
            sed -i "s|image: .*qbshop-migration:.*|image: ${migrationImage}:${imageTag}|g" ${manifestsPath}/12-migration-job.yaml
        fi

        if git diff --quiet; then
            echo "No changes to commit"
        else
            git add ${manifestsPath}/*.yaml
            git commit -m "Update image tag to ${imageTag} [ci skip]"
            git push origin HEAD:${GIT_BRANCH}
        fi
        """
    }
}
