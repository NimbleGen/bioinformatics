package com.roche.sequencing.bioinformatics.common.text.viewer;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.TransferHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DragAndDropFileTransferHandler extends TransferHandler {

	private final Logger logger = LoggerFactory.getLogger(DragAndDropFileTransferHandler.class);
	private static final long serialVersionUID = 1L;

	private final TextViewer textViewer;

	DragAndDropFileTransferHandler(TextViewer textViewer) {
		super();
		this.textViewer = textViewer;
	}

	@Override
	public boolean canImport(TransferHandler.TransferSupport support) {
		boolean canImport = support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
		return canImport;
	}

	@Override
	public boolean importData(TransferHandler.TransferSupport support) {

		boolean success = false;
		if (canImport(support)) {
			List<File> files = null;
			try {
				Transferable transferable = support.getTransferable();
				files = getFiles(transferable);
				success = true;
			} catch (Exception e) {
				success = false;
				e.printStackTrace();
				throw new IllegalStateException(e.getMessage(), e);
			}

			if (files == null) {
				files = new ArrayList<File>();
			}

			final List<File> finalFiles = files;

			new Thread(new Runnable() {

				@Override
				public void run() {
					for (File file : finalFiles) {
						textViewer.readInFile(file);
					}
				}
			}).start();
		}

		return success;
	}

	@SuppressWarnings("unchecked")
	private List<File> getFiles(Transferable transferable) {
		List<File> files = null;
		try {
			files = (java.util.List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
		} catch (UnsupportedFlavorException e) {
			logger.warn(e.getMessage(), e);
		} catch (IOException e) {
			logger.warn(e.getMessage(), e);
		}
		return files;
	}
}
