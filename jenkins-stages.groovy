buildImage = 'docker.knoopje.com/veon/scala-sbt:latest'
orcaImage = 'docker.knoopje.com/veon/orca:' + params.orca_version
registryCredentials = 'nexus-registry-login'
containerArgs = '-e DOCKER_HOST="172.17.0.1:2375" -e JAVA_TOOL_OPTIONS=-Duser.home=/tmp -v /home/jenkins/.docker/config.json:/.docker/config.json -v /var/cache/ivy2:/tmp/.ivy2:rw -v /var/cache/coursier:/tmp/.coursier:rw'
registryURL = 'https://docker.knoopje.com'
sbtMemory = '3072'
buildContainer = docker.image(buildImage)
orcaContainer = docker.image(orcaImage)

def advancedCheckout() {
    checkout([
            $class                           : 'GitSCM',
            branches                         : scm.branches,
            doGenerateSubmoduleConfigurations: scm.doGenerateSubmoduleConfigurations,
            extensions                       : [
                    [$class: 'CloneOption', noTags: false, shallow: false, depth: 0, reference: ''],
                    [$class: 'CleanBeforeCheckout']
            ],
            userRemoteConfigs                : scm.userRemoteConfigs
    ])
}

def buildJobProperties() {
    properties([
            pipelineTriggers(
                    [[
                             $class                       : 'com.dabsquared.gitlabjenkins.GitLabPushTrigger',
                             triggerOnPush                : true,
                             triggerOnMergeRequest        : true,
                             branchFilterType             : 'All',
                             triggerOpenMergeRequestOnPush: 'both',
                             setBuildDescription          : true,
                             secretToken                  : '6acd6d5547882f9e9665b84a4ec9adad',
                             triggerOnNoteRequest         : true,
                             noteRegex                    : ".*(j|J)enkins.*(build).*"
                     ]]
            ),
            buildDiscarder(
                    logRotator(
                            artifactDaysToKeepStr: '7',
                            artifactNumToKeepStr: '10',
                            daysToKeepStr: '7',
                            numToKeepStr: '10'
                    )
            ),
            [$class: 'GitLabConnectionProperty', gitLabConnection: 'Gitlab Test']
    ])
}

def checkfmtStep() {
    this.checkfmtStep("")
}

def checkfmtStep(String additionalCheckTasks) {
    stage('Check Format') {
        gitlabCommitStatus(name: 'Check Format') {
            timestamps {
                timeout(time: 3, unit: "MINUTES") {
                    buildContainer.inside(containerArgs) {
                        wrap([$class: 'AnsiColorBuildWrapper', 'colorMapName': 'XTerm']) {
                            sh 'sbt -mem ' + sbtMemory + ' compile:scalafmtCheck test:scalafmtCheck scalafmtSbtCheck ' + additionalCheckTasks
                        }
                    }
                }
            }
        }
    }
}

def dependencyCheck() {
    stage('CVE Check') {
        gitlabCommitStatus(name:'CVE Check') {
            timestamps {
                timeout(time: 10, unit: "MINUTES") {
                    buildContainer.inside(containerArgs) {
                        wrap([$class: 'AnsiColorBuildWrapper', 'colorMapName': 'XTerm']) {
                            sh 'sbt -mem ' + sbtMemory + ' dependencyCheck'
                        }
                    }
                    this.publishDependencyCheckTestReports()
                }
            }
        }
    }
}

private def publishDependencyCheckTestReports() {
    if (this.isMultiModuleProject()) {
        findFiles(glob: "**/dependency-check-report.html").each {
            String reportDir = it.path.replaceAll("dependency-check-report.html", "")
            module = it.path.replaceAll("/target.*", "").replaceAll("/", "-")
            this.publishHtmlReport(reportDir, "CVE Test Report ($module)", "dependency-check-report.html")
        }
    } else {
        findFiles(glob: "target/**/dependency-check-report.html").each {
            String reportDir = it.path.replaceAll("dependency-check-report.html", "")
            this.publishHtmlReport(reportDir, "CVE Test Report", "dependency-check-report.html")
        }
    }
}

