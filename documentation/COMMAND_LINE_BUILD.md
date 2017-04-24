Building the Heatseq Tool on the command line
-------------------------

These instructions are for building the Heatseq Tool on the Linux command line.

**Section 1: Install Java**

The Heatseq Tool requires Java version 7.  To check which version of Java you have installed type 'javac -version'.  If you don't have Java installed or if your version is less than 1.7 download and install the [latest Java JDK](http://www.oracle.com/technetwork/java/javase/downloads/jdk7-downloads-1880260.html).

**Section 2: Install maven**

Type 'mvn --version' and ensure that you have maven installed.  If not, download and install the [latest version of Maven](http://maven.apache.org/download.cgi).

Maven needs to be configured with a settings.xml file and a local repository.  If you have not already configured maven you will need to set these up.  By default Maven looks for a settings.xml file in your ~/.m2 directory.  If you don't already have a ~/.m2 directory, create one.  Within that directory create a settings.xml file that contains the following:

```
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
    <localRepository>/home/users/username/.m2/repository</localRepository>
    <interactiveMode>true</interactiveMode>
    <usePluginRegistry>false</usePluginRegistry>
    <offline>false</offline>
</settings>
```

The localRepository field should be configured to point to a local directory.  If the directory you have set as your localRepository does not already exist do a mkdir to create the directory.

**Section 3: Install git**

Type 'git --version' and ensure that you have git installed.  If not, download and install the [latest version of git](http://git-scm.com/downloads).


**Section 4: Clone the Heatseq Tool's GitHub repository**

Clone the Heatseq Tool's GitHub repository by doing the following:

git clone https://<i>username</i>@github.com/NimbleGen/bioinformatics.git

Substituting your GitHub username for <i>username</i>.

**Section 5: Build the HeatSeq Tool**

Build the HeatSeq Tool by doing the following:

cd bioinformatics/nimblegen_heatseq_build/hsqutils_commandline<br>
mvn install

After a successful build the HeatSeq application .jar file will be at:

/bioinformatics/nimblegen_heatseq_build/hsqutils_commandline/target/hsqutils_[version].jar 

You can now run the HeatSeq application by calling:

java -jar hsqutils_[version].jar





