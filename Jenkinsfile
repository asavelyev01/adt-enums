node('scala-non-root') {
    gitlabBuilds(builds: ['Build Passed']) {

        stage('Checkout') {
            checkout scm
        }

        def generate = load 'generate-jenkins-files.groovy'
        generate.generateStagesJenkinsFile()

        def stages = load 'jenkins-stages.groovy'

        stages.advancedCheckout()

        stages.buildJobProperties()

        stages.dockerPull()

        if (isReleaseTagBuild()) {
            stages.checkoutTag()
        }

        stages.validateSwaggerFiles()

        if (isReleaseTagBuild() || isDeployBuild()) {
            stages.dependencyCheck()
        }

        stages.runUnitTestStep()

        if (isReleaseTagBuild() || isDeployBuild()) {
            stages.publishStep('docker:publish')
        } else {
            stages.publishStep('docker:publishLocal')
        }

        if (isReleaseTagBuild()) {
            stages.publishCurrentTagReleaseNotes()
        }

        if (isReleaseTagBuild() || isDeployBuild()) {
            stages.deployStep("/backend/deploy/${stages.repoName()}", 'dev')
        }

        gitlabCommitStatus(name:'Build Passed') {}
    }
}

boolean isReleaseTagBuild() {
    // jobs in the tags folder are considered release builds
    env.JOB_NAME.contains("backend/tags/")
}

boolean isDeployBuild() {
    isReleaseTagBuild() || env.BRANCH_NAME.equals("master")
}