def runUnitTestStep() {
    stage('Unit test') {
        gitlabCommitStatus(name: 'Build and Test') {
            timestamps {
                timeout(time: 15, unit: "MINUTES") {
                    try {
                        buildContainer.inside(containerArgs) {
                            wrap([$class: 'AnsiColorBuildWrapper', 'colorMapName': 'XTerm']) {
                                sh 'sbt -mem ' + sbtMemory + ' test'
                            }
                        }
                    } finally {
                        junit '**/target/test-reports/*.xml'
                    }
                }
            }
        }
    }
}

def publishStep(publishTask) {
    stage('Publish') {
        gitlabCommitStatus(name: 'Publish') {
            timestamps {
                timeout(time: 15, unit: "MINUTES") {
                    buildContainer.inside(containerArgs) {
                        wrap([$class: 'AnsiColorBuildWrapper', 'colorMapName': 'XTerm']) {
                            sh 'sbt -mem ' + sbtMemory + ' ' + publishTask
                        }
                    }
                }
            }
        }
    }
}

def publishCurrentTagReleaseNotes() {
    stage('Publish Release Notes') {
        gitlabCommitStatus(name: 'Publish Release Notes') {
            timestamps {
                timeout(time: 3, unit: "MINUTES") {
                    String curVersion = this.currentTag().replaceAll("v", "")
                    String prevVersion = this.previousTag().replaceAll("v", "")
                    buildContainer.inside(containerArgs) {
                        wrap([$class: 'AnsiColorBuildWrapper', 'colorMapName': 'XTerm']) {
                            withCredentials([string(credentialsId: 'backend_orchestra_deployment_access_token', variable: 'gitlab_access_token')]) {
                                sh "GITLAB_ACCESS_TOKEN=$gitlab_access_token sbt -mem $sbtMemory \"publishReleaseNotes $prevVersion $curVersion\""
                            }
                        }
                    }
                }
            }
        }
    }
}

String currentTag() {
    sh(script: "git tag --sort='-creatordate' --merged | head -1", returnStdout: true).trim()
}

String previousTag() {
    String current = this.currentTag()
    String otherTag = sh(script: "git tag --sort='-creatordate' --merged | head -2 | tail -1", returnStdout: true).trim()
    if (otherTag == current) {
        return ""
    } else {
        return otherTag
    }
}

def deployStep(deployJobName, envName) {
    stage('Deploy') {
        gitlabCommitStatus(name: 'Deploy') {
            timestamps {
                timeout(time: 45, unit: "MINUTES") {
                    def buildVersion = ''
                    buildContainer.inside(containerArgs) {
                        buildVersion = sh(script: "sbt -no-colors version 2> /dev/null | awk 'END{ print \$2}'", returnStdout: true).trim()
                    }
                    build(
                            job: deployJobName,
                            parameters: [
                                    string(name: 'Environment', value: envName),
                                    string(name: 'version', value: buildVersion)
                            ],
                            propagate: true,
                            wait: false
                    )
                }
            }
        }
    }
}

def intTestStep(itTestRunnerScript) {
    stage('Integration Test') {
        gitlabCommitStatus(name: 'Integration Test') {
            timestamps {
                timeout(time: 30, unit: "MINUTES") {
                    buildContainer.inside(containerArgs) {
                        wrap([$class: 'AnsiColorBuildWrapper', 'colorMapName': 'XTerm']) {
                            sh itTestRunnerScript
                        }
                    }
                }
            }
        }
    }
}

