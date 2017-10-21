package com.ea.ejbconcurrency.ejbimpl;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ea.ejbconcurrency.dto.SegmentDTO;
import com.ea.ejbconcurrency.entity.SegmentEntity;
import com.ea.ejbconcurrency.entity.PointEntity;
import com.ea.ejbconcurrency.exception.BusinessException;
import com.ea.ejbconcurrency.exception.ServerException;
import com.ea.ejbconcurrency.remote.SegmentRemote;

@Stateless
@Remote(SegmentRemote.class)
public class SegmentEM implements SegmentRemote {

	private static final Log log = LogFactory.getLog(SegmentEM.class);

	@PersistenceContext(unitName = "exampleEM")
	protected EntityManager entityManager;

	/**
	 * Add new point A1 in the dashboard. 
	 * 1. Read from table POINT first not yet coupled point, A0.
	 * 2. If present a free point
	 * 		2.a. Insert new segment (A0, A1)
	 * 		2.b. delete point A0.      (no need to save A1, since it got coupled)
	 * 3. If NOT present any free point
	 * 		3.a. save the point in a temporarily table POINT, until a new point come for coupling.
	 */
	public SegmentDTO addPointToDashboard(String name) throws ServerException {
		SegmentDTO segment = null;
		try {
			log.info("Inserting point '" + name + "'. If present any free (uncoupled) point, a new segment between two will be created. ");
			String freePoint = getFirstFreePoint();
			if (freePoint != null) {
				Thread.sleep(1000*2);
				SegmentEntity newSegment = insertSegment(freePoint, name);
				segment = new SegmentDTO(newSegment.getPoint1(), newSegment.getPoint2());
				removePoint(freePoint);
			} else {
				log.info("No free points present for coupling, inserting '"+name+"' in free points list");
				insertPoint(name);
			}
			return segment;
		} catch (PersistenceException e) {
			log.warn("Point '" + name + "' already present in the dashboard. Try with a different name.");
			throw new BusinessException("Point '" + name + "' already present in the dashboard. Try with a different name.");
		} catch (Exception e) {
			log.error("General exception while inserting point '" + name + "'", e);
			throw new ServerException("General exception while inserting point '" + name + "'");
		}
	}

	
	@Override
	public void cleanDashboard() throws ServerException {
		try {
			removeAllSegments();
			removeAllPoints();
			log.info("Removed all Points and Segments");
		} catch (Exception e) {
			log.warn("General exception while cleaning the Dashboard", e);
			throw new ServerException("General exception while cleaning the Dashboard", e);
		}
	}
	
	
	@Override
	public List<String> getAllPoints() {
		try {
			List<String> res = new ArrayList<String>();
			StringBuffer sql = new StringBuffer();
			sql.append("select e from PointEntity e ");
			sql.append("order by e.name");
	
			@SuppressWarnings("unchecked")
			List<PointEntity> list = entityManager.createQuery(sql.toString()).getResultList();
			log.debug("getAllPoints: " + list);
			
			for (PointEntity e : list) {
				res.add(e.getName());
			}
			return res;
		} catch (Exception e) {
			log.warn("General exception while getAllPoints", e);
			throw new ServerException("General exception while reading the Dashboard", e);
		}
	}


	@Override
	public List<SegmentDTO> getAllSegments() {
		try {
			List<SegmentDTO> res = new ArrayList<SegmentDTO>();
			StringBuffer sql = new StringBuffer();
			sql.append("select e from SegmentEntity e ");
			sql.append("order by e.id");
	
			@SuppressWarnings("unchecked")
			List<SegmentEntity> list = entityManager.createQuery(sql.toString()).getResultList();
			log.debug("getAllSegments: " + list);
		
			SegmentDTO segment = null;
			for (SegmentEntity e : list) {
				segment = new SegmentDTO();
				segment.setId(e.getId());
				segment.setPoint1(e.getPoint1());
				segment.setPoint2(e.getPoint2());
				res.add(segment);
			}
			return res;
		} catch (Exception e) {
			log.warn("General exception while getAllSegments", e);
			throw new ServerException("General exception in getAllSegments", e);
		}
	}


	private PointEntity insertPoint(String name) {
		PointEntity entity = new PointEntity(name);
		entityManager.persist(entity);
		log.debug("insertPoint: " + entity);
		return entity;
	}

	@SuppressWarnings("unchecked")
	private String getFirstFreePoint() {
		StringBuffer sql = new StringBuffer();
		sql.append("select e from PointEntity e ");
		sql.append("order by e.name");

		Query q = entityManager.createQuery(sql.toString());
		List<PointEntity> list = q.getResultList();
		
		String ret = null;
		if(list!=null && list.size()>0){
			ret = list.get(0).getName(); 
		}
		log.debug("getFirstFreePoint: " + ret);
		return ret;
	}

	private void removePoint(String point) {
		PointEntity e = entityManager.getReference(PointEntity.class, point);
		entityManager.remove(e);
		log.debug("removePoint: " + e);
	}

	private void removeAllPoints() {
		for (String e : getAllPoints()) {
			removePoint(e);
		}
	}
	
	private SegmentEntity insertSegment(String point1, String point2) {
		SegmentEntity entity = new SegmentEntity(point1, point2);
		entityManager.persist(entity);
		log.debug("insertSegment: " + entity);
		return entity;
	}

	private void removeSegment(Integer segmentId) {
		SegmentEntity e = entityManager.getReference(SegmentEntity.class, segmentId);
		entityManager.remove(e);
		log.debug("removeSegment: " + e);
	}

	private void removeAllSegments() {
		for (SegmentDTO e : getAllSegments()) {
			removeSegment(e.getId());
		}
	}
	
}
