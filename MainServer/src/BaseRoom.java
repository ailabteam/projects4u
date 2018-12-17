
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class BaseRoom {

	protected String					roomId;
	protected Map<ChatUser, LazySocket>	mapUser;
	protected ServerSocket				serverSocket;

	protected BaseRoom(String roomId, int initialCapacity) {

		super();
		this.roomId = roomId;
		System.out.println("---------- " + roomId + " ----------");
		if(initialCapacity <= 0) {
			initialCapacity = 20;
		}
		mapUser = new ConcurrentHashMap<>(initialCapacity);
		try {
			serverSocket = new ServerSocket(0);// auto port
			System.out.println(serverSocket.getLocalPort());
		} catch(IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	protected abstract void processing();

}
