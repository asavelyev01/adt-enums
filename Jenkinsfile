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

        stages.checkfmtStep()

        if (isReleaseTagBuild()) {
            stages.dependencyCheck()
        }

        stages.runUnitTestStep()

        if (isReleaseTagBuild()) {
            stages.publishStep('publish')
            stages.publishCurrentTagReleaseNotes()
        }

        gitlabCommitStatus(name:'Build Passed') {}
    }
}

boolean isReleaseTagBuild() {
    // jobs in the tags folder are considered release builds
    env.JOB_NAME.contains("tags/")
}
