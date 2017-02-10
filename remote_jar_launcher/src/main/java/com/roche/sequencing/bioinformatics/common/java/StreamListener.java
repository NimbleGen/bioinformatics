package com.roche.sequencing.bioinformatics.common.java;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;

public class StreamListener {
	public static final String NEWLINE = System.getProperty("line.separator");

	private final StringBuilder string;

	public StreamListener(final InputStream inputStream, final PrintStream optionalPrintStream) {

		string = new StringBuilder();
		Thread thread = new Thread(new Runnable() {
			public void run() {
				BufferedReader sc = null;
				try {
					sc = new BufferedReader(new InputStreamReader(inputStream));
					String line = null;
					while ((line = sc.readLine()) != null) {
						string.append(line + NEWLINE);
						if (optionalPrintStream != null) {
							optionalPrintStream.println(line);
						}
					}
				} catch (IOException e) {
					throw new IllegalStateException(e.getMessage(), e);
				} finally {
					if (sc != null) {
						try {
							sc.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}, "Stream_Listener_Thread");
		thread.start();
	}

	public String getString() {
		return string.toString();
	}
}
