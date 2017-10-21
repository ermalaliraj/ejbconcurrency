/*package com.ejbconcurrency.dao;

import javax.ejb.embeddable.EJBContainer;
import javax.naming.Context;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ejbconcurrency.ejbimpl.Command;

import junit.framework.TestCase;

//public class RemoteTest extends TestCase {
public class RemoteTest  {
	protected static final transient Log logger = LogFactory.getLog(RemoteTest.class);

	private Context ctx;
	private EJBContainer ejbContainer;
	
	@BeforeClass
	public void setUp() {
		ejbContainer = EJBContainer.createEJBContainer();
		System.out.println("Opening the container");
		ctx = ejbContainer.getContext();
	}

	@AfterClass
	public void tearDown() {
		ejbContainer.close();
		System.out.println("Closing the container");
	}

	//@Test
	public void testDAO() throws NamingException {

		PointDAO pointDAO = (PointDAO) ctx.lookup("java:global/classes/CommentService");
		//assertNotNull(pointDAO);
		pointDAO.insertPoint("A");

	}
	
	@Test
	public void testRemote() throws NamingException {

		Command command = (Command) ctx.lookup("java:global/classes/Command");
		command.addPointToDashboard("A");
		
	}

}
*/