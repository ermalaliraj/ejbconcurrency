package com.ea.ejbconcurrency.ejbimpl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.sql.DataSource;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ea.ejbconcurrency.dto.SegmentDTO;
import com.ea.ejbconcurrency.exception.ServerException;
import com.ea.ejbconcurrency.remote.DashboardRemote;
import com.ea.ejbconcurrency.util.DBUtil;

@Stateless
@Remote(DashboardRemote.class)
@TransactionManagement(TransactionManagementType.BEAN)
public class DashboardDSTxBean implements DashboardRemote {

	private static final Log log = LogFactory.getLog(DashboardDSTxBean.class);
	
	@Resource(mappedName = "java:jboss/datasources/ExampleDS")
	private DataSource dataSource;
	@Resource
	private UserTransaction tx;

	public SegmentDTO addPointToDashboard(String newPoint) throws ServerException {
		SegmentDTO segment = null;
		Connection connection = null;
		try {
			connection = dataSource.getConnection();
			connection.setAutoCommit(false);
			// connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
			connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);

			tx.begin();

			log.info("Inserting point '" + newPoint + "'. If present any free (uncoupled) point, a new segment between two nodes will be created. ");
			String freePoint = getFirstFreePoint(connection);
			if (freePoint != null) {
				Thread.sleep(1000 * 2);
				segment = insertSegment(freePoint, newPoint, connection);
				log.info("Inserted segment: " + segment);
				removePoint(freePoint, connection);
				log.info("Removed temp point: " + freePoint);
			} else {
				log.info("No free points present for coupling, inserting '" + newPoint + "' in free points list");
				insertPoint(newPoint, connection);
			}
			tx.commit();
			// connection.commit();
		} catch (NotSupportedException | InterruptedException 
				| SQLException
				| SecurityException | IllegalStateException | RollbackException | HeuristicMixedException | HeuristicRollbackException | SystemException  e) {
			try {
				tx.rollback();
			} catch (SecurityException | IllegalStateException | SystemException e1) {
				log.error("Error tx.commit();", e1);
			}

			try {
				connection.rollback();
			} catch (SQLException e1) {
				log.error("Error rollback exception while inserting point '" + newPoint + "'", e1);
			}

			log.error("General exception while inserting point '" + newPoint + "'", e);
			throw new ServerException("Exception inserting point'" + newPoint + "': " + e.getMessage());
		} finally {
			DBUtil.close(connection);
		}
		return segment;
	}

	@Override
	public List<SegmentDTO> getAllSegments() throws ServerException {
		List<SegmentDTO> res = new ArrayList<>();
		Connection connection = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		String sql = "select segment_id, point1, point2 from SEGMENT";
		log.debug("getAllSegments sql: " + sql);
		try {
			connection = dataSource.getConnection();
			ps = connection.prepareStatement(sql);
			rs = ps.executeQuery();

			SegmentDTO segment = null;
			while (rs.next()) {
				segment = new SegmentDTO();
				segment.setId(rs.getInt("segment_id"));
				segment.setPoint1(rs.getString("point1"));
				segment.setPoint2(rs.getString("point2"));
				res.add(segment);
			}
			// log.debug("Nr segments found: " + res.size());
			log.debug("Segments in DB:  " + res);
		} catch (SQLException e) {
			throw new ServerException("Exception in getAllSegments()", e);
		} finally {
			DBUtil.close(rs);
			DBUtil.close(ps);
			DBUtil.close(connection);
		}
		return res;
	}

	public List<String> getAllPoints() throws ServerException {
		List<String> res = new ArrayList<>();
		Connection connection = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		String sql = "select name from POINT";
		log.debug("getAllPoints sql: " + sql);
		try {
			connection = dataSource.getConnection();
			ps = connection.prepareStatement(sql);
			rs = ps.executeQuery();

			String point = null;
			while (rs.next()) {
				point = rs.getString("name");
				res.add(point);
			}
			log.debug("Points in DB:  " + res);
		} catch (SQLException e) {
			throw new ServerException("Exception in getAllPoints()", e);
		} finally {
			DBUtil.close(rs);
			DBUtil.close(ps);
			DBUtil.close(connection);
		}
		return res;
	}

	@Override
	public void cleanDashboard() throws ServerException {
		try {
			removeAllSegments();
			removeAllPoints();
			log.info("Removed all Points and Segments");
		} catch (Exception e) {
			log.error("General exception while cleaning the Dashboard", e);
			throw new ServerException("General exception while cleaning the Dashboard", e);
		}
	}

	private void insertPoint(String newPoint, Connection connection) throws SQLException {
		PreparedStatement ps = null;
		int tot = 0;
		String sql = "insert into POINT values(?)";
		log.debug("insertPoint sql: " + sql + "; newPoint: " + newPoint);
		ps = connection.prepareStatement(sql);
		ps.setString(1, newPoint);
		tot = ps.executeUpdate();
		log.info("Inserted " + tot + " row");
		DBUtil.close(ps);
	}

	private String getFirstFreePoint(Connection connection) throws SQLException {
		String freePoint = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		String sql = "select name from POINT";
		log.debug("getFirstFreePoint sql: " + sql);
		ps = connection.prepareStatement(sql);
		rs = ps.executeQuery();
		if (rs.next()) {
			freePoint = rs.getString("name");
		}
		log.info("Free point found: " + freePoint);
		return freePoint;
	}

	private void removePoint(String point, Connection connection) throws SQLException {
		PreparedStatement ps = null;
		int tot = 0;
		String sql = "delete from POINT where name = ? ";
		log.debug("removePoint sql: " + sql + "; point: " + point);
		ps = connection.prepareStatement(sql);
		ps.setString(1, point);
		tot = ps.executeUpdate();
		log.info("Deleted " + tot + " segments");
	}

	private void removeAllPoints() {
		Connection connection = null;
		PreparedStatement ps = null;
		int tot = 0;

		String sql = "delete from POINT";
		log.debug("removeAllPoints sql: " + sql);

		try {
			connection = dataSource.getConnection();
			ps = connection.prepareStatement(sql);
			tot = ps.executeUpdate();
			log.info("Deleted " + tot + " segments");
		} catch (SQLException e) {
			throw new ServerException("Exception in removeAllPoint()", e);
		} finally {
			DBUtil.close(ps);
			DBUtil.close(connection);
		}
	}

	private SegmentDTO insertSegment(String point1, String point2, Connection connection) {
		PreparedStatement ps = null;
		SegmentDTO ret = null;

		String sql = "insert into SEGMENT values(NULL, ?, ?)";
		log.debug("insertSegment sql: " + sql + "; point1: " + point1 + ", point2: " + point2);

		try {
			ps = connection.prepareStatement(sql);
			ps.setString(1, point1);
			ps.setString(2, point2);
			int tot = ps.executeUpdate();
			log.info("Inserted " + tot + " row");
			ret = new SegmentDTO(point1, point2);
		} catch (SQLException e) {
			throw new ServerException(e);
		} finally {
			DBUtil.close(ps);
		}
		return ret;
	}

	private void removeAllSegments() {
		Connection connection = null;
		PreparedStatement ps = null;
		int tot = 0;

		String sql = "delete from SEGMENT";
		log.debug("removeAllSegments sql: " + sql);

		try {
			connection = dataSource.getConnection();
			ps = connection.prepareStatement(sql);
			tot = ps.executeUpdate();
			log.info("Deleted " + tot + " segments");
		} catch (SQLException e) {
			throw new ServerException("Exception in removeAllSegments()", e);
		} finally {
			DBUtil.close(ps);
			DBUtil.close(connection);
		}
	}
}
