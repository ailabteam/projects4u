import java.io.*;

public interface ILogin {

	ChatUser login(String userName, String pass) throws Exception;

	void processing(LazySocket lazySocket);

	void sendUserToClient(LazySocket lazySocket, ChatUser chatUser) throws IOException, ClassNotFoundException;

}