def validateSwaggerFiles() {
    def stageName = 'Validate Swagger Files'
    stage(stageName) {
        gitlabCommitStatus(name: stageName) {
            sh '''                    
                ret_code=0
                
                validate="docker run --rm -v $(pwd):/home -w /home quay.io/goswagger/swagger:0.13.0 validate"
                
                for swaggerSpec in $(find . -type f -name swagger.yml -not -path "*/target/*")
                do
                    echo "Validating $swaggerSpec"
                    $validate $swaggerSpec
                
                    ok=$?
                    if [ $ok != 0 ]; then ret_code=$ok; fi
                done
                
                exit $ret_cod
               '''
        }
    }
}

def dockerPull() {
    stage('Docker pull') {
        gitlabCommitStatus(name: 'Docker pull') {
            docker.withRegistry(registryURL, registryCredentials) {
                buildContainer.pull()
            }
        }
    }
}

def checkoutTag() {
    stage('Checkout Tag') {
        gitlabCommitStatus(name: 'Checkout Tag') {
            def tagName = this.currentTag()
            echo "BUILDING " + tagName
            checkout([
                    $class  : 'GitSCM',
                    branches: [[name: "refs/tags/" + tagName]]
            ])
        }
    }
}

//// DEPLOYMENT METHODS

void setDeploymentJobParameters() {
    def environments = this.environmentsFromFiles()
    properties([
            parameters([
                    choice(name: 'Environment',
                            choices: environments,
                            description: 'Name of the environment where services will be deployed'),
                    string(name: 'version',
                            description: "Version of the service(s) to deploy (without <code>v</code> prefix)"),
                    string(name: 'orca_version',
                            defaultValue: "latest",
                            description: "Version of orca to use for deploy"),
                    string(name: 'deploy_mask',
                            defaultValue: "**/deploy.json",
                            description: "File search mask to locate services' deploy.json files. " +
                                    "You can use it like <code>wayf/json.deploy</code> or <code>token*/deploy.json</code>"),
                    string(name: 'replicas',
                            defaultValue: "",
                            description: 'Set the desired # of replicas ' +
                                    'or leave it blank for the defaults (taken from <i>env.properties</i>)'),
                    string(name: 'deploy_description_git_ref',
                            defaultValue: "",
                            description: 'Git reference to commit with deployment description (<i>deploy.json</i> files and <i>properties</i>).' +
                                    '<br>By default, if left blank, it will be evaluated to <code>refs/tags/v{version}</code>.' +
                                    '<br>If you want to use a reference that is different to <code>version</code> parameter, then set the value to: <ul>' +
                                    '<li><code>refs/tags/v{version}</code></li> ' +
                                    '<li><code>refs/heads/{branch}</code></li> ' +
                                    '<li><code>{commit}</code></li> ' +
                                    '</ul>')
            ]),
            [$class: 'GitLabConnectionProperty', gitLabConnection: 'Gitlab Test'],
            buildDiscarder(
                    logRotator(
                            artifactDaysToKeepStr: '7',
                            artifactNumToKeepStr: '10',
                            daysToKeepStr: '7',
                            numToKeepStr: '10'
                    )
            )
    ])
}

String environmentsFromFiles() {
    String env = ""
    def files = findFiles(glob: "environments/*.properties")
    files.each {
        env += it.name.replace(".properties", "") + "\n"
    }
    return env
}

def checkoutDeploymentBranch() {
    stage("Checkout Deployment Description") {
        checkout([
                $class  : 'GitSCM',
                branches: [[name: this.deployDescriptionRef()]]
        ])
    }
}

void setDeploymentBuildDescription() {
    currentBuild.description = "#${BUILD_NUMBER} ${params.deploy_mask}:${params.version} from " +
            "${this.deployDescriptionRef()} to ${params.Environment}"
}

def readDeploymentEnvironmentProperties() {
    def environmentProperties = readProperties(file: "environments/${params.Environment}.properties")
    echo "Environment properties: ${environmentProperties}"
    return environmentProperties
}

def findAllDeployTemplates() {
    def templates = findFiles(glob: "${params.deploy_mask}")
    echo "Found ${templates.size()} json templates"
    return templates
}

