#!groovy
package software.btech.pipeline

/**
 * Base class with shared functions and class scoped fields
 * - pipeline being executed
 * - className resolved to subclasses
 */
abstract class AbstractPipelineUtility implements Serializable {

  final Script pipeline
  final String className

  /**
   * Constructor with pipeline reference injection.
   * @param pipeline pipeline being executed
   */
  AbstractPipelineUtility(Script pipeline) {
    this.pipeline = pipeline
    this.className = this.getClass().getSimpleName()
  }

  /**
   * Print with className
   * @param inputMessage message to print
   */
  def print(String inputMessage) {
    this.pipeline.echo this.className + ": " + inputMessage
  }

  /**
   * Default error handling function with local directory
   * @param message message for debugging purposes
   * @param e exception to be thrown
   */
  def defaultErrorHandler(message, e) {
    print("==== ERROR ====")
    print(message)
    print("==== CURRENT DIR ====")
    this.pipeline.sh "pwd"
    print("==== DIR CONTENT ====")
    this.pipeline.sh "ls -lh ."
    print("==== STACK TRACE CONTENT ====")
    throw e
  }

}