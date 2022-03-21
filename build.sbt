// Configurable settings
val scalaJSVersion =
  settingKey[String]("Version of Scala.js for which to build to CLI")
val scalaJSScalaVersions =
  settingKey[Seq[String]]("All the minor versions of Scala for which to build the CLI")

// Custom tasks
val cliLibJars =
  taskKey[Seq[File]]("All the .jars that must go to the lib/ directory of the CLI")
val cliPack =
  taskKey[File]("Pack the CLI for the current configuration")

inThisBuild(Def.settings(
  version := "1.1.1-SNAPSHOT",
  organization := "org.scala-js",

  crossScalaVersions := Seq("2.11.12", "2.12.15", "2.13.6"),
  scalaVersion := crossScalaVersions.value.last,
  scalacOptions ++= Seq("-deprecation", "-feature", "-Xfatal-warnings"),

  scalaJSVersion := "1.7.1",

  scalaJSScalaVersions := Seq(
    "2.11.12",
    "2.12.1",
    "2.12.2",
    "2.12.3",
    "2.12.4",
    "2.12.5",
    "2.12.6",
    "2.12.7",
    "2.12.8",
    "2.12.9",
    "2.12.10",
    "2.12.11",
    "2.12.12",
    "2.12.13",
    "2.12.14",
    "2.12.15",
    "2.13.0",
    "2.13.1",
    "2.13.2",
    "2.13.3",
    "2.13.4",
    "2.13.5",
    "2.13.6",
  ),

  homepage := Some(url("https://www.scala-js.org/")),
  licenses += ("BSD New",
      url("https://github.com/scala-js/scala-js-cli/blob/main/LICENSE")),
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
      "com.github.scopt" %% "scopt" % "3.7.1",
    ),

    // assembly options
    assembly / mainClass := None, // don't want an executable JAR
    assembly / assemblyOption ~= { _.copy(includeScala = false) },
    assembly / assemblyJarName :=
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

    cliPack / target := baseDirectory.value / "pack",
    cliPack / moduleName :=
      s"scalajs_${scalaBinaryVersion.value}-${scalaJSVersion.value}",
    cliPack / crossTarget :=
      (cliPack / target).value / (cliPack / moduleName).value,

    cliPack := {
      val scalaBinVer = scalaBinaryVersion.value
      val sjsVer = scalaJSVersion.value

      val trg = (cliPack / crossTarget).value
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
      val scriptDir = (Compile / resourceDirectory).value
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
