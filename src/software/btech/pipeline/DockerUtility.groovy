#!groovy
package software.btech.pipeline


/**
 * Docker utility class with reusable pipeline functions based on image bsantanna/jenkins-docker-agent
 */
class DockerUtility extends AbstractPipelineUtility {

  final Map<String, String> configuration

  /**
   * Constructor with pipeline reference injection.
   * @param pipeline pipeline being executed
   */
  DockerUtility(Script pipeline) {
    this(pipeline, null)
  }

  /**
   * Constructor with pipeline reference injection and configuration map
   * @param pipeline
   * @param configuration
   */
  DockerUtility(Script pipeline, Map<String, String> configuration) {
    super(pipeline)
    this.configuration = configuration
  }

  /**
   * Build image
   * @param buildContext
   * @param baseTag
   * @param tag
   */
  void buildImage(String buildContext, String baseTag, String tag) {
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

    String setupKey = "skipDaemonSetup"
    if (!this.configuration.containsKey(setupKey) || !this.configuration.get(setupKey)) {

      String configCommand = "echo '{\"experimental\":false, \"debug\":false, \"storage-driver\":\"vfs\""
      if (this.configuration.containsKey("proxy")) {
        configCommand += ", \"insecure-registries\":[\"http://" + this.configuration.get("proxy") + "\"]"
        configCommand += ", \"registry-mirrors\":[\"http://" + this.configuration.get("proxy") + "\"]"
      }
      configCommand += "}' > /etc/docker/daemon.json"

      print("SETTING UP DOCKER DAEMON CONFIG:")
      print(configCommand)
      this.pipeline.sh "mkdir /etc/docker || true"
      this.pipeline.sh configCommand

    }

    this.pipeline.sh "killall -9 dockerd || true"
    this.pipeline.sh "dockerd & sleep ${timeoutInSeconds} || true "
    print("DOCKER DAEMON RESTART COMPLETE")
  }

  /**
   * Logs in into Docker registry.
   * @param registryCredentialsId credentials id configured in Jenkins.
   */
  void registryLogin(String registryCredentialsId) {
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
   * @param tag image tag
   * @param volumeSource origin host volume
   * @param volumeDestination destination guest volume
   */
  void runContainer(String tag, String volumeSource, String volumeDestination) {
    this.runContainerWithCommand(tag, volumeSource, volumeDestination, null, "")
  }

  /**
   * Run Docker container with given arguments
   * @param tag image tag
   * @param volumeSource origin host volume
   * @param volumeDestination destination guest volume
   * @param envs environment variables
   * @param command command to be executed
   */
  void runContainerWithCommand(String tag, String volumeSource, String volumeDestination, Map<String, String> envs, String command) {
    print("RUNNING DOCKER CONTAINER")
    print("\tTag: ${tag}")
    print("\tVolume Source: ${volumeSource}")
    print("\tVolume Destination: ${volumeDestination}")

    String envArgs = ""
    if (envs != null) {
      for (String key : envs.keySet()) {
        envArgs += "-e " + key + "=" + envs.get(key) + " "
      }
    }

    this.pipeline.sh "docker pull ${tag} || true"
    this.pipeline.sh "mkdir -p /mnt/docker || true"
    this.pipeline.sh "docker run -i " + envArgs + " --rm -v /mnt/docker:/var/lib/docker -v ${volumeSource}:${volumeDestination} ${tag} ${command}"
    this.pipeline.sh "rm -fr /mnt/docker || true"
  }

  /**
   * Clear image cache
   */
  void clearImageCache() {
    print("CLEANING IMAGE CACHE")
    this.pipeline.sh "docker stop \$(docker ps -aq) && docker rm \$(docker ps -aq) || true"
    this.pipeline.sh "docker rmi --force \$(docker images -q) || true"
  }

}
