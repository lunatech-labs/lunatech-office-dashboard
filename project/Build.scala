import play.Project._
import sbt._
import Keys._



object ApplicationBuild extends Build {

    val appName         = "joc-dashboard"
    val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(

  )

  val main = play.Project(appName, appVersion, appDependencies).settings(
    // Add your own project settings here
  )

}
