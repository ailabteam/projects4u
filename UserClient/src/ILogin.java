import java.io.*;

public interface ILogin {

	LazySocket getLazySocket();

	ChatUser login(String username, String pass) throws IOException, ClassNotFoundException;

}
