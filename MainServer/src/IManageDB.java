import java.sql.*;

public interface IManageDB {

	void closeConnect() throws Exception;

	Connection getConnection() throws SQLException;

}
