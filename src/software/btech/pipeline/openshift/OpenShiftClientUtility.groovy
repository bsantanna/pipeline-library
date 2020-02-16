#!groovy
package software.btech.pipeline.openshift

import software.btech.pipeline.AbstractPipelineUtility

/**
 * OpenShift client setup utility
 */
class OpenShiftClientUtility extends AbstractPipelineUtility {

  final String clusterName;

  /**
   * Constructor with pipeline reference injection.
   * @param pipeline pipeline being executed
   * @param clusterName name of OpenShift cluster
   */
  OpenShiftClientUtility(Script pipeline, String clusterName) {
    super(pipeline)
    this.clusterName = clusterName
  }

  /**
   * Builds a project from OpenShift Cluster
   * @param projectName
   * @return
   */
  Void buildProject(String projectName, Integer timeout) {
    this.pipeline.openshift.withCluster(this.clusterName) {
      this.pipeline.openshift.withProject(projectName) {

        def builds = this.pipeline.openshift.selector('bc')
        builds.withEach {
          it.startBuild()
        }

        this.pipeline.timeout(timeout) {
          builds.untilEach(1) {
            return it.object().status.phase == "Complete"
          }
        }
        
      }
    }
  }

}
