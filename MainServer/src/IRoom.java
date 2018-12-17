
import java.io.*;
import java.sql.*;
import java.util.*;

/**
 * synchroninzed
 */
public interface IRoom {

	boolean addUser(ChatUser chatUser, LazySocket lazySocket);

	CustomAddress getAddressRoom();

	String getIDRoom();

	ArrayList<Message> loadHistoryFromServerDB(ChatUser u, int numMsg, int startId) throws Exception;

	void loadMore(ChatUser us, LazySocket lazySocket, ObjectInputStream ois, ObjectOutputStream oos) throws NumberFormatException, IOException;

	void logout(LazySocket lazySocket);

	/**
	 * never return null
	 *
	 * @throws Exception
	 */

	void processingInRoom(ChatUser us, LazySocket lazySocket);// new thread{ while(true)}

	void receiveFileFromClient(LazySocket lazySocket) throws IOException;

	void removeUser(ChatUser user);

	void resgistryRoomToMainServer(String host, int port) throws IOException;

	void saveMessageToDatabase(Message message) throws SQLException, UnsupportedEncodingException;

	void sendHistoryToClient(ArrayList<Message> messages, LazySocket lazySocket) throws IOException;

	void sendMessageToAllClient(Message m) throws IOException;

	void shutdownRoom();
}
