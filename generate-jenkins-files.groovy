buildImage = 'docker.knoopje.com/veon/scala-sbt:latest'
registryCredentials = 'nexus-registry-login'
containerArgs = '-e JAVA_TOOL_OPTIONS=-Duser.home=/tmp -v /var/cache/ivy2:/tmp/.ivy2:rw -v /var/cache/coursier:/tmp/.coursier:rw'
registryURL = 'https://docker.knoopje.com'
buildContainer = docker.image(buildImage)
generateJenkinsFilesName = 'Generate Jenkins Files'

def generateStagesJenkinsFile() {
    stage(generateJenkinsFilesName) {
        timestamps {
            timeout(time: 5, unit: "MINUTES") {
                gitlabCommitStatus(name:generateJenkinsFilesName) {
                    docker.withRegistry(registryURL, registryCredentials) {
                        buildContainer.pull()
                        buildContainer.inside(containerArgs) {
                            sh 'sbt -no-colors -mem 3072 generateStagesJenkinsFile'
                        }
                    }
                }
            }
        }
    }
}

return this
