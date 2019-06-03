// Configurable settings
val scalaJSVersion =
  settingKey[String]("Version of Scala.js for which to build to CLI")
val scalaJSScalaVersions =
  settingKey[Seq[String]]("All the minor versions of Scala for which to build the CLI")

// Computed settings
val scalaJSBinaryVersion =
  settingKey[String]("Binary version of Scala.js")

// Custom tasks
val cliLibJars =
  taskKey[Seq[File]]("All the .jars that must go to the lib/ directory of the CLI")
val cliPack =
  taskKey[File]("Pack the CLI for the current configuration")

// Duplicated from the Scala.js sbt plugin
def binaryScalaJSVersion(full: String): String = {
  val ReleaseVersion = raw"""(\d+)\.(\d+)\.(\d+)""".r
  val MinorSnapshotVersion = raw"""(\d+)\.(\d+)\.([1-9]\d*)-SNAPSHOT""".r
  full match {
    case ReleaseVersion(major, _, _)       => major
    case MinorSnapshotVersion(major, _, _) => major
    case _                                 => full
  }
}

inThisBuild(Def.settings(
  version := "1.0.0-SNAPSHOT",
  organization := "org.scala-js",

  crossScalaVersions := Seq("2.12.8", "2.11.12"),
  scalaVersion := crossScalaVersions.value.head,
  scalacOptions ++= Seq("-deprecation", "-feature", "-Xfatal-warnings"),

  scalaJSVersion := "1.0.0-M8",
  scalaJSBinaryVersion := binaryScalaJSVersion(scalaJSVersion.value),

  scalaJSScalaVersions := Seq(
    "2.11.0",
    "2.11.1",
    "2.11.2",
    "2.11.4",
    "2.11.5",
    "2.11.6",
    "2.11.7",
    "2.11.8",
    "2.11.11",
    "2.11.12",
    "2.12.1",
    "2.12.2",
    "2.12.3",
    "2.12.4",
    "2.12.5",
    "2.12.6",
    "2.12.7",
    "2.12.8",
  ),

  homepage := Some(url("https://www.scala-js.org/")),
  licenses += ("BSD New",
      url("https://github.com/scala-js/scala-js-env-cli/blob/master/LICENSE")),
  scmInfo := Some(ScmInfo(
      url("https://github.com/scala-js/scala-js-cli"),
      "scm:git:git@github.com:scala-js/scala-js-cli.git",
      Some("scm:git:git@github.com:scala-js/scala-js-cli.git"))),
))

val commonSettings = Def.settings(
  publishMavenStyle := true,
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },
  pomExtra := (
    <developers>
      <developer>
        <id>sjrd</id>
        <name>Sébastien Doeraene</name>
        <url>https://github.com/sjrd/</url>
      </developer>
      <developer>
        <id>gzm0</id>
        <name>Tobias Schlatter</name>
        <url>https://github.com/gzm0/</url>
      </developer>
      <developer>
        <id>nicolasstucki</id>
        <name>Nicolas Stucki</name>
        <url>https://github.com/nicolasstucki/</url>
      </developer>
    </developers>
  ),
  pomIncludeRepository := { _ => false },
)

lazy val `scalajs-cli`: Project = project.in(file(".")).
  settings(
    commonSettings,

    libraryDependencies ++= Seq(
      "org.scala-js" %% "scalajs-linker" % scalaJSVersion.value,
      "com.github.scopt" %% "scopt" % "3.5.0",
    ),

    // assembly options
    mainClass in assembly := None, // don't want an executable JAR
    assemblyOption in assembly ~= { _.copy(includeScala = false) },
    assemblyJarName in assembly :=
      s"${normalizedName.value}-assembly_${scalaBinaryVersion.value}-${scalaJSVersion.value}.jar",

    cliLibJars := {
      val s = streams.value
      val log = s.log

      val sjsOrg = organization.value
      val scalaBinVer = scalaBinaryVersion.value
      val sjsVer = scalaJSVersion.value

      val scalaFullVers = scalaJSScalaVersions.value.filter { full =>
        CrossVersion.binaryScalaVersion(full) == scalaBinVer
      }

      val cliAssemblyJar = assembly.value

      val stdLibModuleID =
        sjsOrg % s"scalajs-library_$scalaBinVer" % sjsVer
      val compilerPluginModuleIDs =
        scalaFullVers.map(v => sjsOrg % s"scalajs-compiler_$v" % sjsVer)
      val allModuleIDs = (stdLibModuleID +: compilerPluginModuleIDs).toVector
      val allModuleIDsIntransitive = allModuleIDs.map(_.intransitive())

      val resolvedLibJars = {
        val retrieveDir = s.cacheDirectory / "cli-lib-jars"
        val lm = {
          import sbt.librarymanagement.ivy._
          val ivyConfig = InlineIvyConfiguration().withLog(log)
          IvyDependencyResolution(ivyConfig)
        }
        val dummyModuleName =
          s"clilibjars-$sjsVer-$scalaBinVer-" + scalaFullVers.mkString("-")
        val dummyModuleID = sjsOrg % dummyModuleName % version.value
        val descriptor =
          lm.moduleDescriptor(dummyModuleID, allModuleIDsIntransitive, scalaModuleInfo = None)
        val maybeFiles = lm.retrieve(descriptor, retrieveDir, log)
        maybeFiles.fold({ unresolvedWarn =>
          throw unresolvedWarn.resolveException
        }, { files =>
          files
        }).distinct
      }

      cliAssemblyJar +: resolvedLibJars
    },

    target in cliPack := baseDirectory.value / "pack",
    moduleName in cliPack :=
      s"scalajs_${scalaBinaryVersion.value}-${scalaJSVersion.value}",
    crossTarget in cliPack :=
      (target in cliPack).value / (moduleName in cliPack).value,

    cliPack := {
      val scalaBinVer = scalaBinaryVersion.value
      val sjsVer = scalaJSVersion.value

      val trg = (crossTarget in cliPack).value
      val trgLib = trg / "lib"
      val trgBin = trg / "bin"

      if (trg.exists)
        IO.delete(trg)

      IO.createDirectory(trgLib)
      val libJars = cliLibJars.value
      for (libJar <- libJars) {
        IO.copyFile(libJar, trgLib / libJar.getName)
      }

      IO.createDirectory(trgBin)
      val scriptDir = (resourceDirectory in Compile).value
      for {
        scriptFile <- IO.listFiles(scriptDir)
        if !scriptFile.getPath.endsWith("~")
      } {
        val content = IO.read(scriptFile)
        val processedContent = content
          .replaceAllLiterally("@SCALA_BIN_VER@", scalaBinVer)
          .replaceAllLiterally("@SCALAJS_VER@", sjsVer)
        val dest = trgBin / scriptFile.getName
        IO.write(dest, processedContent)
        if (scriptFile.canExecute)
          dest.setExecutable(/* executable = */ true, /* ownerOnly = */ false)
      }

      trg
    },
  )
