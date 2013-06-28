bioinformatics
==============

Bioinformatics tools developed at Roche NimbleGen.

Building the Heatseq Tool in Eclipse
-------------------------

**Section 1: Eclipse Setup**

1.  Download the current Eclipse IDE for Java Developers (Juno) from http://www.eclipse.org/downloads/.  
2.  Install Eclipse by unzipping the downloaded file into the desired install location (Recommended: C://Eclipse/Juno/).  Note:  If you install in Program Files you may have to run as Admin to get plugins to install correctly.
2A (optional) – Deleting the C://Users/[user name]/.eclipse directory may alleviate issues with installing plug-ins.
3.  Run eclipse by double clicking the eclipse.exe file (sample location: C://Eclipse/Juno/eclipse/eclipse.exe).  You may want to right click the eclipse.exe file and “Run as Administrator” if you run into issues installing plug-ins.
4.  Create a workspace by browsing to the desired location for the heatseq code when the “Select a workspace” dialog appears (Recommended:  C://Eclipse_Workspaces/heatseq_workspace).
5.  Install EGit plug-in by clicking “Help”>>”Install New Software…” from the main menu within Eclipse.   Add http://download.eclipse.org/releases/juno to the "Work with" text box and "git" in the type filter textbox and hit return:  <br>  ![alt text](https://github.com/NimbleGen/bioinformatics/raw/master/documentation/images/egit.png "Install EGit Dialog")<br> Then select and install:
  1. Eclipse EGit
  2. Eclipse JGit
6.  Install m2e-jaxb2-connector.
  1. Click on Help>Install New Software.  Type the following into the “Work with” drop down box:<br>
http://m2e-jaxb2-connector.eclipselabs.org.codespot.com/hg.update/org.eclipselabs.m2e.jaxb2.connector.update-site/ <br> Click the “Add…” button.
  2. Type an arbitrary name in the “Name” textbox when the “Add Repository” dialog appears and click the “OK” button:<br>  ![alt text](https://github.com/NimbleGen/bioinformatics/raw/master/documentation/images/jaxb.png "Add Repository Dialog")<br>
  3. Make sure the m2e-jaxb2-connector is selected and click the “Next>” button: <br>  ![alt text](https://github.com/NimbleGen/bioinformatics/raw/master/documentation/images/jaxb2.png "Install Jaxb Dialog")<br>
  4. An “Install Details” panel will appear, click the “Next>” button. <br>  ![alt text](https://github.com/NimbleGen/bioinformatics/raw/master/documentation/images/jaxb3.png "Install Jaxb Details Dialog")<br>
  5. Make sure the “I accept the terms of the license agreement” radio button is selected and click the “Finish” button. <br>  ![alt text](https://github.com/NimbleGen/bioinformatics/raw/master/documentation/images/jaxb4.png "Accept Jaxb License Dialog")<br>
  6. After the connector is installed you will be prompted to restart eclipse, comply.
7. Set a java 1.7 jdk in Eclipse to the default by clicking “Window”>>”Preferences” from the main menu.  Open the “Java”>>”Installed JREs” panel.  If there is not a jdk1.7 listed, add one by clicking the “Add...”  button.   Select “Standard VM” and click “Next”.   Click on the “Directory…” button and browse to an installed 1.7 jdk (For example: C:\Program Files\Java\jdk1.7.0_26) and click the “OK” button in the “Browse For Folder” dialog.  Click the “Finish” button in the “Add JRE” dialog.  Make sure that the 1.7 jdk has its checkbox selected and click “OK”. <br>  ![alt text](https://github.com/NimbleGen/bioinformatics/raw/master/documentation/images/jdk.png "Select JDK dialog")<br>  
