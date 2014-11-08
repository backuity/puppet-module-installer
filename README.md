puppet-module-installer [![Build Status](https://travis-ci.org/backuity/puppet-module-installer.png?branch=master)](https://travis-ci.org/backuity/puppet-module-installer)
=======================

Installs puppet modules recursively from a `Puppetfile`.

# Installation

Until proper packages (Deb, Rpm) exist you can install the puppet-module-installer by downloading it from Maven Central:
<http://repo1.maven.org/maven2/org/backuity/puppet-module-installer_2.11/2.1.2/puppet-module-installer_2.11-2.1.2-one-jar.jar>

If you have `binfmt-support` installed you can simply give the executable permission to your jar:

    wget http://repo1.maven.org/maven2/org/backuity/puppet-module-installer_2.11/2.1.2/puppet-module-installer_2.11-2.1.2-one-jar.jar -O ~/bin/puppet-module-installer
    chmod u+x ~/bin/puppet-module-installer
    
If you don't have `binfmt-support`, you can create a bash script:    

    #!/bin/bash
    
    java -jar ~/bin/puppet-module-installer.jar "$@"
     
And download the jar next to it:

    wget http://repo1.maven.org/maven2/org/backuity/puppet-module-installer_2.11/2.1.2/puppet-module-installer_2.11-2.1.2-one-jar.jar -O ~/bin/puppet-module-installer.jar         
      
      
# Usage

The tool provides the following options:

  * `--help` : print a detailed help message
  * `--version` : print the version


Go to a folder containing a `Puppetfile` and run `puppet-module-installer`, it will download recursively all the module described by the `Puppetfile`.
You can inspect the graph of modules with the `puppet-module-installer graph` command, ex:

    Puppetfile
      ├ my-app(HEAD)
      │ ├ java(1.16.0)
      │ ├ jmxtrans(1.74.0)
      │ │ └ logrotate(1.0.0)
      │ │
      │ └ tomcat(2.23.0)
      │   ├ dynatrace(1.4.0)
      │   ├ introscope(3.4.0)
      │   ├ java(1.16.0)
      │   ├ jmxtrans(1.28.0)
      │   ├ nexus(1.10.0)
      │   ├ puppi(2.0.8)
      │   ├ ssl(1.76.0)
      │   └ stdlib(1.2.0)
      │
      ├ motd(HEAD)
      ├ nestools(HEAD)
      └ sudo(HEAD)


## Fetch mode

There are 5 different ways to retrieve the modules:

  * Normal : (*no option*) Stick to the versions found in the Puppetfile, when no version is found, HEAD is used. 
  * Head : (`--head`) Use HEAD for all modules.
  * Latest : (`--latest`) Ask git for a list of tags choose the highest minor for the current major
  * Latest Forced : (`--latest-force`) Ask git for a list of tags and choose the highest
  * Latest Head : (`--latest-head`) Same as *Latest* unless HEAD is wanted

**Example**

Given 2 puppet modules :
  * *java* whose repository git is `ssh://repos/java.git` with the following tags : `v1.3, v2.5, v2.6, v3.2`
  * *nexus* whose repository git is `ssh://repos/nexus.git` with the following tags : `v1.10, v2.5`
  
Give the following puppet file :

    mod 'java',
      :git => 'ssh://repos/java.git',
      :ref => 'v2.5'
      
    mod 'nexus',
      :git => 'ssh://repos/nexus.git'

Here are results of the different modes:

 Mode           | Java  | Nexus 
 -------------- | ----- | ----- 
 Normal         | v2.5  | HEAD
 Head           | HEAD  | HEAD
 Latest         | v2.6  | v2.5
 Latest forced  | v3.2  | v2.5
 Latest Head    | v2.6  | HEAD


# Puppetfile syntax

The syntax comes from [Librarian Puppet](https://github.com/rodjek/librarian-puppet).

    