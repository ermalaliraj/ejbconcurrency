package com.ea.ejbconcurrency.dao;

import java.util.List;

import javax.ejb.Local;

import com.ea.ejbconcurrency.entity.SegmentEntity;
import com.ea.ejbconcurrency.entity.PointEntity;
import com.ea.ejbconcurrency.exception.ServerException;

@Local
public interface SegmentDAO {

	public PointEntity insertPoint(String name);
	public List<PointEntity> getAllPoints();
	public PointEntity getFirstFreePoint();
	public void removePoint(PointEntity point);
	public void removeAllPoints();
	
	public SegmentEntity insertSegment(String point1, String point2) throws ServerException;
	public List<SegmentEntity> getAllSegments() throws ServerException;
	public void removeAllSegments() throws ServerException;

}
