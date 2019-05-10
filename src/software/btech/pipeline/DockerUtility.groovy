#!groovy
package software.btech.pipeline


/**
 * Docker utility class with reusable pipeline functions based on image bsantanna/jenkins-docker-agent
 */
class DockerUtility extends AbstractPipelineUtility {

  final String proxy

  /**
   * Constructor with pipeline reference injection.
   * @param pipeline pipeline being executed
   */
  DockerUtility(Script pipeline) {
    this(pipeline, null)
  }

  /**
   * Constructor with pipeline reference injection and proxy property set
   * @param pipeline
   * @param proxy
   */
  DockerUtility(Script pipeline, String proxy) {
    super(pipeline)
    this.proxy = proxy
  }

  /**
   * Build image
   * @param buildContext
   * @param baseTag
   * @param tag
   */
  void buildImage(def buildContext, def baseTag, def tag) {
    print("BUILDING DOCKER IMAGE")
    print("\tTag: ${tag}")
    print("\tBase Tag: ${baseTag}")
    print("\tBuild Context: ${buildContext}")
    this.pipeline.dir(buildContext) {
      this.pipeline.sh "docker pull ${baseTag}"
      this.pipeline.sh "docker build -t ${tag} ."
    }
  }

  /**
   * Removes all pipeline, restarts daemon service and sleeps for cooldown time
   *
   * @param timeoutInSeconds time in seconds after restarting
   */
  void dockerDaemonRestart(def timeoutInSeconds) {
    print("RESTARTING DOCKER DAEMON...")
    this.pipeline.sh "docker stop \$(docker ps -aq) && docker rm \$(docker ps -aq) || true"
    this.pipeline.sh "\$(service docker stop && sleep ${timeoutInSeconds}) || true"
    if (this.proxy != null) {
      this.pipeline.sh "mkdir /etc/docker || true"
      String proxyConfigCommand = "echo '{\"insecure-registries\":[\"http://"
      proxyConfigCommand += proxy + "\"],\"experimental\":false,\"debug\":false,\"registry-mirrors\":[\"http://"
      proxyConfigCommand += proxy + "\"]}' > /etc/docker/daemon.json"
      print("SETTING UP PROXY FOR DOCKER DAEMON: ")
      print(proxyConfigCommand)
      this.pipeline.sh proxyConfigCommand
    }
    this.pipeline.sh "\$(service docker start && sleep ${timeoutInSeconds}) || true"
    print("DOCKER DAEMON RESTART COMPLETE")
  }

  /**
   * Logs in into Docker registry.
   * @param registryCredentialsId credentials id configured in Jenkins.
   */
  void registryLogin(def registryCredentialsId) {
    print("PERFORMING REGISTRY LOGIN...")
    // perform inside credential injection block
    this.pipeline.withCredentials([[$class          : 'UsernamePasswordMultiBinding',
                                    credentialsId   : registryCredentialsId,
                                    usernameVariable: 'DOCKER_REGISTRY_USERNAME',
                                    passwordVariable: 'DOCKER_REGISTRY_PASSWORD']]) {

      this.pipeline.sh "" +
          "docker login " +
          "-u ${this.pipeline.env.DOCKER_REGISTRY_USERNAME} " +
          "-p ${this.pipeline.env.DOCKER_REGISTRY_PASSWORD}"

      print("LOGIN COMPLETE")
    }
  }

  /**
   * Run Docker container with given arguments.
   *
   * @param tag
   * @param volumeSource
   * @param volumeDestination
   */
  void runContainer(def tag, def volumeSource, def volumeDestination) {
    print("RUNNING DOCKER CONTAINER")
    print("\tTag: ${tag}")
    print("\tVolume Source: ${volumeSource}")
    print("\tVolume Destination: ${volumeDestination}")
    this.pipeline.sh "docker pull ${tag} || true"
    this.pipeline.sh "docker run -i --rm -v ${volumeSource}:${volumeDestination} ${tag}"
  }

  /**
   * Run Docker container with given arguments
   * @param tag
   * @param volumeSource
   * @param volumeDestination
   * @param command
   */
  void runContainerWithCommand(def tag, def volumeSource, def volumeDestination, def command) {
    print("RUNNING DOCKER CONTAINER WITH COMMAND")
    print("\tTag: ${tag}")
    print("\tVolume Source: ${volumeSource}")
    print("\tVolume Destination: ${volumeDestination}")
    print("\tCommand: ${command}")
    this.pipeline.sh "docker pull ${tag} || true"
    this.pipeline.sh "docker run -i --rm -v ${volumeSource}:${volumeDestination} ${tag} ${command}"
  }

}
