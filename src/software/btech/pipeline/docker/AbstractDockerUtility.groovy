#!groovy
package software.btech.pipeline.docker

import software.btech.pipeline.AbstractPipelineUtility

/**
 * Abstract Docker Utility used to declare
 */
abstract class AbstractDockerUtility extends AbstractPipelineUtility {

  final Map<String, String> configuration

  /**
   * Constructor with pipeline reference injection and configuration map
   *
   * @param pipeline
   * @param configuration
   */
  AbstractDockerUtility(Script pipeline, Map<String, String> configuration) {
    super(pipeline)
    this.configuration = configuration
  }

  /**
   * Build image
   * @param buildContext
   * @param baseTag
   * @param tag
   */
  Void buildImage(String buildContext, String baseTag, String tag) {

    print("BUILDING DOCKER IMAGE WITH PARENT IMAGE" +
        "\n\tTag: ${tag}" +
        "\n\tBase Tag: ${baseTag}" +
        "\n\tBuild Context: ${buildContext}")

    this.pipeline.dir(buildContext) {
      this.pipeline.sh "docker pull ${baseTag}"
      this.pipeline.sh "docker build -t ${tag} ."
    }

  }

  /**
   * Build image
   * @param buildContext
   * @param tag
   */
  Void buildImage(String buildContext, String tag) {

    print("BUILDING DOCKER IMAGE" +
        "\n\tTag: ${tag}" +
        "\n\tBuild Context: ${buildContext}")

    this.pipeline.dir(buildContext) {
      this.pipeline.sh "docker build -t ${tag} ."
    }

  }

  /**
   * Logs in into Docker registry.
   * @param registryCredentialsId credentials id configured in Jenkins.
   */
  Void registryLogin(String registryCredentialsId) {

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
   * Get formatted env args
   * @param envs map of environment variables
   * @return string formatted as a valid command line argument
   */
  String getFormattedEnvArgs(Map<String, String> envs) {
    String envArgs = ""
    if (envs != null) {
      for (String key : envs.keySet()) {
        envArgs += "-e " + key + "=" + envs.get(key) + " "
      }
    }
    return envArgs
  }

  /**
   * Get formatted commands
   * @param commands command line list
   * @return string formatted as a valid command line argument
   */
  String getFormattedCommandArgs(List<String> commands) {
    String commandArgs = ""
    if (commands != null) {
      commandArgs += String.join("&&", commands)
    }
    return commandArgs
  }

  /**
   * Run Docker container with given arguments.
   *
   * @param tag image tag
   * @param volumeSource origin host volume
   * @param volumeDestination destination guest volume
   */
  Void runContainer(String tag, String volumeSource, String volumeDestination) {
    this.runContainerWithCommand(tag, volumeSource, volumeDestination, null, null)
  }


  /**
   * Run Docker container with given arguments
   * @param tag image tag
   * @param volumeSource origin host volume
   * @param volumeDestination destination guest volume
   * @param envs environment variable map
   * @param commands command map
   * @return true if success
   */
  abstract Void runContainerWithCommand(String tag, String volumeSource, String volumeDestination, Map<String, String> envs, List<String> commands)

}
