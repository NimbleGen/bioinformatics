package com.roche.sequencing.bioinformatics.common.text.viewer;

import java.io.File;
import java.util.prefs.Preferences;

import javax.swing.JFileChooser;

public class DialogHelper {

	private static Preferences preferences = Preferences.userRoot().node(DialogHelper.class.getName());
	private final static String LAST_VIEW_DIRECTORY_PROPERTIES_KEY = "lastview.lastdirectory";

	private DialogHelper() {
		throw new AssertionError();
	}

	public static File getFileForSavingClipBoardContents(TextViewerPanel textViewerPanel) {
		File fileForSavingClipboardContents = null;
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fileChooser.setApproveButtonText("Save Selected Text to File");
		fileChooser.setDialogTitle("Choose File for Selected Text");

		String lastDirectory = preferences.get(LAST_VIEW_DIRECTORY_PROPERTIES_KEY, null);
		if (lastDirectory != null) {
			fileChooser.setCurrentDirectory(new File(lastDirectory));
		}
		int returnValue = fileChooser.showOpenDialog(textViewerPanel.getTextViewer());
		if (returnValue == JFileChooser.APPROVE_OPTION) {
			fileForSavingClipboardContents = fileChooser.getSelectedFile();
			preferences.put(LAST_VIEW_DIRECTORY_PROPERTIES_KEY, fileForSavingClipboardContents.getParent());
		}
		return fileForSavingClipboardContents;
	}

}
