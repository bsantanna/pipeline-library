#!groovy
package software.btech.pipeline

/**
 * File manipulation for archiving purposes functions.
 */
class ArchiveUtility extends AbstractPipelineUtility {

  /**
   * Constructor with pipeline reference injection.
   * @param pipeline pipeline being executed
   */
  ArchiveUtility(def pipeline) {
    super(pipeline)
  }

  /**
   * Unarchive file, useful for recovering dependencies such as m2 repository or node_modules
   * @param filename name of previous file.
   */
  def unarchiveFile(filename) {
    try {
      this.pipeline.copyArtifacts filter: "${filename}", projectName: "${env.JOB_NAME}", selector: lastCompleted(), target: "."
      this.pipeline.sh "tar -zxf ${filename} && rm ${filename}"
    } catch (ignored) {
      print("Archive not found: ${ignored}")
      print("default error handling behavior is to ignore and proceed...")
    }
  }

  /**
   * Archive file, useful for storing dependencies from previous builds such as maven m2 repository or node_modules
   * @param filename
   * @return
   */
  def archiveFile(filename) {
    try {
      this.pipeline.sh "tar -czf ${filename} archive"
      this.pipeline.archiveArtifacts filename
      this.pipeline.sh "rm ${filename} && rm -fr archive"
    } catch (e) {
      defaultErrorHandler("Failure archiving artifacts.", e)
    }
  }

}
