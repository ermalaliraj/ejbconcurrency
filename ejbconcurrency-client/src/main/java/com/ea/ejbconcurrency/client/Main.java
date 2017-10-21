package com.ea.ejbconcurrency.client;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.ea.ejbconcurrency.dto.SegmentDTO;

public class Main {

	private static final Logger log = Logger.getLogger(Main.class);
	private static final int SLEEP_TIME = 1000 / 1000;
	private static final int DEFAULT_NR_CLIENTS = 2;
	private static final int DEFAULT_NR_CALLS_FOR_CLIENT = 10;
	public static final String IMPLEMENTATION_EM = "SegmentEM";
	public static final String IMPLEMENTATION_DS = "SegmentDS";
	public static final String IMPLEMENTATION_SINGLETON = "SegmentDSIsolationLevel";
	public static final String IMPLEMENTATION_SERVICE_DAO = "SegmentEMSafe";
	
	public static String beanImplementation = IMPLEMENTATION_EM;
	public static int nrClients = DEFAULT_NR_CLIENTS;
	public static int nrCallsForClient = DEFAULT_NR_CALLS_FOR_CLIENT;

	public static void main(String[] args) {
		try {
			getValuesFromInput(args);
			log.info("Calling EJB Segment with implementation \""+beanImplementation+"\". Each client will send to the EJB "+nrCallsForClient+" points");
			
			//1. Call EJB to clean data from DB. 
			SegmentClient cleaner = new SegmentClient("CLEANER", beanImplementation);
			cleaner.resetDashboard();
			
			//2. Create two clients in two different threads
			ExecutorService executor = Executors.newFixedThreadPool(2);
			List<String> points1List = createPointsWithPrefix("A");
			Runnable task1 = createClient("CL1", points1List, beanImplementation);
			
			points1List = createPointsWithPrefix("B");
			Runnable task2 = createClient("CL2", points1List, beanImplementation);

			long start = System.currentTimeMillis();
			executor.execute(task1);
			executor.execute(task2);
			executor.shutdown();
			try {
				executor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				log.error("InterruptedException: " + e.getMessage());
			}
			long end = System.currentTimeMillis();
			log.info("Completed in " + (end - start) + " ms, "+((end - start)/1000)+" secs");

			//3. Call EJB to get data from DB
			SegmentClient clientCheck = new SegmentClient("clientCheck", beanImplementation);
			checkData(clientCheck.getAllSegments(), clientCheck.getAllPoints());
		} catch (Exception e) {
			log.error("Exception in main: ", e);
		}
	}

	//Check 1.if same point do not appear in two segments, 2. if any point got lost.
	private static void checkData(List<SegmentDTO> listSegments, List<String> points) {
		List<String> listPoints = getAllPointsFromSegments(listSegments); // A0, A1, A2, A3
		Set<String> set = new HashSet<String>(listPoints);
		if (set.size() != listPoints.size()) {
			log.error("DUPLICATED point found!!!");
		} else {
			log.info("Segments OK. No duplicates found.");
		}
		
		if ((listPoints.size() - points.size()) != nrCallsForClient * 2) { // CoupledPoints - StillFreePoints != TotalCallsWeMadeToEjb
			log.error("POINT missing!!! tot: " + (nrCallsForClient * 2 - (listPoints.size() - points.size())));
		} else {
			log.info("Points OK. All point are coupled.");
		}
	}

	//From list: [A0-A1], [A2-A3],  returns res: [A0], [A1], [A2], [A3]
	private static List<String> getAllPointsFromSegments(List<SegmentDTO> list) {
		List<String> res = new ArrayList<>();
		for (SegmentDTO l : list) {
			res.add(l.getPoint1());
			res.add(l.getPoint2());

		}
		return res;
	}

	//if prefix is "A", returns: "A0", "A1", "A2", "A3", "A4"...
	private static List<String> createPointsWithPrefix(String prefix) {
		List<String> points1List = new ArrayList<>();
		for (int i = 0; i < nrCallsForClient; i++) {
			points1List.add(prefix + i);
		}
		return points1List;
	}

	private static Runnable createClient(String clientName, List<String> points1List, String beanImplementation) {
		SegmentClient client = new SegmentClient(clientName, beanImplementation);
		Runnable task = new MyThread(client, points1List, SLEEP_TIME);
		return task;
	}
	
	private static void getValuesFromInput(String[] args) {
		int implInput = 0;
		try {
			implInput = Integer.valueOf(args[0]);
			nrCallsForClient =  Integer.valueOf(args[1]);
			switch (implInput) {
			case 1:
				beanImplementation = IMPLEMENTATION_EM;
				break;
			case 2:
				beanImplementation = IMPLEMENTATION_DS;
				break;
			case 3:
				beanImplementation = IMPLEMENTATION_SINGLETON;
				break;
			case 4:
				beanImplementation = IMPLEMENTATION_SERVICE_DAO;
				break;

			default:
				beanImplementation = IMPLEMENTATION_EM;
				break;
			}
		} catch (Exception e) {
			mostraHelp();
			System.exit(0);
		}
	}
	
	private static void mostraHelp() {
		StringBuilder sb = new StringBuilder();
		sb.append("Run the client SegmentClient using: java -jar segmentclient.jar implNr nrCallsForEachClient\n ");
		sb.append("- implNr must be an integer:");
		sb.append("\n");
		sb.append("\t1 - for SegmentEM impl which use @PersistenceContext EntityManager");
		sb.append("\n");
		sb.append("\t2 - for SegmentDS impl which use @Datasource");
		sb.append("\n");
		sb.append("\t3 - for SegmentDSIsolationLevel impl which use @Singleton");
		sb.append("\n");
		sb.append("\t4 - for SegmentEMServiceDao impl which use Service-Dao approach and @PersistenceContext is located in the DAO layer");
		sb.append("\n");
		sb.append("- nrCallsForEachClient should be an integer. How many points each client will send to the EJB");
		sb.append("Example of correct call: using: java -jar segmentclient.jar 1 10\n");
		System.out.println(sb.toString());
	}

}

class MyThread implements Runnable {

	private static final Logger log = Logger.getLogger(MyThread.class);
	private long timeSleep;
	private SegmentClient client;
	private List<String> pointList;

	public MyThread(final SegmentClient client, List<String> points, long newtimeSleep) {
		this.client = client;
		this.pointList = points;
		this.timeSleep = newtimeSleep;
	}

	@Override
	public void run() {
		try {
			for (String point : pointList) {
				client.addPointToDashboard(point);
				Thread.sleep(timeSleep);
			}
		} catch (InterruptedException e) {
			log.error("InterruptedException in Client " + (client != null ? client.getName() : "null"), e);
		} catch (Exception e) {
			log.error("General Exception in Client " + (client != null ? client.getName() : "null"), e);
		}
	}

}