package com.ea.ejbconcurrency.client;

import java.util.List;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.log4j.Logger;

import com.ea.ejbconcurrency.dto.SegmentDTO;
import com.ea.ejbconcurrency.remote.SegmentRemote;

public class SegmentClient {

	private static final Logger log = Logger.getLogger(SegmentClient.class);
	private String name;
	private String beanImplementation; // EJB implementation, CommandEntityManager or CommandDatasource.java

	public SegmentClient(String name, String beanImplementation) {
		this.name = name;
		this.beanImplementation = beanImplementation;
	}

	private SegmentRemote getRemote() {
		SegmentRemote remote = null;
		try {
			Context context = getContextEnvJboss7();
			String lookupString = getLookupString(beanImplementation, SegmentRemote.class.getName());
			remote = (SegmentRemote) context.lookup(lookupString);
		} catch (NamingException e) {
			log.error("Error creating client " + name + ", NamingException: ", e);
		}
		return remote;
	}

	
	public void addPointToDashboard(String pointName) {
		int count = 0;
		boolean success = false;
		while (!success && count < 10) {
			try {
				SegmentRemote remote = getRemote();
				remote.addPointToDashboard(pointName);
				log.info("CLIENT-" + name + " - Added point: " + pointName);
				success = true;
			} catch (Exception e) { // EJBTransactionRolledbackException
				log.error("General Exception (retry for point "+pointName+"): "+ e.getMessage() +" - Cause: "+ e.getCause());
				count++;
			}
		}
	}

	public List<SegmentDTO> getAllSegments() {
		List<SegmentDTO> segments = null;
		try {
			SegmentRemote remote = getRemote();
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
			SegmentRemote remote = getRemote();
			points = remote.getAllPoints();
			log.info("CLIENT-" + name + " - points: " + points);
		} catch (Exception e) {
			log.error("General Exception", e);
		}
		return points;
	}

	public void resetDashboard() {
		try {
			SegmentRemote remote = getRemote();
			remote.cleanDashboard();
			log.info("CLIENT-" + name + ": Dashboard cleaned");
		} catch (Exception e) {
			log.error("General Exception", e);
		}
	}

	// Get string to lookup for Remote interface in Jboss7
	// ejb:<app-name>/<module-name>/<distinct-name>/<bean-name>!<fully-qualified-classname-of-the-remote-interface>
	protected static String getLookupString(String beanName, String fullRemoteName) {
		final String appName = "ejbconcurrency-ear";
		// final String appName = "backupsystem-ear-1.0.0-SNAPSHOT";
		final String moduleName = "ejbconcurrency-server";
		final String distinctName = "";
		// final String beanName = "RuCommand";
		// final String viewClassName = RuCommandRemote.class.getName();
		// ejb:backupsystem-server-ear/backupsystem-server-server//RuCommand!RuComandRemote
		// se specificato "org.jboss.ejb.client.naming"
		String lookupString = "ejb:" + appName + "/" + moduleName + "/" + distinctName + "/" + beanName + "!" + fullRemoteName;
		//String lookupString = "" + appName + "/" + moduleName + "/" + distinctName + "/" + beanName + "!" + fullRemoteName;
		return lookupString;
	}

	protected static Context getContextEnvJboss7() throws NamingException {
		String host = "127.0.0.1";
		String port = "4447";
		try {
			Properties properties = new Properties();
			properties.put(Context.INITIAL_CONTEXT_FACTORY, "org.jboss.naming.remote.client.InitialContextFactory");
			properties.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming"); // senza questa riga, lookup: "" + appName + "/" + moduleName + "/" + distinctName + "/" + beanName + "!" + fullRemoteName;
			properties.put(Context.PROVIDER_URL, "remote://" + host + ":" + port);
			properties.put("jboss.naming.client.ejb.context", "true");
//			properties.put(Context.SECURITY_PRINCIPAL, "testuser");
//			properties.put(Context.SECURITY_CREDENTIALS, "testpwd");
			// deactivate authentication
			 properties.put("jboss.naming.client.connect.options.org.xnio.Options.SASL_POLICY_NOPLAINTEXT","false");
			 properties.put("remote.connection.default.connect.options.org.xnio.Options.SASL_POLICY_NOANONYMOUS","false");
			Context context = new InitialContext(properties);
			return context;
		} catch (NamingException e) {
			log.error("Error creating InitialContext", e);
			throw e;
		}
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
