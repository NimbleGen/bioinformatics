Building the Heatseq Tool on the command line
-------------------------

These instructions are for building the Heatseq Tool on a Linux command line.

**Section 1: Install Java**

The Heatseq Tool requires Java version 7.  To check which version of Java you have installed type 'javac -version'.  If you don't have Java installed or if your version is less than 1.7 download and install the [latest Java JDK](http://www.oracle.com/technetwork/java/javase/downloads/jdk7-downloads-1880260.html).

**Section 2: Install maven**

Type 'mvn --version' and ensure that you have maven installed.  If not download and install the [latest version](http://maven.apache.org/download.cgi).

Maven needs to be configured with a settings.xml file and a local repository.  If you have not already configured maven you will need to set these up.  By default Maven looks for a settings.xml file in the ~/.m2 directory.  The settings.xml file contains a localRepository field that must be configured to point to a directory.  For example:

```
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
<localRepository>/home/users/bainc/.m2/repository</localRepository>
<interactiveMode>true</interactiveMode>
<usePluginRegistry>false</usePluginRegistry>
<offline>false</offline>
</settings>
```

If the directory you have set as your localRepository does not exist you'll need to do a mkdir to create the directory.

**Section 3: Clone the Heatseq Tool's GitHub repository**

**Section 4: Build the HeatSeq Tool**
