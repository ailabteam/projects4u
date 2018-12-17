
import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;

public class ManageMainServer implements IManageMainServer {

	private static final Config						CONFIG	= Config.getInstance();
	private static final SimpleDateFormat			sdf		= new SimpleDateFormat("dd/MM/yyyy hh:mmaaa");	// ex: 22/12/2018 06:08PM
	private static final StringBuilder				sb		= new StringBuilder();

	private ServerSocket							serverSocket;
	private IMapRoom								mapRoom;
	private ManageChild								manageForRoom;
	private ManageChild								manageForUser;
	private ILogin									login;
	private ILogout									logout;

	private BlockingQueue<Socket>					mainQueue;

	private BlockingQueue<LazySocket>				registryQueue;
	private BlockingQueue<LazySocket>				processingQueue;
	private BlockingQueue<LazySocket>				loginQueue;
	private BlockingQueue<LazySocket>				logoutQueue;

	private ConcurrentHashMap<String, LazySocket>	mapLogins;												// too heavy?

	private final class ListenerRunnable implements Runnable {

		@Override
		public void run() {

			while(true) {
				try {
					Socket socket = serverSocket.accept();
					mainQueue.put(socket);
				} catch(IOException e) {
					e.printStackTrace();
				} catch(InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	private final class LoginRunnable implements Runnable {

		@Override
		public void run() {

			while(true) {
				try {
					System.out.println("ManageMainServer.LoginRunnable.run()");
					login.processing(loginQueue.take());
				} catch(InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private final class LogoutRunnable implements Runnable {

		@Override
		public void run() {

			while(true) {
				try {
					System.out.println("ManageMainServer.LogoutRunnable.run()");
					logout.processing(logoutQueue.take());
				} catch(InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private final class ProcessingRunnable implements Runnable {

		private int		numUserRequest		= 0;
		private int		numRoomRequest		= 0;
		private int		numOtherRequest		= 0;
		private int		numLoginRequest		= 0;
		private int		numLogoutRequest	= 0;

		private long	totalRequest		= 0;

		@Override
		public void run() {

			while(true) {
				try {
					sb.setLength(0);

					Socket socket = mainQueue.take();
					LazySocket lazySocket = new LazySocket(socket);

					String type = getType(lazySocket);

					if(type.equalsIgnoreCase("user")) {

						processingQueue.put(lazySocket);
						System.out.println(sb.append("User request: ").append(++numUserRequest).append(", Wait For Next User!.. ").append(", Total request: ").append(++totalRequest).toString());

					} else if(type.equalsIgnoreCase("room")) {
						registryQueue.put(lazySocket);

						System.out.println(sb.append("Room request: ").append(++numRoomRequest).append(", Wait For Next Room!.. ").append(", Total request: ").append(++totalRequest).toString());

					} else if(type.equalsIgnoreCase("login")) {

						loginQueue.put(lazySocket);
						System.out.println(sb.append("Login request: ").append(++numLoginRequest).append(", Wait For Next Login!.. ").append(", Total request: ").append(++totalRequest).toString());

					} else if(type.equalsIgnoreCase("logout")) {

						logoutQueue.put(lazySocket);
						System.out.println(sb.append("Logout request: ").append(++numLogoutRequest).append(", Wait For Next Logout!.. ").append(", Total request: ").append(++totalRequest).toString());

					} else {

						System.out.println("Other request???? warning! warning!");
						System.out.println(sb.append("Other request: ").append(++numOtherRequest).append(", Total request: ").append(++totalRequest).toString());
						lazySocket.cleanup();
						socket = null;
					}

				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	private final class ProcessingUserRunnable implements Runnable {

		@Override
		public void run() {

			while(true) {
				try {
					System.out.println("ManageMainServer.processingUserRunnable.run()");
					manageForUser.processing(processingQueue.take());
				} catch(InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private final class RegistryRoomRunnable implements Runnable {

		@Override
		public void run() {

			while(true) {
				try {
					System.out.println("ManageMainServer.registryRoomRunnable.run()");
					manageForRoom.processing(registryQueue.take());
				} catch(InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public ManageMainServer(int port) throws IOException {

		super();
		serverSocket = new ServerSocket(port);
		inititalize();
	}

	public ManageMainServer(ServerSocket serverSocket) {

		super();
		this.serverSocket = serverSocket;
		inititalize();
	}

	@Override
	public void processing() {

		System.out.println(sb.append(sdf.format(Calendar.getInstance().getTime())).append("\nServer is starting..."));

		Runnable runnableListener = new ListenerRunnable();
		ProcessingRunnable processingRunnable = new ProcessingRunnable();

		Thread t1 = new Thread(runnableListener);
		Thread t2 = new Thread(processingRunnable);

		t1.start();
		t2.start();

	}

	private String getType(LazySocket lazySocket) throws IOException {

		return lazySocket.getOis().readUTF();

	}

	private void inititalize() {

		mapRoom = MapRoom.getInstance();
		mapLogins = new ConcurrentHashMap<>();

		manageForRoom = new RegistryRoom(mapRoom);
		manageForUser = new ManagePortRoomForUser(mapRoom);

		login = new Login(mapLogins);
		logout = new Logout(mapLogins);

		int numProcessInQueue = CONFIG.getNumProcessInQueue();
		mainQueue = new ArrayBlockingQueue<>(numProcessInQueue * 4);
		processingQueue = new ArrayBlockingQueue<>(numProcessInQueue);
		registryQueue = new ArrayBlockingQueue<>(numProcessInQueue);
		loginQueue = new ArrayBlockingQueue<>(numProcessInQueue);
		logoutQueue = new ArrayBlockingQueue<>(numProcessInQueue);

		int numThreadProcessing = CONFIG.getNumThreadProcessing();

		for(int i = 0; i < numThreadProcessing; i++) {
			System.out.println("Thread " + i + ":");
			new Thread(new ProcessingUserRunnable()).start();
			new Thread(new RegistryRoomRunnable()).start();
			new Thread(new LoginRunnable()).start();
			new Thread(new LogoutRunnable()).start();
		}

	}

}
