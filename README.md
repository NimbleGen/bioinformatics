bioinformatics
==============

Bioinformatics tools developed at Roche NimbleGen.

Building the Heatseq Tool in Eclipse
-------------------------

**Section 1**: Eclipse Setup

1.  Download the current Eclipse IDE for Java Developers (Juno) from http://www.eclipse.org/downloads/.  
2.  Install Eclipse by unzipping the downloaded file into the desired install location (Recommended: C://Eclipse/Juno/).  Note:  If you install in Program Files you may have to run as Admin to get plugins to install correctly.
2A (optional) – Deleting the C://Users/[user name]/.eclipse directory may alleviate issues with installing plug-ins.
3.  Run eclipse by double clicking the eclipse.exe file (sample location: C://Eclipse/Juno/eclipse/eclipse.exe).  You may want to right click the eclipse.exe file and “Run as Administrator” if you run into issues installing plug-ins.
4.  Create a workspace by browsing to the desired location for the heatseq code when the “Select a workspace” dialog appears (Recommended:  C://Eclipse_Workspaces/heatseq_workspace).
5.  Install EGit plug-in by clicking “Help”>>”Install New Software…” from the main menu within Eclipse.   Add http://download.eclipse.org/releases/juno to the "Work with" text box and "git" in the type filter textbox and hit return.  Then select and install:
  1. Eclipse EGit
  2. Eclipse JGit
