#!groovy
package software.btech.pipeline.docker

/**
 * Docker client setup utility
 */
class DockerClientUtility extends AbstractDockerUtility {

  /**
   * Constructor with pipeline reference injection.
   * @param pipeline pipeline being executed
   * @param configuration configuration map
   */
  DockerClientUtility(Script pipeline, Map<String, String> configuration) {
    super(pipeline, configuration)
  }

  /**
   * {@inheritDoc}
   */
  @Override
  Void runContainerWithCommand(String tag, String volumeSource, String volumeDestination, Map<String, String> envs, List<String> commands) {
    print("RUNNING DOCKER CONTAINER\n" +
        "\tTag: ${tag}\n" +
        "\tVolume Source: ${volumeSource}\n" +
        "\tVolume Destination: ${volumeDestination}")

    String envArgs = getFormattedEnvArgs(envs)
    String commandArgs = getFormattedCommandArgs(commands)
    this.pipeline.sh "docker pull ${tag} || true"
    this.pipeline.sh "docker run -i " + envArgs + " --rm -v ${volumeSource}:${volumeDestination} ${tag} ${commandArgs}"

    return null
  }
}
