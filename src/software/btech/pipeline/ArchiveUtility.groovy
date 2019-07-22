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
  ArchiveUtility(Script pipeline) {
    super(pipeline)
  }

  /**
   * Unarchive file, useful for recovering dependencies such as m2 repository or node_modules
   * @param filename name of previous file.
   */
  void unarchiveFile(String filename) {
    try {
      this.pipeline.copyArtifacts filter: "${filename}", projectName: "${this.pipeline.env.JOB_NAME}", selector: this.pipeline.lastCompleted(), target: "."
      this.pipeline.sh "tar -zxf ${filename} && rm ${filename}"
    } catch (ignored) {
      print("Archive not found: ${ignored}")
      print("default error handling behavior is to ignore and proceed...")
    }
  }

  /**
   * Archive file, useful for storing dependencies from previous builds such as maven m2 repository or node_modules
   * @param filename
   */
  void archiveFile(String filename) {
    try {
      this.pipeline.sh "tar -czf ${filename} archive"
      this.pipeline.archiveArtifacts filename
      this.pipeline.sh "rm ${filename} && rm -fr archive"
    } catch (e) {
      defaultErrorHandler("Failure archiving artifacts.", e)
    }
  }

}
