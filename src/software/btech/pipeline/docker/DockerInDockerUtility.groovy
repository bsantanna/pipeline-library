#!groovy
package software.btech.pipeline.docker

import software.btech.pipeline.AbstractPipelineUtility


/**
 * Docker in Docker utility class with reusable pipeline functions based on image bsantanna/jenkins-docker-agent
 */
class DockerInDockerUtility extends AbstractPipelineUtility {

  /**
   * Constructor with pipeline reference injection.
   * @param pipeline pipeline being executed
   * @param configuration configuration map
   */
  DockerInDockerUtility(Script pipeline, Map<String, String> configuration) {
    super(pipeline, configuration)
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

    String configCommand = "echo '{\"experimental\":false, \"debug\":false, \"storage-driver\":\"vfs\""
    if (this.configuration.containsKey("proxy")) {
      configCommand += ", \"insecure-registries\":[\"http://" + this.configuration.get("proxy") + "\"]"
      configCommand += ", \"registry-mirrors\":[\"http://" + this.configuration.get("proxy") + "\"]"
    }
    configCommand += "}' > /etc/docker/daemon.json"

    print("SETTING UP DOCKER DAEMON CONFIG:")
    print(configCommand)
    this.pipeline.sh "mkdir /etc/docker || true"
    this.pipeline.sh configCommand + " || true"


    this.pipeline.sh "killall -9 dockerd || true"
    this.pipeline.sh "dockerd & sleep ${timeoutInSeconds} || true "
    print("DOCKER DAEMON RESTART COMPLETE")
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
