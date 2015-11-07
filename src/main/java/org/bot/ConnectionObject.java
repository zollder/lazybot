package org.bot;

import java.sql.Connection;
import java.sql.DriverManager;

public class ConnectionObject
{
	private static Connection connection = null;

	public static Connection getConnection(String dbName)
	{
		String url = "jdbc:mysql://46.165.249.203:3306/" + dbName + "?characterEncoding=UTF-8";
		try
		{
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			connection = DriverManager.getConnection(url, "admin_user", "NkeSr1PPFT");
		}
		catch(Exception e) { e.printStackTrace(); }
		return connection;
	}
}