Global / onChangedBuildSource := IgnoreSourceChanges // not working well with webpack devserver

inThisBuild(
  Seq(
    version           := "0.1.0-SNAPSHOT",
    scalaVersion      := "2.13.10",
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision,
    pushRemoteCacheTo := Some(MavenCache("local-cache", file("/tmp/sbt-remote-cache"))),
  )
)

val versions = new {
  val outwatch      = "1.0.0-RC15"
  val colibri       = "0.7.8"
  val funStack      = "0.9.14"
  val sloth         = "0.7.1"
  val pprint        = "0.7.2"
  val scalajsAwsSdk = "0.33.0-v2.892.0"
  val tapir         = "1.7.2"
  val clearconfig   = "3.1.0"
  val caseApp       = "2.1.0-M25"
  val skunk         = "0.5.1"
  val acyclic       = "0.3.8"
  val enumeratum    = "1.7.3"
}

ThisBuild / resolvers ++= Seq(
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
  "Sonatype OSS Snapshots S01" at "https://s01.oss.sonatype.org/content/repositories/snapshots", // https://central.sonatype.org/news/20210223_new-users-on-s01/
  "Jitpack" at "https://jitpack.io",
)

// do not warn about unused setting key. TODO: why is this needed? scala-js-bundler bug? sbt says this setting is unused, but it is used.
Global / excludeLintKeys += webpackDevServerPort

val enableFatalWarnings =
  sys.env.get("ENABLE_FATAL_WARNINGS").flatMap(value => scala.util.Try(value.toBoolean).toOption).getOrElse(false)

lazy val commonSettings = Seq(
  addCompilerPlugin("org.typelevel" % "kind-projector" % "0.13.2" cross CrossVersion.full),
  // overwrite scalacOptions "-Xfatal-warnings" from https://github.com/DavidGregory084/sbt-tpolecat
  if (enableFatalWarnings) scalacOptions += "-Xfatal-warnings" else scalacOptions -= "-Xfatal-warnings",
  scalacOptions ++= Seq(
    "-Xasync",
    "-Ymacro-annotations",
    "-Ywarn-macros:both",
    "-Vimplicits",
    "-Vtype-diffs",
    // TODO: "-Xlint:nonlocal-return",
  ),
  scalacOptions --= Seq("-Xcheckinit"), // produces check-and-throw code on every val access
  scalacOptions --= Seq(
    "-Wnonunit-statement"
  ), // does not work with scala-async yet: https://github.com/scala/bug/issues/12723
  libraryDependencies ++= Seq(
    // Use the %%% operator instead of %% for Scala.js and Scala Native
    "org.typelevel"                         %%% "cats-core"             % "2.10.0",
    "org.typelevel"                         %%% "alleycats-core"        % "2.10.0",
    "org.typelevel"                         %%% "cats-effect"           % "3.5.1",
    "org.scala-lang.modules"                %%% "scala-async"           % "1.0.1",
    "io.circe"                              %%% "circe-core"            % "0.14.5",
    "io.circe"                              %%% "circe-parser"          % "0.14.5",
    "io.circe"                              %%% "circe-generic"         % "0.14.5",
    "io.circe"                              %%% "circe-generic-extras"  % "0.14.3",
    "com.github.japgolly.clearconfig"       %%% "core"                  % versions.clearconfig,
    "org.scalacheck"                        %%% "scalacheck"            % "1.17.0"                                    % Test,
    "com.lihaoyi"                           %%% "utest"                 % "0.8.1"                                     % Test,
    "com.lihaoyi"                           %%% "pprint"                % versions.pprint                             % Test,
    "io.estatico"                           %%% "newtype"               % "0.4.4",
    "com.beachape"                          %%% "enumeratum"            % versions.enumeratum,
    "com.beachape"                          %%% "enumeratum-circe"      % versions.enumeratum,
    "com.beachape"                          %%% "enumeratum-cats"       % versions.enumeratum,
    "com.github.plokhotnyuk.jsoniter-scala" %%% "jsoniter-scala-core"   % "2.23.2",
    "com.github.plokhotnyuk.jsoniter-scala" %%% "jsoniter-scala-circe"  % "2.23.2",
    "com.github.plokhotnyuk.jsoniter-scala" %%% "jsoniter-scala-macros" % "2.23.2"                                    % "compile-internal",
    "io.scalaland"                          %%% "chimney"               % "0.7.5",
    ("com.lihaoyi"                           %% "acyclic"               % versions.acyclic cross (CrossVersion.full)) % "provided",
  ),
  testFrameworks      += new TestFramework("utest.runner.Framework"),
  autoCompilerPlugins := true,
  addCompilerPlugin("com.lihaoyi" %% "acyclic" % versions.acyclic cross (CrossVersion.full)),
  scalacOptions += "-P:acyclic:force", // enable acyclic for all files
)

