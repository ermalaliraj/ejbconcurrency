package com.ea.ejbconcurrency.service;

import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import com.ea.ejbconcurrency.dao.SegmentDAO;
import com.ea.ejbconcurrency.entity.SegmentEntity;
import com.ea.ejbconcurrency.entity.PointEntity;

@Stateless
public class SegmentServiceImpl implements SegmentService {

	@EJB(name = "segmentDAO")
	private SegmentDAO segmentDTO;

	public PointEntity insertPoint(String name) {
		return segmentDTO.insertPoint(name);
	}

	public List<PointEntity> getAllPoints() {
		return segmentDTO.getAllPoints();
	}

	public PointEntity getFirstFreePoint() {
		return segmentDTO.getFirstFreePoint();
	}

	public void removePoint(PointEntity point) {
		segmentDTO.removePoint(point);
	}

	public void removeAllPoints() {
		segmentDTO.removeAllSegments();
	}
	
	public SegmentEntity insertSegment(String point1, String point2) {
		return segmentDTO.insertSegment(point1, point2);
	}

	public List<SegmentEntity> getAllSegments() {
		return segmentDTO.getAllSegments();
	}

	@Override
	public void cleanDashboard() {
		segmentDTO.removeAllSegments();
		segmentDTO.removeAllPoints();
	}

}
