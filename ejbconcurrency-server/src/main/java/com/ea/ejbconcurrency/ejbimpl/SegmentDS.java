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
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.PersistenceException;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ea.ejbconcurrency.dto.SegmentDTO;
import com.ea.ejbconcurrency.exception.BusinessException;
import com.ea.ejbconcurrency.exception.ServerException;
import com.ea.ejbconcurrency.remote.SegmentRemote;
import com.ea.ejbconcurrency.util.DBUtil;

@Stateless
@Remote(SegmentRemote.class)
public class SegmentDS implements SegmentRemote {

	private static final Log log = LogFactory.getLog(SegmentDS.class);

	@Resource(mappedName = "java:jboss/datasources/ExampleDS")
	private DataSource dataSource;

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public SegmentDTO addPointToDashboard(String newPoint) throws ServerException {
		SegmentDTO segment = null;
		try {
			log.info("Inserting point '" + newPoint + "'. If present any free (uncoupled) point, a new segment between two nodes will be created. ");
			String freePoint = getFirstFreePoint();
			if (freePoint != null) {
				Thread.sleep(1000*2);
				segment = insertSegment(freePoint, newPoint);
				log.info("Inserted segment: " + segment);
				removePoint(freePoint);
				log.info("Removed temp point: " + freePoint);
			} else {
				log.info("No free points present for coupling, inserting '"+newPoint+"' in free points list");
				insertPoint(newPoint);
			}
			return segment;
		} catch (PersistenceException e) {
			log.warn("Point '" + newPoint + "' already present in the dashboard. Try with a different name.");
			throw new BusinessException("Point '" + newPoint + "' already present in the dashboard. Try with a different name.");
		} catch (Exception e) {
			log.error("General exception while inserting point '" + newPoint + "'", e);
			throw new ServerException("General exception while inserting point '" + newPoint + "'");
		}
	}

	@Override
	public List<SegmentDTO> getAllSegments() throws ServerException {
		try {
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
				//log.debug("Nr segments found:  " + res.size());
				log.debug("Segments in DB:  " + res);
			} catch (SQLException e) {
				throw new ServerException("Exception in getAllSegments()", e);
			} finally {
				DBUtil.close(rs);
				DBUtil.close(ps);
				DBUtil.close(connection);
			}
			return res;
		} catch (Exception e) {
			log.error("General exception while reading the Dashboard", e);
			throw new ServerException("General exception while reading the Dashboard", e);
		}
	}
	

	public List<String> getAllPoints() throws ServerException{
		try {
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
		} catch (Exception e) {
			log.warn("General exception while getAllPoints", e);
			throw new ServerException("General exception while reading the Dashboard", e);
		}
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
	
	private void insertPoint(String newPoint) {
		Connection connection = null;
		PreparedStatement ps = null;
		int tot = 0;

		String sql = "insert into POINT values(?)";
		log.debug("insertPoint sql: " + sql + "; newPoint: "+newPoint);

		try {
			connection = dataSource.getConnection();
			ps = connection.prepareStatement(sql);
			ps.setString(1, newPoint);
			tot = ps.executeUpdate();
			log.info("Inserted "+tot+" row");
		} catch (SQLException e) {
			throw new ServerException("Exception in insertPoint", e);
		} finally {
			DBUtil.close(ps);
			DBUtil.close(connection);
		}
	}

	private String getFirstFreePoint() {
		String freePoint = null;
		Connection connection = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		String sql = "select name from POINT";
		log.debug("getFirstFreePoint sql: " + sql);
		try {
			connection = dataSource.getConnection();
			ps = connection.prepareStatement(sql);
			rs = ps.executeQuery();
			
			if (rs.next()) {
				freePoint = rs.getString("name");
			}
			log.info("Free point found: " + freePoint);
		} catch (SQLException e) {
			throw new ServerException("Exception in getFirstFreePoint()", e);
		} finally {
			DBUtil.close(rs);
			DBUtil.close(ps);
			DBUtil.close(connection);
		}
		return freePoint;
	}

	private void removePoint(String point) {
		Connection connection = null;
		PreparedStatement ps = null;
		int tot = 0;

		String sql = "delete from POINT where name = ? ";
		log.debug("removePoint sql: " + sql + "; point: " + point);

		try {
			connection = dataSource.getConnection();
			ps = connection.prepareStatement(sql);
			ps.setString(1, point);
			tot = ps.executeUpdate();
			log.info("Deleted "+tot+" segments");
		} catch (SQLException e) {
			throw new ServerException("Exception in removePoint()", e);
		} finally {
			DBUtil.close(ps);
			DBUtil.close(connection);
		}
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
			log.info("Deleted "+tot+" segments");
		} catch (SQLException e) {
			throw new ServerException("Exception in removeAllPoint()", e);
		} finally {
			DBUtil.close(ps);
			DBUtil.close(connection);
		}
	}
	
	private SegmentDTO insertSegment(String point1, String point2) {
		Connection connection = null;
		PreparedStatement ps = null;
		SegmentDTO ret = null;

		String sql = "insert into SEGMENT values(NULL, ?, ?)";
		log.debug("insertSegment sql: " + sql + "; point1: "+point1+", point2: "+point2);

		try {
			connection = dataSource.getConnection();
			ps = connection.prepareStatement(sql);
			ps.setString(1, point1);
			ps.setString(2, point2);
			int tot = ps.executeUpdate();
			log.info("Inserted "+tot+" row");
			ret = new SegmentDTO(point1, point2);
		} catch (SQLException e) {
			throw new ServerException("Exception in insertSegment()", e);
		} finally {
			DBUtil.close(ps);
			DBUtil.close(connection);
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
			log.info("Deleted "+tot+" segments");
		} catch (SQLException e) {
			throw new ServerException("Exception in removeAllSegments()", e);
		} finally {
			DBUtil.close(ps);
			DBUtil.close(connection);
		}
	}
}
