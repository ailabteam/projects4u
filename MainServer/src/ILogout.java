import java.sql.*;

public interface ILogout {

	void logout(String username) throws SQLException;

	void processing(LazySocket take);
}
