package de.nomagic.database_cleanup;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DataBaseWrapper
{
    private boolean connected = false;
    private Connection conn;
    private Statement stmt;

    public DataBaseWrapper()
    {
        // TODO Auto-generated constructor stub
    }

    public void commit()
    {
        try
        {
            conn.commit();
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public boolean connectToDataBase(String dbLocation, String dbUser, String dbPassword)
    {
        try
        {
            conn = DriverManager.getConnection(dbLocation, dbUser, dbPassword);
            stmt = conn.createStatement();
            connected = true;
            return true;
        }
        catch (SQLException e)
        {
            e.printStackTrace();
            connected = false;
        }
        return false;
    }

    public void closeDataBaseConnection()
    {
        if(true == connected)
        {
            if(null != stmt)
            {
                try
                {
                    stmt.close();
                }
                catch (SQLException e)
                {
                    e.printStackTrace();
                }
            }

            if(null != conn)
            {
                try
                {
                    conn.commit();
                    conn.close();
                }
                catch (SQLException e)
                {
                    e.printStackTrace();
                }
            }

            connected = false;
        }
        //else : already closed
    }

    public ResultSet executeQuery(String sql) throws SQLException
    {
        return stmt.executeQuery(sql);
    }

    public void executeUpdate(String sql) throws SQLException
    {
        stmt.executeUpdate(sql);
    }



}
