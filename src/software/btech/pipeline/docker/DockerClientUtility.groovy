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

}
