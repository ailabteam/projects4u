import java.io.*;
import java.net.*;

public class Login implements ILogin {

	public static final ConfigClient	CONFIG	= ConfigClient.getInstance();
	private String						hostMainServer;
	private int							portMainServer;
	private LazySocket					lazySocket;

	/**
	 *
	 */
	public Login() {

		super();
		hostMainServer = CONFIG.getHostMainServer();
		portMainServer = CONFIG.getPortMainServer();

	}

	@Override
	public LazySocket getLazySocket() {

		return lazySocket;
	}

	@Override
	public ChatUser login(String username, String pass) throws IOException, ClassNotFoundException {

		Socket socket = new Socket(hostMainServer, portMainServer);
		lazySocket = new LazySocket(socket);
		System.out.println(username + " Login.login()");
		ObjectOutputStream oos = lazySocket.getOos();
		oos.writeUTF("login");
		oos.flush();
		oos.writeUTF(username);
		oos.flush();
		oos.writeUTF(pass);
		oos.flush();
		System.out.println("sent");

		ObjectInputStream ois = lazySocket.getOis();
		String rs = ois.readUTF();

		if(rs.equalsIgnoreCase("fail")) { return null; }
		return (ChatUser)ois.readObject();
	}
}
