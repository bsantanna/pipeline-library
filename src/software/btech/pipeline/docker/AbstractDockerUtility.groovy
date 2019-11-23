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
  void buildImage(String buildContext, String baseTag, String tag) {
    print("BUILDING DOCKER IMAGE WITH PARENT IMAGE")
    print("\tTag: ${tag}")
    print("\tBase Tag: ${baseTag}")
    print("\tBuild Context: ${buildContext}")
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
  void buildImage(String buildContext, String tag) {
    print("BUILDING DOCKER IMAGE")
    print("\tTag: ${tag}")
    print("\tBuild Context: ${buildContext}")
    this.pipeline.dir(buildContext) {
      this.pipeline.sh "docker pull ${tag}"
      this.pipeline.sh "docker build -t ${tag} ."
    }
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

}
