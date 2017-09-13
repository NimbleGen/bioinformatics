package com.roche.heatseq.process;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.roche.sequencing.bioinformatics.common.utils.DateUtil;
import com.roche.sequencing.bioinformatics.common.utils.probeinfo.Probe;

public class H2ReadToProbeFileStore {

	// NOTE: This was part of an attempt to push a memory stored map into file storage. In the end the
	// query times were a big issue so the effort was stopped. This is being placed here just in case this solution
	// is needed in the short term. If you are reading this and have no idea what it is feel free to delete it.
	// Also see ReadToProbeAssignmentResultsWithFileStore
	// Kurt Heilman

	private static final String DB_DRIVER = "org.h2.Driver";
	private static final String DB_FILE_CONNECTION_PREFIX = "jdbc:h2:file:";
	private static final String DB_FILE_FAST_DATABASE_IMPORT_SUFFIX = ";LOG=0;CACHE_SIZE=65536;LOCK_MODE=0;UNDO_LOG=0";
	private static final String DB_USER = "";
	private static final String DB_PASSWORD = "";

	private static final int INSERT_SIZE = 100000;

	private static final String READ_NAME_STRING_SIZE_PATTERN = "&READ_NAME_STRING_SIZE&";
	private static final String PROBE_ID_STRING_SIZE_PATTERN = "&PROBE_ID_STRING_SIZE&";

	private static final String CREATE_QUERY = "CREATE TABLE READ2PROBE(readName varchar(" + READ_NAME_STRING_SIZE_PATTERN + "), probeId varchar(" + PROBE_ID_STRING_SIZE_PATTERN + "));";
	private static final String INSERT_QUERY = "INSERT INTO READ2PROBE" + "(readName, probeId) values" + "(?,?);";
	private static final String SELECT_PROBE_ID_BY_READ_NAME_QUERY = "SELECT probeId FROM READ2PROBE WHERE readName = ?;";
	private static final String SELECT_ALL_READ_NAMES_QUERY = "SELECT DISTINCT(readName) FROM READ2PROBE;";
	private static final String ADD_READ_NAME_INDEX_QUERY = "CREATE INDEX IF NOT EXISTS readNameIndex ON READ2PROBE(readName);";

	private static Connection connection;

	private final PreparedStatement insertStatement;
	private final PreparedStatement selectProbeIdByReadNameStatement;
	private final PreparedStatement selectAllReadNamesStatement;
	private final PreparedStatement createReadNameIndexStatement;

