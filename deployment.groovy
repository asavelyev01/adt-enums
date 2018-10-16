node('scala-non-root') {
    stage("Checkout") {
        checkout scm
    }

    def generate = load 'generate-jenkins-files.groovy'
    generate.generateStagesJenkinsFile()

    def stages = load 'jenkins-stages.groovy'

    stages.advancedCheckout()

    stages.setDeploymentJobParameters()

    stages.checkoutDeploymentBranch()

    stages.setDeploymentBuildDescription()

    def templates = stages.findAllDeployTemplates()

    def environmentProperties = stages.readDeploymentEnvironmentProperties()

    stages.deployAllServices(templates, environmentProperties)

    stages.initQaTestJob("QA/test-ci-trigger")
}
