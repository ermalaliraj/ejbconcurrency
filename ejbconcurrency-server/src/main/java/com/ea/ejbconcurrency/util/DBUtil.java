package com.ea.ejbconcurrency.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DBUtil {

	private static final Log log = LogFactory.getLog(DBUtil.class);
	
	public static void close(Object sqlObject) {
		if (sqlObject != null) {
			if (sqlObject instanceof Connection)
				closeConnection((Connection) sqlObject);
			if (sqlObject instanceof Statement)
				closeStatement((Statement) sqlObject);
			if (sqlObject instanceof PreparedStatement)
				closePreparedStatement((PreparedStatement) sqlObject);
			if (sqlObject instanceof ResultSet)
				closeResultSet((ResultSet) sqlObject);
		}
	}

	private static void closeConnection(Connection connection) {
		try {
			connection.close();
		} catch (SQLException sqle) {
			log.warn("Impossibile chiudere la connessione SQL", sqle);
		}
	}

	private static void closeStatement(Statement statement) {
		try {
			statement.close();
		} catch (SQLException sqle) {
			log.warn("Impossibile chiudere lo statement SQL", sqle);
		}
	}
	

	private static void closePreparedStatement(PreparedStatement preparedStatement) {
		try {
			preparedStatement.close();
		} catch (SQLException sqle) {
			log.warn("Impossibile chiudere il preparedStatement SQL", sqle);
		}
	}

	private static void closeResultSet(ResultSet resultSet) {
		try {
			resultSet.close();
		} catch (SQLException sqle) {
			log.warn("Impossibile chiudere il resultSet SQL", sqle);
		}
	}

}
