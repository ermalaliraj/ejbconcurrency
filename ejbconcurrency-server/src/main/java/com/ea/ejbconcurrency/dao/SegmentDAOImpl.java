package com.ea.ejbconcurrency.dao;

import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ea.ejbconcurrency.entity.SegmentEntity;
import com.ea.ejbconcurrency.entity.PointEntity;

@Stateless
public class SegmentDAOImpl implements SegmentDAO {

	private static final Log log = LogFactory.getLog(SegmentDAOImpl.class);

	@PersistenceContext(unitName = "exampleEM")
	protected EntityManager entityManager;

	public PointEntity insertPoint(String name) {
		PointEntity entity = new PointEntity(name);
		entityManager.persist(entity);
		log.debug("insertPoint: " + entity);
		return entity;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<PointEntity> getAllPoints() {
		StringBuffer sql = new StringBuffer();
		sql.append("select e from PointEntity e ");
		sql.append("order by e.name");

		List<PointEntity> list = entityManager.createQuery(sql.toString()).getResultList();
		log.debug("getAllPoints: " + list);
		return list;
	}

	@SuppressWarnings("unchecked")
	public PointEntity getFirstFreePoint() {
		StringBuffer sql = new StringBuffer();
		sql.append("select e from PointEntity e ");
		sql.append("order by e.name");

		Query q = entityManager.createQuery(sql.toString());
		List<PointEntity> list = q.getResultList();
		
		PointEntity ret = null;
		if(list!=null && list.size()>0){
			ret = list.get(0); 
		}
		log.debug("getFirstFreePoint: " + ret);
		return ret;
	}

	public void removePoint(PointEntity point) {
		PointEntity e = entityManager.getReference(PointEntity.class, point.getName());
		entityManager.remove(e);
		log.debug("removePoint: " + e);
	}

	@Override
	public void removeAllPoints() {
		for (PointEntity e : getAllPoints()) {
			removePoint(e);
		}
	}
	
	public SegmentEntity insertSegment(String point1, String point2) {
		SegmentEntity entity = new SegmentEntity(point1, point2);
		entityManager.persist(entity);
		log.debug("insertSegment: " + entity);
		return entity;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<SegmentEntity> getAllSegments() {
		StringBuffer sql = new StringBuffer();
		sql.append("select e from SegmentEntity e ");
		sql.append("order by e.id");

		List<SegmentEntity> list = entityManager.createQuery(sql.toString()).getResultList();
		log.debug("getAllSegments: " + list);
		return list;
	}

	@Override
	public void removeAllSegments() {
		StringBuffer sql = new StringBuffer();
		sql.append("delete from SegmentEntity ");
		int a = entityManager.createQuery(sql.toString()).executeUpdate();
		log.debug("removeAllSegments: " + a);
	}

}
