package com.ea.ejbconcurrency.client;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;

import org.apache.log4j.Logger;

import com.ea.ejbconcurrency.dto.SegmentDTO;

public class Main {

	private static final Logger log = Logger.getLogger(Main.class);
	private static final int DEFAULT_NR_CLIENTS = 2;
	private static final int DEFAULT_NR_CALLS_FOR_CLIENT = 10;
	public static final String IMPLEMENTATION_EM = "DashboardEM";
	public static final String IMPLEMENTATION_DS = "DashboardDS";
	public static final String IMPLEMENTATION_DS_TX_BEAN = "DashboardDSTxBean";
	public static final String IMPLEMENTATION_DS_SELECT_UPDATE = "DashboardDSSelectForUpdate";
	public static final String IMPLEMENTATION_SINGLETON = "DashboardDSUseSingleton";
	public static final String IMPLEMENTATION_SERVICE_DAO = "DashboardEMServiceDao";
	
	public static String beanImplementation = IMPLEMENTATION_EM;
	public static int nrClients = DEFAULT_NR_CLIENTS;
	public static int nrCallsForClient = DEFAULT_NR_CALLS_FOR_CLIENT;

	public static void main(String[] args) {
		try {
			getValuesFromInput(args);
			log.info("Calling EJB Segment with implementation \""+beanImplementation+"\". Created "+nrClients+" clients, each of them will send to the EJB "+nrCallsForClient+" points");
			log.info("We expect at the end of the test: "+(nrCallsForClient*nrClients)/2 + " Segments and "+(nrCallsForClient*nrClients)%2 + " Points");
			
			//1. Call EJB to clean data from DB. 
			DashboardClient cleaner = new DashboardClient("CLEANER", beanImplementation, nrCallsForClient);
			cleaner.resetDashboard();
			
			//2. Create two clients in run them in two different threads
			ExecutorService executor = Executors.newFixedThreadPool(2);
			DashboardClient client = new DashboardClient("A", beanImplementation, nrCallsForClient);
			Runnable task1 = new MyThread(client);
			DashboardClient client2 = new DashboardClient("B", beanImplementation, nrCallsForClient);
			Runnable task2 = new MyThread(client2);

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
			DashboardClient clientCheck = new DashboardClient("clientCheck", beanImplementation, nrCallsForClient);
			checkData(clientCheck.getAllSegments(), clientCheck.getAllPoints(), nrClients, nrCallsForClient);
		} catch (Exception e) {
			log.error("Exception in main: ", e);
		}
	}

	//Check 1.if same point do not appear in two segments, 2. if any point got lost.
	static void checkData(List<SegmentDTO> segments, List<String> points, int nrClients, int nrCallsForClient) {
		log.info("nrClients: " + nrClients + ", nrCallsForClient: "+nrCallsForClient+", nr segments created: "+segments.size()+", nr single points: "+points.size());
		
		List<String> listPoints = getAllPointsFromSegments(segments); // A0, A1, A2, A3
		Set<String> set = new HashSet<String>(listPoints);
		if (set.size() != listPoints.size()) {
			log.error("DUPLICATED point found!!!");
		} else {
			log.info("Segments OK. No duplicates found.");
		}
		
		if ((listPoints.size() - points.size()) != nrCallsForClient * nrClients) { // CoupledPoints - StillFreePoints != TotalCallsWeMadeToEjb
			log.error("POINT missing!!! tot: " + (nrCallsForClient * nrClients - (listPoints.size() - points.size())));
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
				beanImplementation = IMPLEMENTATION_DS_TX_BEAN;
				break;
			case 4:
				beanImplementation = IMPLEMENTATION_DS_SELECT_UPDATE;
				break;
			case 5:
				beanImplementation = IMPLEMENTATION_SINGLETON;
				break;
			case 6:
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
		sb.append("Run the client DashboardClient using: java -jar dashboardclient.jar implNr nrCallsForEachClient\n ");
		sb.append("- implNr must be an integer:");
		sb.append("\n");
		sb.append("\t1 - for DashboardEM impl which use @PersistenceContext EntityManager");
		sb.append("\n");
		sb.append("\t2 - for DashboardDS impl which use @Datasource");
		sb.append("\n");
		sb.append("\t3 - for DashboardDSTxBean impl which use @Datasource in @TransactionManagement(BEAN) and conn.autocommit(false)");
		sb.append("\n");
		sb.append("\t4 - for DashboardDSSelectForUpdate impl which use SELECT FOR UPDATE");
		sb.append("\n");
		sb.append("\t5 - for DashboardDSUseSingleton impl which use @Singleton");
		sb.append("\n");
		sb.append("\t6 - for DashboardEMServiceDao impl which use Service-Dao approach and @PersistenceContext is located in the DAO layer");
		sb.append("\n");
		sb.append("- nrCallsForEachClient should be an integer. How many points each client will send to the EJB");
		sb.append("Example of correct call: using: java -jar dashboardclient.jar 1 10\n");
		System.out.println(sb.toString());
	}

}

class MyThread implements Runnable {
	private static final Logger log = Logger.getLogger(MyThread.class);
	private DashboardClient client;

	public MyThread(DashboardClient client) {
		this.client = client;
	}

	@Override
	public void run() {
		try {
			client.send();
		} catch (Exception e) {
			log.error("General Exception in Thread of Client " + client, e);
		}
	}

}