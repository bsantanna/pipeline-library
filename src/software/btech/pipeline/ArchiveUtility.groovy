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
      this.pipeline.copyArtifacts filter: "${filename}", projectName: "${this.pipeline.env.JOB_NAME}", selector: this.pipeline.lastStable(), target: "."
      this.pipeline.sh "tar -zxf ${filename} && rm ${filename}"
    } catch (ignored) {
      print("Archive not found: ${ignored}")
      print("default error handling behavior is to ignore and proceed...")
    }
  }

  /**
   * Archive file, useful for storing dependencies from previous builds such as maven m2 repository or node_modules
   * @param filename
   * @param directory
   */
  void archiveFile(String filename, String directory) {
    try {
      this.compressDirectory(filename, directory)
      this.pipeline.archiveArtifacts filename
      this.pipeline.sh "rm ${filename}"
    } catch (e) {
      defaultErrorHandler("Failure archiving artifacts.", e)
    }
  }

  /**
   * Compress directory used for generating tarballs
   * @param filename
   * @param directory
   */
  void compressDirectory(String filename, String directory) {
    try {
      this.pipeline.sh "tar -czf ${filename} ${directory}"
    } catch (e) {
      defaultErrorHandler("Failure compressing artifacts.", e)
    }
  }

}
