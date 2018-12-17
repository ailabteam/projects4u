import java.io.*;
import java.security.*;
import java.sql.*;
import java.util.concurrent.*;

public class Login implements ILogin {

	private Connection									connection;
	private CallableStatement							statement;
	private ResultSet									resultSet;
	private final ConcurrentHashMap<String, LazySocket>	mapLogins;
	private final StringBuilder							sb;

	public Login(ConcurrentHashMap<String, LazySocket> mapLogins) {

		super();
		this.mapLogins = mapLogins;
		IManageDB manageDB = new ManageDB();
		sb = new StringBuilder();

		try {
			connection = manageDB.getConnection();
		} catch(SQLException e) {
			// TODO Auto-generated catch block

			e.printStackTrace();
			System.exit(1);
		}
	}

	@Override
	public ChatUser login(String userName, String pass) throws Exception {

		System.out.println(pass);
		statement = connection.prepareCall("{call get_salt(?)}");
		statement.setString(1, userName);
		resultSet = statement.executeQuery();
		String salt = null;
		if(resultSet.next()) {
			salt = resultSet.getString("salt");
		}

		pass = hashPassword(pass + salt);
		System.out.println(pass);
		statement = connection.prepareCall("{call logout_1(?,?)}");
		statement.setString(1, userName);
		statement.setString(2, pass);

		int x = statement.executeUpdate();
		if(x > 0) {
			try {
				LazySocket lazySocket = mapLogins.remove(userName);
				if(lazySocket != null) {
					sendDisconnected(lazySocket);
				}
			} catch(Exception e) {
				System.out.println("user " + userName + " is offline!!");
			}
		}

		System.out.println(userName + " Login.login()");
		statement = connection.prepareCall("{call login(?,?)}");
		statement.setString(1, userName);
		statement.setString(2, pass);

		resultSet = statement.executeQuery();
		if(resultSet.next()) {
			ChatUser user = new ChatUser();
			user.setUsername(resultSet.getString("username"));
			user.setName(resultSet.getString("name"));
			user.setAddress(resultSet.getString("address"));
			user.setEmail(resultSet.getString("email"));
			user.setPhone(resultSet.getString("phone"));

			return user;
		}
		System.out.println(userName + " -> Login Failed!!!!!!!!!!");
		return null;
	}

	@Override
	public void processing(LazySocket lazySocket) {

		try {

			ObjectInputStream ois = lazySocket.getOis();
			String username = ois.readUTF();
			String pass = ois.readUTF();

			ObjectOutputStream oos = lazySocket.getOos();

			ChatUser user = login(username, pass);
			if(user == null) {
				oos.writeUTF("fail");
				oos.flush();
			} else {
				oos.writeUTF("success");
				oos.flush();
				sendUserToClient(lazySocket, user);
				mapLogins.put(username, lazySocket);
				System.out.println("Current connect: " + mapLogins);
			}
		} catch(IOException e) {
			e.printStackTrace();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void sendUserToClient(LazySocket lazySocket, ChatUser chatUser) throws IOException, ClassNotFoundException {

		System.out.println("Login.sendUserToClient()");
		ObjectOutputStream oos = lazySocket.getOos();
		oos.writeObject(chatUser);
		oos.flush();

	}

	private String hashPassword(String passwordToHash) {

		sb.setLength(0);
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(passwordToHash.getBytes());
			byte[] bytes = md.digest();
			for(int i = 0; i < bytes.length; i++) {
				sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
			}
		} catch(NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return sb.toString();
	}

	private void sendDisconnected(LazySocket lazySocket) throws IOException {

		System.out.println("Login.sendDisconnect()");
		ObjectOutputStream oos = lazySocket.getOos();
		oos.writeUTF("disconnected");
		oos.flush();
	}

}
