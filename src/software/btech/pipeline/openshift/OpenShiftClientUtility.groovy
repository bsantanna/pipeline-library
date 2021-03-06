#!groovy
package software.btech.pipeline.openshift

import software.btech.pipeline.AbstractPipelineUtility

import java.text.SimpleDateFormat

/**
 * OpenShift client setup utility
 */
class OpenShiftClientUtility extends AbstractPipelineUtility {

  final String clusterName

  final SimpleDateFormat dateFormat

  /**
   * Constructor with pipeline reference injection.
   * @param pipeline pipeline being executed
   * @param clusterName name of OpenShift cluster
   */
  OpenShiftClientUtility(Script pipeline, String clusterName) {
    super(pipeline)
    this.clusterName = clusterName
    this.dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
  }

  /**
   * Returns a list of complete build objects
   * @param startTimestamp
   * @return
   */
  List getCompleteBuilds(long startTimestamp) {
    def buildList = this.pipeline.openshift.selector("build").objects()
    def completeBuilds = []
    for (Object build : buildList) {
      if (build.status != null && build.status.startTimestamp != null) {
        long buildStartTimestamp = this.dateFormat.parse(build.status.startTimestamp).getTime()
        if (buildStartTimestamp > startTimestamp) {
          if ("Complete".equalsIgnoreCase(build.status.phase)) {
            completeBuilds.add(build)
          }
        }
      }
    }
    return completeBuilds
  }

  /**
   * Builds a project from OpenShift Cluster
   * @param projectName
   * @param timeout for exiting
   * @return
   */
  Void buildProject(String projectName, int timeout) {
    this.pipeline.timeout(timeout) {
      this.pipeline.openshift.withCluster(this.clusterName) {
        this.pipeline.openshift.withProject(projectName) {

          long startTimestamp = Calendar.getInstance().getTimeInMillis()
          int startedBuildCount = 0
          this.pipeline.openshift.selector('bc').withEach {
            def buildConfig = it.object()
            if (!"Binary".equals(buildConfig.spec.source.type)) {
              it.startBuild()
              startedBuildCount++
            }
          }

          this.print(String.format("Started %s build jobs", startedBuildCount))

          boolean isComplete = false
          while (!isComplete) {
            // loop condition
            List completeBuilds = getCompleteBuilds(startTimestamp)
            isComplete = completeBuilds.size() == startedBuildCount
            if (!isComplete) {
              int sleepTime = 1
              String unit = "MINUTES"
              this.print(String.format("From the %s started build jobs, %s are complete.",
                  startedBuildCount, completeBuilds.size(), sleepTime, unit.toLowerCase()))
              this.pipeline.sleep(time: sleepTime, unit: unit)
            } else {
              this.print(String.format("Build Complete, all %s finished with Complete Status", startedBuildCount))
            }
          }
        }
      }
    }
  }

  /**
   * Builds a binary Docker image from remote URL resource, handy for compiled code
   * @param projectName project namespace
   * @param buildConfigName build config name
   * @param archiveURL archive remote url
   */
  Void buildBinaryImage(String projectName, String buildConfigName, String archiveURL) {
    this.pipeline.openshift.withCluster(this.clusterName) {
      this.pipeline.openshift.withProject(projectName) {
        def result = this.pipeline.openshift.raw("start-build", buildConfigName, String.format("--from-archive=%s", archiveURL))
        this.print(String.format("Build Creation result: %s", result.out))
      }
    }
  }

}
