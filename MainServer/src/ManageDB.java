import java.sql.*;
import java.sql.Connection;
import java.util.*;

import com.mysql.jdbc.Driver;

public class ManageDB implements IManageDB {

	private static final Config	CONFIG				= Config.getInstance();
	private Connection			connection;

	private String				hostmysqldatabase	= CONFIG.getHostMysqldatabase();
	private int					portmysqldatabase	= CONFIG.getPortmysqldatabase();
	private String				mysqldatabase		= CONFIG.getMysqlDatabase();
	private String				username			= CONFIG.getMysqlUser();
	private String				password			= CONFIG.getMysqlPassword();

	@Override
	public void closeConnect() throws Exception {

		connection.close();
	}

	@Override
	public Connection getConnection() throws SQLException {

		System.out.println("ManageDB.getConnection()");
		String strConnect = "jdbc:mysql://" + hostmysqldatabase + ":" + portmysqldatabase + "/" + mysqldatabase;
		Properties pro = new Properties();
		pro.put("user", username);
		pro.put("password", password);
//		pro.setProperty("useSSL", "false");
		pro.setProperty("autoReconnect", "true");

		Driver driver = new Driver();
		connection = driver.connect(strConnect, pro);
		System.out.println("Connect DB success!");
		return connection;
	}

}
