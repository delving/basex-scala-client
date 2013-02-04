import sbt._
import Keys._

object ProjectBuild extends Build {

  val buildVersion = "0.6"

  val delvingReleases = "Delving Releases Repository" at "http://development.delving.org:8081/nexus/content/repositories/releases"
  val delvingSnapshots = "Delving Snapshot Repository" at "http://development.delving.org:8081/nexus/content/repositories/snapshots"
  val delvingRepository = if (buildVersion.endsWith("SNAPSHOT")) delvingSnapshots else delvingReleases

  lazy val root = Project(
    id = "basex-scala-client",
    base = file(".")
  ).settings(

    organization := "eu.delving",
    version := buildVersion,

    crossScalaVersions := Seq("2.9.1", "2.9.2", "2.10.0"),

    resolvers += "BaseX Repository" at "http://files.basex.org/maven",
    resolvers += "OSS Sonatype" at "https://oss.sonatype.org/content/repositories/releases",
 
    libraryDependencies += "org.basex"  %     "basex"  % "7.3",
    libraryDependencies += "org.specs2" %%    "specs2" % "1.11" %  "test",

    publishTo := Some(delvingRepository),

    credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"),

    publishMavenStyle := true
  )

}
