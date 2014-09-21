name := "module-installer"

organization := "org.backuity"

scalaVersion := "2.11.2"

libraryDependencies ++= Seq(
  "org.parboiled" %% "parboiled" % "2.0.1",
  "commons-io"    % "commons-io" % "2.4",
  "com.google.guava" % "guava" % "18.0",
  "org.backuity" %% "matchete" % "1.10" % "test",
  "com.novocode" % "junit-interface" % "0.11" % "test"
)

com.github.retronym.SbtOneJar.oneJarSettings