package com.ea.ejbconcurrency.ejbimpl;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ea.ejbconcurrency.dto.SegmentDTO;
import com.ea.ejbconcurrency.exception.ServerException;
import com.ea.ejbconcurrency.remote.SegmentRemote;
import com.ea.ejbconcurrency.singleton.DSSegmentSingleton;

@Stateless
@Remote(SegmentRemote.class)
public class SegmentDSIsolationLevel implements SegmentRemote {

	private static final Log log = LogFactory.getLog(SegmentDSIsolationLevel.class);

	@EJB(name = "datasourceExample")
	private DSSegmentSingleton segmentSingleton;

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public SegmentDTO addPointToDashboard(String newPoint) throws ServerException {
		return segmentSingleton.addPointToDashboard(newPoint);
	}

	@Override
	public List<SegmentDTO> getAllSegments() throws ServerException {
		try {
			List<SegmentDTO> res = segmentSingleton.getAllSegments();
			return res;
		} catch (Exception e) {
			log.error("General exception in getAllSegments", e);
			throw new ServerException("General exception in getAllSegments", e);
		}
	}
	
	public List<String> getAllPoints() throws ServerException{
		try {
			List<String> res = new ArrayList<String>();
			for (String point : segmentSingleton.getAllPoints()) {
				res.add(point);
			}
			return res;
		} catch (Exception e) {
			log.warn("General exception in getAllPoints", e);
			throw new ServerException("General exception in getAllPoints", e);
		}
	}

	@Override
	public void cleanDashboard() throws ServerException {
		try {
			segmentSingleton.cleanDashboard();
			log.info("Removed all Points and Segments");
		} catch (Exception e) {
			log.error("General exception in cleanDashboard", e);
			throw new ServerException("General exception in cleanDashboard", e);
		}
	}

}