void deployAllServices(def templates, def environmentProperties) {
    String orchestraAccessToken = this.orchestraLogin(environmentProperties.env_base_hostname)
    templates.each {
        this.deployService(it, environmentProperties, orchestraAccessToken)
    }
}

void deployService(def jsonFile, Map environmentProperties, orchestraAccessToken) {
    String jsonTemplate = readFile jsonFile.path
    def json = this.replacePlaceholders(jsonTemplate, environmentProperties)
    this.deploy(jsonFile.path, json, orchestraAccessToken)
    this.waitForService(jsonFile.path, json, environmentProperties.env_base_hostname, orchestraAccessToken)
}

String replacePlaceholders(String json, Map<String, String> environmentProperties) {
    json = this.replaceJsonPlaceholder(json, "version", params.version)
    json = this.replaceJsonPlaceholder(json, "timestamp", System.currentTimeMillis().toString())
    if (params.replicas != '') {
        json = this.replaceJsonPlaceholder(json, "replicas", params.replicas)
    }
    List<String> keys = new ArrayList<String>(environmentProperties.keySet())
    keys = keys.sort().reverse()
    keys.each {
        String value = environmentProperties.get(it)
        json = this.replaceJsonPlaceholder(json, it, value)
    }
    json
}

static String replaceJsonPlaceholder(String json, String placeholder, String value) {
    return json.replaceAll("\\\$$placeholder", value)
}

void deploy(def path, def json, String orchestra_access_token) {
    stage("Deploy ${path}") {
        docker.withRegistry(registryURL, registryCredentials) {
            orcaContainer.pull()
        }
        this.deployOrchestra(json, orchestra_access_token)
    }
}

def deployOrchestra(String json, String orchestra_access_token) {
    orcaContainer.inside {
        preparedJSON = readJSON text: json
        writeJSON file: 'deploy.json', json: preparedJSON
        sh "cat deploy.json | jq ."
        withEnv(["ORCHESTRA_TOKEN=${orchestra_access_token}"]) {
            try {
                sh "orca --stage ${params.Environment} service deploy deploy.json"
            } catch (e) {
                throw e
            } finally {
                sh "rm deploy.json"
            }
        }
    }
}

void waitForService(def path, def json, String baseHostName, String orchestra_access_token) {
    stage("Wait for ${path}") {
        timeout(time: 5, unit: "MINUTES") {
            Map jsonMap = readJSON text: json
            this.waitForService(baseHostName, jsonMap, orchestra_access_token)
        }
    }
}


void waitForService(String baseHostName, Map deploymentProps, orchestra_access_token) {
    def serviceName = deploymentProps.serviceName
    orcaContainer.inside {
        echo "Waiting for docker update to finish ..."
        waitUntil {
            withEnv(["ORCHESTRA_TOKEN=${orchestra_access_token}"]) {
                def updateState = sh(script: "orca --stage ${params.Environment} service info ${serviceName} | jq '.UpdateStatus.State'", returnStdout: true).trim().replaceAll('"', "")
                echo "Update state is: ${updateState}"
                // The following check covers swarm service update. Swarm takes care of checking container health.
                // Once the state changed to "completed" or does not have any status (when deploying for the first time)
                // we are checking haproxy side to make sure service responds to the world.
                return (updateState == "completed" || updateState == '')
            }
        }
    }
    if ("internal" != deploymentProps.serviceTags) {
        String path = ""
        if (deploymentProps["proxyRoutes"] && deploymentProps["proxyRoutes"][0]) {
            echo "Service specified proxyRoutes in deployment properties, will take first item as path"
            path = deploymentProps["proxyRoutes"][0]
        }
        String baseUrl = (this.sslDisabled(deploymentProps.serviceTags) ? "http://" : "https://") + baseHostName
        echo "Waiting for the ${serviceName} to become available in lb"
        waitUntil {
            def responseCode = sh(script: "curl -Isk -o /dev/null -w '%{http_code}' ${baseUrl}/${serviceName}/${path}", returnStdout: true).trim()
            echo "Response code returned by haproxy: ${responseCode}"
            return (responseCode.matches("[2-4]\\d\\d"))
        }
    }
}