	public H2ReadToProbeFileStore(File dbFile, int readNameStringSize, int probeIdStringSize) {
		Connection connection = getDBConnection(dbFile);
		try {

			// create the table
			Statement statement = connection.createStatement();
			String createQuery = CREATE_QUERY.replaceAll(READ_NAME_STRING_SIZE_PATTERN, "" + readNameStringSize);
			createQuery = createQuery.replaceAll(PROBE_ID_STRING_SIZE_PATTERN, "" + probeIdStringSize);
			statement.execute(createQuery);
			statement.close();

			this.insertStatement = connection.prepareStatement(INSERT_QUERY);
			this.selectProbeIdByReadNameStatement = connection.prepareStatement(SELECT_PROBE_ID_BY_READ_NAME_QUERY);
			this.selectAllReadNamesStatement = connection.prepareStatement(SELECT_ALL_READ_NAMES_QUERY);
			this.createReadNameIndexStatement = connection.prepareStatement(ADD_READ_NAME_INDEX_QUERY);

		} catch (SQLException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					connection.close();
				} catch (SQLException e) {
					throw new IllegalStateException(e.getMessage(), e);
				}

				for (File file : dbFile.getParentFile().listFiles()) {
					if (file.getName().startsWith(dbFile.getName())) {
						file.delete();
					}
				}
			}
		});
	}

	public void putAll(Map<String, Set<Probe>> readNameToProbe) {
		synchronized (insertStatement) {
			int count = 0;
			for (Entry<String, Set<Probe>> entry : readNameToProbe.entrySet()) {
				String readName = entry.getKey();
				for (Probe probe : entry.getValue()) {
					try {
						insertStatement.setString(1, readName);
						insertStatement.setString(2, probe.getProbeId());
						insertStatement.addBatch();
						count++;
					} catch (SQLException e) {
						throw new IllegalStateException(e.getMessage(), e);
					}

					if (count > INSERT_SIZE) {
						try {
							insertStatement.executeBatch();
							insertStatement.clearBatch();
						} catch (SQLException e) {
							throw new IllegalStateException(e.getMessage(), e);
						}
						count = 0;
					}
				}
			}
			try {
				insertStatement.executeBatch();
				insertStatement.clearBatch();
			} catch (SQLException e) {
				throw new IllegalStateException(e.getMessage(), e);
			}

		}
	}

	public void indexReadNames() {
		System.out.println("starting to index");
		long start = System.currentTimeMillis();
		// index on read name
		try {
			createReadNameIndexStatement.executeUpdate();
		} catch (SQLException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}

		long stop = System.currentTimeMillis();
		System.out.println("done indexing in:" + DateUtil.convertMillisecondsToHHMMSSMMM(stop - start));
	}

	public Iterator<String> getReadNames() {
		Iterator<String> readNameIter = null;
		synchronized (selectAllReadNamesStatement) {
			try {
				ResultSet resultSet = selectAllReadNamesStatement.executeQuery();
				readNameIter = new ResultSetIterator(resultSet);
			} catch (SQLException e) {
				throw new IllegalStateException(e.getMessage(), e);
			}
		}
		return readNameIter;
	}

	private static class ResultSetIterator implements Iterator<String> {

		private ResultSet resultSet;

		public ResultSetIterator(ResultSet resultSet) {
			this.resultSet = resultSet;
		}

		@Override
		public boolean hasNext() {
			boolean hasNext = false;
			if (resultSet != null) {
				try {
					hasNext = resultSet.next();
				} catch (SQLException e) {
					hasNext = false;
				}
			}
			return hasNext;
		}

		@Override
		public String next() {
			String nextReadName = null;
			try {
				nextReadName = resultSet.getString("readName");
			} catch (SQLException e) {
				throw new IllegalStateException(e.getMessage(), e);
			}
			return nextReadName;
		}

	}

	public Set<String> getProbeIds(String readName) {
		Set<String> probeIds = new HashSet<String>();
		synchronized (selectProbeIdByReadNameStatement) {
			try {
				selectProbeIdByReadNameStatement.setString(1, readName);
				ResultSet resultSet = selectProbeIdByReadNameStatement.executeQuery();
				while (resultSet.next()) {
					String probeId = resultSet.getString("probeId");
					probeIds.add(probeId);
				}
			} catch (SQLException e) {
				// throw new IllegalStateException("Unable to retrieve probeIds for readName[" + readName + "]. " + e.getMessage(), e);
				try {
					selectProbeIdByReadNameStatement.setString(1, readName);
					ResultSet resultSet = selectProbeIdByReadNameStatement.executeQuery();
					while (resultSet.next()) {
						String probeId = resultSet.getString("probeId");
						probeIds.add(probeId);
					}
				} catch (SQLException e2) {

				}
			}

		}
		return probeIds;
	}

	public void close() {
		if (connection != null) {
			try {
				connection.close();
			} catch (SQLException e) {
				throw new IllegalStateException(e.getMessage(), e);
			}
		}
	}

	private static Connection getDBConnection(File dbFile) {
		if (connection == null) {
			try {
				Class.forName(DB_DRIVER);
			} catch (ClassNotFoundException e) {
				System.out.println(e.getMessage());
			}
			try {
				connection = DriverManager.getConnection(DB_FILE_CONNECTION_PREFIX + dbFile.getAbsolutePath() + DB_FILE_FAST_DATABASE_IMPORT_SUFFIX, DB_USER, DB_PASSWORD);
			} catch (SQLException e) {
				throw new IllegalStateException(e.getMessage(), e);
			}
		}
		return connection;
	}
}
