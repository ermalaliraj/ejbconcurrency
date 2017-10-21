package com.ea.ejbconcurrency.ejbimpl;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.persistence.PersistenceException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ea.ejbconcurrency.dto.SegmentDTO;
import com.ea.ejbconcurrency.entity.SegmentEntity;
import com.ea.ejbconcurrency.entity.PointEntity;
import com.ea.ejbconcurrency.exception.BusinessException;
import com.ea.ejbconcurrency.exception.ServerException;
import com.ea.ejbconcurrency.remote.SegmentRemote;
import com.ea.ejbconcurrency.service.SegmentService;

@Stateless
@Remote(SegmentRemote.class)
public class SegmentEMServiceDao implements SegmentRemote {

	private static final Log log = LogFactory.getLog(SegmentEMServiceDao.class);

	@EJB(name = "segmentService")
	protected SegmentService segmentService;

	public SegmentDTO addPointToDashboard(String name) throws ServerException {
		SegmentDTO segment = null;
		try {
			log.info("Inserting point '" + name + "'. If present any free (uncoupled) point, a new segment between two nodes will be created. ");
			PointEntity freePoint = segmentService.getFirstFreePoint();
			if (freePoint != null) {
				Thread.sleep(1000*2);
				SegmentEntity newSegment = segmentService.insertSegment(freePoint.getName(), name);
				segment = new SegmentDTO(newSegment.getPoint1(), newSegment.getPoint2());
				segmentService.removePoint(freePoint);
			} else {
				log.info("No free points present for coupling, inserting '"+name+"' in free points list");
				segmentService.insertPoint(name);
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
	public List<SegmentDTO> getAllSegments() throws ServerException {
		try {
			List<SegmentDTO> res = new ArrayList<SegmentDTO>();
			SegmentDTO dto;
			for (SegmentEntity e : segmentService.getAllSegments()) {
				dto = new SegmentDTO(e.getPoint1(), e.getPoint2());
				res.add(dto);
			}
			return res;
		} catch (Exception e) {
			log.warn("General exception getAllSegments", e);
			throw new ServerException("General exception while reading the Dashboard", e);
		}
	}
	
	public List<String> getAllPoints() throws ServerException{
		try {
			List<String> res = new ArrayList<String>();
			for (PointEntity e : segmentService.getAllPoints()) {
				res.add(e.getName());
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
			segmentService.cleanDashboard();
			log.info("Removed all Points and Segments");
		} catch (Exception e) {
			log.warn("General exception while cleaning the Dashboard", e);
			throw new ServerException("General exception while cleaning the Dashboard", e);
		}
	}

	
	
}