lazy val scalaJsSettings = Seq(
  scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) },
  libraryDependencies += "org.portable-scala" %%% "portable-scala-reflect" % "1.1.2",
) ++ scalaJsBundlerSettings ++ scalaJsMacrotaskExecutor ++ scalaJsSecureRandom

// code change
lazy val scalaJsBundlerSettings = Seq(
  webpack / version               := "5.75.0",
  webpackCliVersion               := "5.0.0",
  startWebpackDevServer / version := "4.11.1",
  useYarn                         := true,
  yarnExtraArgs                  ++= Seq("--prefer-offline", "--pure-lockfile"),
)

lazy val scalaJsMacrotaskExecutor = Seq(
  // https://github.com/scala-js/scala-js-macrotask-executor
  libraryDependencies += "org.scala-js" %%% "scala-js-macrotask-executor" % "1.1.1"
)

lazy val scalaJsSecureRandom = Seq(
  // https://www.scala-js.org/news/2022/04/04/announcing-scalajs-1.10.0
  libraryDependencies += "org.scala-js" %%% "scalajs-java-securerandom" % "1.0.0"
)

def readJsDependencies(baseDirectory: File, field: String): Seq[(String, String)] = {
  val packageJson = ujson.read(IO.read(new File(s"$baseDirectory/package.json")))
  packageJson(field).obj.mapValues(_.str.toString).toSeq
}

// shared project which contains api definitions.
// these definitions are used for type safe implementations
// of client and server
lazy val foo = project
  .enablePlugins(ScalaJSPlugin, ScalaJSBundlerPlugin, ScalablyTypedConverterPlugin)
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir"   %%% "tapir-core"       % versions.tapir,
      "com.softwaremill.sttp.tapir"   %%% "tapir-json-circe" % versions.tapir,
      "com.softwaremill.sttp.tapir"   %%% "tapir-enumeratum" % versions.tapir,
      "com.softwaremill.sttp.apispec" %%% "apispec-model"    % "0.3.2",
      "com.github.cornerman"          %%% "sloth"            % versions.sloth,
    ),
    scalaJSUseMainModuleInitializer := true,
  )


addCommandAlias("prod", "lambda/fullOptJS/webpack; webapp/fullOptJS/webpack; cli/fullOptJS/webpack")
addCommandAlias("dev", "devInit; devWatchAll; devDestroy")
addCommandAlias("devf", "devInit; devWatchFrontend; devDestroy") // compile only frontend
// devInit needs to execute lambda/fastOptJS/webpack, to prepare all lambda npm dependencies.
// We want to avoid this expensive preparation in the hot-reload process,
// and therefore only watch lambda/fastOptJS, where local-env can resolve the dependencies
// from the previously prepared node_modules folder.
addCommandAlias("devInit", "; lambda/fastOptJS/webpack; webapp/fastOptJS/startWebpackDevServer")
addCommandAlias("devWatchFrontend", "~; webapp/fastOptJS")
addCommandAlias("devWatchAll", "~; lambda/fastOptJS; webapp/fastOptJS; compile; Test/compile")
addCommandAlias("devDestroy", "webapp/fastOptJS/stopWebpackDevServer")
