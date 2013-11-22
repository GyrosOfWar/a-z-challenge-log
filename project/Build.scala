import sbt._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "AToZChallengeLog"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    // Add your project dependencies here,
    jdbc,
    anorm,
    "com.typesafe.play" %% "play-slick" % "0.5.0.8",
    "com.google.code.findbugs" % "jsr305" % "1.3.+"
  )


  val main = play.Project(appName, appVersion, appDependencies).settings(

  )

}
