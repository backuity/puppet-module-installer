
name := "puppet-module-installer"

organization := "org.backuity"

scalaVersion := "2.11.2"

homepage := Some(url("https://github.com/backuity/puppet-module-installer"))

licenses := Seq("Apache 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.html"))

scalacOptions ++= Seq("-deprecation", "-unchecked")

libraryDependencies ++= Seq(
  "org.parboiled" %% "parboiled" % "2.0.1",
  "commons-io"    % "commons-io" % "2.4",
  "com.google.guava" % "guava" % "18.0",
  "org.backuity" %% "matchete" % "1.10" % "test",
  "com.novocode" % "junit-interface" % "0.11" % "test"
)

com.github.retronym.SbtOneJar.oneJarSettings

artifact in (Compile, oneJar) ~= { art =>
  art.copy(`classifier` = Some("one-jar"))
}

addArtifact(artifact in (Compile, oneJar), oneJar)

publishMavenStyle := true

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (version.value.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

// replace publish by publishSigned
publish := com.typesafe.sbt.pgp.PgpKeys.publishSigned.value

pomIncludeRepository := { _ => false }

pomExtra :=
  <scm>
    <url>git@github.com:backuity/puppet-module-installer.git</url>
    <connection>scm:git:git@github.com:backuity/puppet-module-installer.git</connection>
  </scm>
    <developers>
      <developer>
        <id>backuitist</id>
        <name>Bruno Bieth</name>
        <url>https://github.com/backuitist</url>
      </developer>
    </developers>

releaseSettings