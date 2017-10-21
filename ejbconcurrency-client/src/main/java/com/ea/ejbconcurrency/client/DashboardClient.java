package com.ea.ejbconcurrency.client;

import java.util.List;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.log4j.Logger;

import com.ea.ejbconcurrency.dto.SegmentDTO;
import com.ea.ejbconcurrency.remote.DashboardRemote;

public class DashboardClient {

	private static final Logger log = Logger.getLogger(DashboardClient.class);
	private String name;
	private String beanImplementation; // EJB implementation, CommandEntityManager or CommandDatasource.java
	private int nrCallsForClient;
	private DashboardRemote remote;

	public DashboardClient(String name, String beanImplementation, int nrCallsForClient) {
		this.name = name;
		this.beanImplementation = beanImplementation;
		this.nrCallsForClient = nrCallsForClient;
		this.remote = getRemote();
	}

	private DashboardRemote getRemote() {
		try {
			Properties properties = new Properties();
			// deactivate authentication, not enough only .properties values
			properties.put("jboss.naming.client.connect.options.org.xnio.Options.SASL_POLICY_NOPLAINTEXT", "false");
			properties.put("remote.connection.default.connect.options.org.xnio.Options.SASL_POLICY_NOANONYMOUS", "false");
			Context context = new InitialContext(properties);
			String lookupString = getLookupString(beanImplementation, DashboardRemote.class.getName());
			remote = (DashboardRemote) context.lookup(lookupString);
		} catch (NamingException e) {
			log.error("Error creating client " + name + ", NamingException: ", e);
		}
		return remote;
	}

	// Get string to lookup for Remote interface in Jboss7
	// ejb:<app-name>/<module-name>/<distinct-name>/<bean-name>!<fully-qualified-classname-of-the-remote-interface>
	// no prefix "ejb:" if "org.jboss.ejb.client.naming" is not specified
	protected static String getLookupString(String beanName, String fullRemoteName) {
		final String appName = "ejbconcurrency-ear";
		final String moduleName = "ejbconcurrency-server";
		final String distinctName = "";
		String lookupString = "ejb:" + appName + "/" + moduleName + "/" + distinctName + "/" + beanName + "!" + fullRemoteName;
		// String lookupString = "" + appName + "/" + moduleName + "/" + distinctName + "/" + beanName + "!" + fullRemoteName;
		return lookupString;
	}
	
	public static void main(String[] args) {
		final int nrCallsForClient = 1;
		DashboardClient client = new DashboardClient("A", Main.IMPLEMENTATION_EM, nrCallsForClient);
		client.resetDashboard();
		client.send();
	    Main.checkData(client.getAllSegments(), client.getAllPoints(), 1, nrCallsForClient);
	}

	public void send() {
		for (int i = 0; i < nrCallsForClient; i++) {
			addPointToDashboard(name + i);
		}
	}

	private void addPointToDashboard(String pointName) {
		int count = 0;
		boolean success = false;
		while (!success && count < 10) {
			try {
				remote.addPointToDashboard(pointName);
				log.info("CLIENT-" + name + " - Added point: " + pointName);
				success = true;
			} catch (Exception e) { // EJBTransactionRolledbackException
				log.error("General Exception (retry for point " + pointName + "): " + e.getMessage() + " - Cause: " + e.getCause());
				count++;
			}
		}
	}

	public List<SegmentDTO> getAllSegments() {
		List<SegmentDTO> segments = null;
		try {
			segments = remote.getAllSegments();
			log.info("CLIENT-" + name + " - Segments: " + segments);
		} catch (Exception e) {
			log.error("General Exception", e);
		}
		return segments;
	}

	public List<String> getAllPoints() {
		List<String> points = null;
		try {
			points = remote.getAllPoints();
			log.info("CLIENT-" + name + " - points: " + points);
		} catch (Exception e) {
			log.error("General Exception", e);
		}
		return points;
	}

	public void resetDashboard() {
		try {
			remote.cleanDashboard();
			log.info("CLIENT-" + name + ": Dashboard cleaned");
		} catch (Exception e) {
			log.error("General Exception", e);
		}
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
