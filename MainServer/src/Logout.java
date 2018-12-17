import java.io.*;
import java.sql.*;
import java.util.concurrent.*;

public class Logout implements ILogout {

	private Connection									connection;
	private CallableStatement							statement;
	private final ConcurrentHashMap<String, LazySocket>	mapLogins;

	public Logout(ConcurrentHashMap<String, LazySocket> mapLogins) {

		super();
		this.mapLogins = mapLogins;
		IManageDB manageDB = new ManageDB();

		try {
			connection = manageDB.getConnection();
		} catch(SQLException e) {
			// TODO Auto-generated catch block

			e.printStackTrace();
			System.exit(1);
		}
	}

	@Override
	public void logout(String userName) throws SQLException {

		System.out.println("Logout.logout()");
		mapLogins.remove(userName);

		statement = connection.prepareCall("{call logout(?)}");
		statement.setString(1, userName);

		statement.executeUpdate();// don't care result
	}

	@Override
	public void processing(LazySocket lazySocket) {

		try {

			ObjectInputStream ois = lazySocket.getOis();
			String username = ois.readUTF();

			System.out.println(username);
			logout(username);

		} catch(IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch(Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
