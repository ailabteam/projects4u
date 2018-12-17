
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.sql.*;
import java.text.*;
import java.util.*;
import java.util.Map.*;
import java.util.concurrent.*;

public class RoomServer extends BaseRoom implements IRoom {

	private static final Config				CONFIG	= Config.getInstance();
	private static final SimpleDateFormat	sdf		= new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");
	private boolean							isStarting;

	private BlockingQueue<Socket>			queue;

	private Connection						connection;
	private String							hostMainServer;
	private int								portMainServer;

	private Queue<ChatUser>					listRemove;
	private CustomAddress					address;

	private int								numLoadHistory;

	private StringBuilder					sb;

	private final class RunnableListener implements Runnable {

		@Override
		public void run() {

			while(isStarting) {
				try {
					Socket socket = serverSocket.accept();
					queue.put(socket);
					System.out.println("in queue: " + socket);
				} catch(IOException e) {
					e.printStackTrace();
				} catch(InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private final class RunnableProcessing implements Runnable {

		@Override
		public void run() {

			while(isStarting) {
				try {
					LazySocket lazySocket;
					ObjectInputStream ois;
					String query;
					try {
						Socket socket = queue.take();
						System.out.println("take(): " + socket);
						lazySocket = new LazySocket(socket);
						ois = lazySocket.getOis();
						query = ois.readUTF();
					} catch(IOException e) {
						System.out.println("Main checked!");
						continue;
					}

					if(query.equalsIgnoreCase("gotoRoom")) {
						ChatUser us = reiceiveChatUser(lazySocket);
						if(us != null) {
							ObjectOutputStream oos = lazySocket.getOos();
							if(!addUser(us, lazySocket)) {
								oos.writeUTF("full");
								oos.flush();
							} else {
								oos.writeUTF("success");
								oos.flush();

								ArrayList<Message> messages = loadHistoryFromServerDB(us, numLoadHistory, 0);
								sendHistoryToClient(messages, lazySocket);
								processingInRoom(us, lazySocket);
							}
						}
					} else {
						lazySocket.cleanup();
						lazySocket = null;
					}
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		}

	}

	public RoomServer(String id, int initialCapacity) {

		super(id, initialCapacity);

		try {
			initialize();
			resgistryRoomToMainServer(hostMainServer, portMainServer);
			System.out.println("Resgistry Sucess!");
			processing();
		} catch(IOException | SQLException e) {
			System.err.println("Can't Connect To Server: " + e.getMessage());
			shutdownRoom();
		}
	}

	@Override
	public boolean addUser(ChatUser chatUser, LazySocket lazySocket) {

		try {
			mapUser.put(chatUser, lazySocket);
			return true;
		} catch(Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public CustomAddress getAddressRoom() {

		return address;
	}

	@Override
	public String getIDRoom() {

		return roomId;
	}

	public RoomServer getRoomServer() {

		return this;
	}

	@Override
	public ArrayList<Message> loadHistoryFromServerDB(ChatUser u, int numMsg, int startId) throws Exception {

		System.out.println("RoomServer.loadHistoryFromServerDB()");
		CallableStatement statement = connection.prepareCall("{call loadhistory(?,?,?,?)}");
		statement.setString(1, u.getUsername());
		statement.setInt(2, numMsg);
		statement.setString(3, roomId);
		statement.setInt(4, startId);

		ResultSet resultSet = statement.executeQuery();
		ArrayList<Message> rs = new ArrayList<>();
		while(resultSet.next()) {

			String username = resultSet.getString("username");
			String content = URLDecoder.decode(resultSet.getString("content"), "UTF-8");
			String dateSend = resultSet.getString("time_send");

			try {
				Message message = new Message(new ChatUser(username), content, sdf.parse(dateSend));
				rs.add(message);
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		return rs;
	}

	@Override
	public void loadMore(ChatUser us, LazySocket lazySocket, ObjectInputStream ois, ObjectOutputStream oos) throws NumberFormatException, IOException {

		int offset = Integer.parseInt(ois.readUTF());
		ArrayList<Message> messages = null;
		try {
			messages = loadHistoryFromServerDB(us, numLoadHistory, offset);
		} catch(Exception e) {
			e.printStackTrace();
		}
		oos.writeUTF("loadmore");
		oos.flush();
		sendHistoryToClient(messages, lazySocket);
	}

	@Override
	public void logout(LazySocket lazySocket) {

		ChatUser user = getKeyByValue(mapUser, lazySocket);
		if(user == null) { return; }
		try {
			removeUser(user);
			Socket socket = new Socket(hostMainServer, portMainServer);
			LazySocket lazySocketMain = new LazySocket(socket);

			ObjectOutputStream oos = lazySocketMain.getOos();
			oos.writeUTF("logout");
			oos.writeUTF(user.getUsername());
			oos.flush();
			lazySocketMain.cleanup();
			lazySocket.cleanup();

		} catch(IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	public synchronized void processingInRoom(ChatUser us, LazySocket lazySocket) {

		System.out.println("RoomServer.receiveMessageFromClient()");

		Thread thread = new Thread(() -> {

			try {
				ObjectInputStream ois = lazySocket.getOis();
				ObjectOutputStream oos = lazySocket.getOos();

				while(true) {

					String key = ois.readUTF();

					if("chat".equalsIgnoreCase(key)) {
						Message message = readMessage(lazySocket);
						message.setContent(URLDecoder.decode(message.getContent(), "UTF-8"));

						sendMessageToAllClient(message);// DE YEN DAY
						saveMessageToDatabase(message);
					} else if("sendfile".equalsIgnoreCase(key)) {
						receiveFileFromClient(lazySocket);
					} else if("loadMore".equalsIgnoreCase(key)) {
						loadMore(us, lazySocket, ois, oos);
					} else {
						System.out.println("Other request: \"" + key + "\"");
					}
				}
			} catch(EOFException e) {
				System.out.println("Disconnected Client!");
			} catch(IOException e) {
				if(e.getMessage().equalsIgnoreCase("Connection reset")) {
					logout(lazySocket);
					System.out.println("user logout!");
				} else {
					e.printStackTrace();
				}
			} catch(SQLException e) {
				e.printStackTrace();
			} finally {
				lazySocket.cleanup();
			}
		});
		thread.start();
	}

	@Override
	public void receiveFileFromClient(LazySocket lazySocket) throws IOException {

		ObjectInputStream ois = lazySocket.getOis();
		String fileName = ois.readUTF();

		String rootPath = "E:\\";
		File file = new File(rootPath + fileName);
		int c = 0;
		while(file.exists()) {
			c++;
			fileName = fileName + c;
			file = new File(rootPath + fileName);
		}

		System.out.println("received file: " + fileName + "from client, path in server room: " + file.getAbsolutePath());
		Files.copy(ois, file.toPath());
		// TODO hard code, hard edit!...

	}

	@Override
	public void removeUser(ChatUser user) {

		System.out.println("RoomServer.removeUser()");
		mapUser.remove(user);

	}

	@Override
	public void resgistryRoomToMainServer(String mainServerHost, int mainServerPort) throws IOException {

		System.out.println("RoomServer.resgistryRoomToMainServer()");
		System.out.println(mainServerHost + "->" + mainServerPort);

		Socket socket = new Socket(mainServerHost, mainServerPort);
		LazySocket lazySocket = new LazySocket(socket);

		ObjectOutputStream oos = lazySocket.getOos();
		oos.writeUTF("room");
		oos.writeUTF(roomId);
		oos.flush();
		oos.writeObject(getAddressRoom());
		oos.flush();

		ObjectInputStream ois = lazySocket.getOis();
		String result = ois.readUTF();

		if(!result.equalsIgnoreCase("success")) { throw new IOException("Resgistry failed!"); }

	}

	@Override
	public void saveMessageToDatabase(Message m) throws SQLException, UnsupportedEncodingException {

		System.out.println("RoomServer.saveMessageToDatabase()");
		CallableStatement statement = connection.prepareCall("{call saveMessage(?,?,?,?,?)}");
		statement.setString(1, m.getOwner().getUsername());
		String content = m.getContent();

		sb.setLength(0);
		sb.append("[_");
		Set<ChatUser> users = mapUser.keySet();
		for(ChatUser u : users) {
			sb.append(u.getUsername()).append("_");
		}
		sb.append("]");

		statement.setString(2, URLEncoder.encode(content, "UTF-8"));
		statement.setString(3, sdf.format(m.getDateSend()));
		statement.setString(4, roomId);
		statement.setString(5, sb.toString());

		statement.executeUpdate();

	}

	@Override
	public void sendHistoryToClient(ArrayList<Message> messages, LazySocket lazySocket) throws IOException {

		System.out.println("RoomServer.sendHistoryToClient()");
		ObjectOutputStream oos = lazySocket.getOos();
		oos.writeUTF("history");
		oos.flush();
		oos.writeObject(messages);
		oos.flush();
	}

	@Override
	public void sendMessageToAllClient(Message m) throws IOException {

		for(Entry<ChatUser, LazySocket> entry : mapUser.entrySet()) {
			LazySocket lazySocket = entry.getValue();
			if(lazySocket == null) {
				listRemove.add(entry.getKey());
				continue;
			}
			Socket socket = lazySocket.getSocket();
			if(socket.isInputShutdown() || socket.isOutputShutdown() || socket.isClosed()) {
				listRemove.add(entry.getKey());
				logout(lazySocket);
				continue;
			}
			ObjectOutputStream oos = lazySocket.getOos();
			oos.writeUTF("chat");
			oos.flush();
			oos.writeObject(m);
			oos.flush();
		}
		while(!listRemove.isEmpty()) {
			ChatUser key = listRemove.remove();
			removeUser(key);
		}
	}

	@Override
	public void shutdownRoom() {

		isStarting = false;
		for(LazySocket c : mapUser.values()) {
			c.cleanup();
		}
		try {
			connection.close();
		} catch(SQLException e) {
			e.printStackTrace();
		}
		System.out.println("Shutdown Room Server!");
	}

	@Override
	protected void processing() {

		isStarting = true;

		Runnable runnableListener = new RunnableListener();
		Runnable runnableProcessing = new RunnableProcessing();

		Thread t1 = new Thread(runnableListener);
		Thread t2 = new Thread(runnableProcessing);

		t1.start();
		t2.start();

	}

	// from StackOverFlow
	private <T, E> T getKeyByValue(Map<T, E> map, E value) {

		for(Entry<T, E> entry : map.entrySet()) {
			if(Objects.equals(value, entry.getValue())) { return entry.getKey(); }
		}
		return null;
	}

	private void initialize() throws SQLException {

		IManageDB iManageDB = new ManageDB();
		connection = iManageDB.getConnection();
		String host = ""; // InetAddress.getLocalHost().getHostAddress();
		host = CONFIG.getHostRoomserver();// "192.168.1.21";
		int port = serverSocket.getLocalPort();

		address = new CustomAddress(host, port);
		address.setHost(host);
		address.setPort(port);
		System.out.println(address);
		sb = new StringBuilder();

		isStarting = false;
		queue = new ArrayBlockingQueue<>(CONFIG.getNumProcessInQueue());
		listRemove = new LinkedList<>();

		hostMainServer = CONFIG.getHostMainServer();
		portMainServer = CONFIG.getPortMainServer();
		numLoadHistory = CONFIG.getNumLoadHistory();
	}

	private Message readMessage(LazySocket lazySocket) {

		try {
			return (Message)lazySocket.getOis().readObject();
		} catch(ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private ChatUser reiceiveChatUser(LazySocket lazySocket) {

		try {
			ObjectInputStream ois = lazySocket.getOis();
			Object obj = ois.readObject();
			return (ChatUser)obj;
		} catch(ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}
		return null;
	}

}
