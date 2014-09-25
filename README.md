puppet-module-installer [![Build Status](https://travis-ci.org/backuity/puppet-module-installer.png?branch=master)](https://travis-ci.org/backuity/puppet-module-installer)
=======================

Installs puppet modules recursively.

# Usage

The tool provides the following options:

  * `-v` : verbose mode
  * `--version` : prints the version


# Installation

Until proper packages (Deb, Rpm) exist you can install the puppet-module-installer by downloading it from Maven Central:

    # go to your home bin folder (if you have one)
    cd ~/bin
    wget http://repo1.maven.org/maven2/org/backuity/puppet-module-installer_2.11/1.0/puppet-module-installer_2.11-1.0-one-jar.jar -O puppet-module-installer.jar
      
Create a bash script with the following

    #!/bin/bash

    java -jar ~/bin/puppet-module-installer.jar "$@" 