static boolean sslDisabled(String serviceTag) {
    return serviceTag.equalsIgnoreCase("plain_rest") || serviceTag.equalsIgnoreCase("plain_restricted")
}

String orchestraLogin(String baseUrl) {
    if (this.isOrchestraSSOEnabled(baseUrl)) {
        withCredentials([usernamePassword(credentialsId: 'orca_sso_credentials', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
            orchestraResponse = sh(script: "curl -s -X POST -d '{\"username\":\"${USERNAME}\",\"password\":\"${PASSWORD}\"}' https://${baseUrl}/orchestra/login", returnStdout: true).trim()
            Map orchestraResponseJSON = readJSON text: orchestraResponse
            return orchestraResponseJSON.orchestraToken
        }
    } else {
        withCredentials([string(credentialsId: 'backend_orchestra_deployment_access_token', variable: 'orchestraToken')]) {
            return orchestraToken
        }
    }
}

boolean isOrchestraSSOEnabled(String baseUrl) {
    withCredentials([usernamePassword(credentialsId: 'orca_sso_credentials', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
        String orchestraResponseCode = sh(script: "curl -sk -o /dev/null -w '%{http_code}' -X POST -d '{\"username\":\"${USERNAME}\",\"password\":\"${PASSWORD}\"}' https://${baseUrl}/orchestra/login", returnStdout: true).trim()
        if (orchestraResponseCode == '200') {
            echo "SSO supported in this version of orchestra. Using jenkins credentials to log in."
            return true
        } else {
            echo "SSO is not supported. Using old token."
            return false
        }
    }
}

private def publishAllITestReports() {
    String itTestReportDir = "target/integration-test-html-report"
    if (this.isMultiModuleProject()) {
        findFiles(glob: "**/$itTestReportDir/index.html").each {
            String reportDir = it.path.replaceAll("index.html", "")
            module = it.path.replaceAll("/target.*", "").replaceAll("/", "-")
            this.publishHtmlReport(reportDir, "Integration Test Report ($module)", 'index.html')
        }
    } else {
        this.publishHtmlReport(itTestReportDir, "Integration Test Report", 'index.html')
    }
}

private boolean isMultiModuleProject() {
    isMultiModule = false
    findFiles(glob: "*/**/target/*").each {
        isMultiModule = true
    }
    isMultiModule
}

private void publishHtmlReport(String reportDir, String reportName, String reportFile) {
    print "PUBLISH test report for $reportDir $reportName"
    publishHTML([
            allowMissing         : true,
            alwaysLinkToLastBuild: true,
            keepAll              : true,
            reportDir            : reportDir,
            reportName           : reportName,
            reportFiles          : reportFile,
            reportTitles         : ''
    ])
}

private String deployDescriptionRef() {
    if (params.deploy_description_git_ref == "") {
        if (params.version.matches("\\d+\\.\\d+.*")) {
            return "refs/tags/v" + params.version
        } else {
            return params.version
        }
    } else {
        return params.deploy_description_git_ref
    }
}

String repoName() {
    sh(
            returnStdout: true,
            script: 'REPO_URL=$(git config --get remote.origin.url); basename "$REPO_URL" ".${REPO_URL##*.}"'
    ).trim()
}

void initQaTestJob(String jobName) {
    stage("Notify $jobName") {
        build(
                job: jobName,
                parameters: [
                        string(name: 'Environment', value: "${params.Environment}"),
                        string(name: 'TriggerRepository', value: repoName())
                ],
                propagate: true,
                wait: false
        )
    }
}


return this
