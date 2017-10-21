package com.ea.ejbconcurrency.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ea.ejbconcurrency.exception.ServerException;

@Singleton
@Startup
public class DBPopulation {

	protected static final transient Log log = LogFactory.getLog(DBPopulation.class);

	@Resource(mappedName = "java:jboss/datasources/ExampleDS")
	private DataSource dataSource;

	@PostConstruct
	public void init() throws Exception {
		dropDBTables();
		createDBTables();
	}

	private void dropDBTables() {
		Connection connection = null;
		PreparedStatement ps = null;
		
		StringBuffer sql = new StringBuffer();
		sql.append("drop table if exists POINT; drop table if exists SEGMENT; ");
		
		try {
			connection = dataSource.getConnection();
			ps = connection.prepareStatement(sql.toString());
			boolean exec = ps.execute();
			log.info("Tables dropped! exec: "+exec);
		} catch (SQLException e) {
			throw new ServerException("Exception in insertPoint", e);
		} finally {
			DBUtil.close(ps);
			DBUtil.close(connection);
		}
	}

	public void createDBTables() {
		Connection connection = null;
		PreparedStatement ps = null;
		
		StringBuffer sql = new StringBuffer();
		sql.append("create table POINT(name varchar(20)); ");
		sql.append("create table SEGMENT(segment_id int not null primary key auto_increment, point1 varchar(20), point2 varchar(20));");

		try {
			connection = dataSource.getConnection();
			ps = connection.prepareStatement(sql.toString());
			boolean exec = ps.execute();
			
			log.info("DB created! exec: "+exec);
		} catch (SQLException e) {
			throw new ServerException("Exception in insertPoint", e);
		} finally {
			DBUtil.close(ps);
			DBUtil.close(connection);
		}
	}

}