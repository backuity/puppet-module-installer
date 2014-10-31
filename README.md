puppet-module-installer [![Build Status](https://travis-ci.org/backuity/puppet-module-installer.png?branch=master)](https://travis-ci.org/backuity/puppet-module-installer)
=======================

Installs puppet modules recursively.

# Usage

The tool provides the following options:

  * `-v` : verbose mode
  * `--version` : prints the version


# Installation

Until proper packages (Deb, Rpm) exist you can install the puppet-module-installer by downloading it from Maven Central:
<http://repo1.maven.org/maven2/org/backuity/puppet-module-installer_2.11/2.1.0/puppet-module-installer_2.11-2.1.0-one-jar.jar>

If you have `binfmt-support` installed you can simply give the executable permission to your jar:

    wget http://repo1.maven.org/maven2/org/backuity/puppet-module-installer_2.11/2.1.0/puppet-module-installer_2.11-2.1.0-one-jar.jar -O ~/bin/puppet-module-installer
    chmod u+x ~/bin/puppet-module-installer
    
If you don't have `binfmt-support`, you can create a bash script:    

    #!/bin/bash
    
    java -jar ~/bin/puppet-module-installer.jar "$@"
     
And download the jar next to it:

    wget http://repo1.maven.org/maven2/org/backuity/puppet-module-installer_2.11/2.1.0/puppet-module-installer_2.11-2.1.0-one-jar.jar -O ~/bin/puppet-module-installer.jar         
      
    