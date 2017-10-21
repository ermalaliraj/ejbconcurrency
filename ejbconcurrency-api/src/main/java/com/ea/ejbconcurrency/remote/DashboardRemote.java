package com.ea.ejbconcurrency.remote;

import java.util.List;

import javax.ejb.Remote;

import com.ea.ejbconcurrency.dto.SegmentDTO;
import com.ea.ejbconcurrency.exception.ServerException;

@Remote
public interface DashboardRemote {
	
	public SegmentDTO addPointToDashboard(String name) throws ServerException;
	public List<SegmentDTO> getAllSegments() throws ServerException;
	public List<String> getAllPoints() throws ServerException;
	public void cleanDashboard() throws ServerException;

}
