/**
 * Launcher for command line java programs on windows.
 */

package kessler

object Launcher {
  def main(args: Array[String]) {
    val home = System.getProperty("java.home")
    val cmd = Array("cmd", "/C", "start", home + "\\bin\\java.exe") ++ args

    println("Found java at: " + home)
    println("Executing: " + cmd.mkString(" "))
    Runtime.getRuntime.exec(cmd).waitFor
    println("Done execution.")
  }
}
