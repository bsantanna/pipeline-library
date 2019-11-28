#!groovy
package software.btech.pipeline.docker

import java.text.DateFormat
import java.text.SimpleDateFormat

/**
 * Docker in Docker utility class with reusable pipeline functions based on image bsantanna/jenkins-docker-agent
 */
class DockerInDockerUtility extends AbstractDockerUtility {

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
  Void dockerDaemonRestart(def timeoutInSeconds) {
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
  Void clearImageCache() {
    print("CLEANING IMAGE CACHE")
    this.pipeline.sh "docker stop \$(docker ps -aq) && docker rm \$(docker ps -aq) || true"
    this.pipeline.sh "docker rmi --force \$(docker images -q) || true"
  }

  /**
   * {@inheritDoc}
   */
  @Override
  Void runContainerWithCommand(String tag, String volumeSource, String volumeDestination, Map<String, String> envs, List<String> commands) {
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd")
    String date = dateFormat.format(Calendar.getInstance().getTime())
    String isolatedVolume = "/tmp/" + className + "_" + date

    print("RUNNING DOCKER CONTAINER WITH ISOLATED /var/lib/docker\n" +
        "\tTag: ${tag}\n" +
        "\tVolume /var/lib/docker: ${isolatedVolume}\n" +
        "\tVolume Source: ${volumeSource}\n" +
        "\tVolume Destination: ${volumeDestination}")

    String envArgs = getFormattedEnvArgs(envs)
    String commandArgs = getFormattedCommandArgs(commands)
    this.pipeline.sh "docker pull ${tag} || true"
    this.pipeline.sh "mkdir -p " + isolatedVolume + " || true"
    this.pipeline.sh "docker run -i " + envArgs + " --rm -v " + isolatedVolume + ":/var/lib/docker -v ${volumeSource}:${volumeDestination} ${tag} ${commandArgs}"
    this.pipeline.sh "rm -fr " + isolatedVolume + " || true"

    return null
  }

}
