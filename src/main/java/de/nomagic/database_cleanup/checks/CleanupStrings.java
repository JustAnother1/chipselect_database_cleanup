package de.nomagic.database_cleanup.checks;

import java.sql.ResultSet;
import java.sql.SQLException;

import de.nomagic.database_cleanup.DataBaseWrapper;

public class CleanupStrings extends BasicCheck
{

    public CleanupStrings(boolean verbose, DataBaseWrapper db)
    {
        super(verbose, db);
    }

    public String cleanupString(final String dirty)
    {
        String wet = dirty.trim();
        wet = wet.replaceAll("\n", " ");
        wet = wet.replaceAll("\t", " ");
        wet = wet.replaceAll("\r", " ");
        wet = wet.replaceAll(";", " ");
        wet = wet.replaceAll("\"", " ");
        wet = wet.replaceAll("&", " and ");
        while(true == wet.contains("  "))
        {
            wet = wet.replaceAll("  ", " ");
        }
        String clean = wet.trim();
        return clean;
    }

    public boolean execute()
    {
        boolean run = true;
        if(true == run)
        {
            run = namesInPackage();
        }
        if(true == run)
        {
            run = namesInArchitecture();
        }
        return run;
    }

    private boolean namesInPackage()
    {
        System.out.println("cleaning names in p_package...");
        try
        {
            String sql = "SELECT id, name FROM p_package";
            ResultSet rs = db.executeQuery(sql);
            while(rs.next())
            {
                int id = rs.getInt(1);
                String name = rs.getString(2);
                String cleaned = cleanupString(name);
                comparisons++;
                if(false == cleaned.equals(name))
                {
                    String sqlFix = "UPDATE p_package SET name = \"" + cleaned + "\" WHERE id = " + id;
                    if(true == verbose)
                    {
                        System.out.println("In db: _" + name + "_ cleaned : " + cleaned);
                        System.out.println("SQL: " + sqlFix);
                    }
                    db.executeUpdate(sqlFix);
                    fixes++;
                }
                // else no change -> OK
            }
            return true;
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
        return false;
    }

    private boolean namesInArchitecture()
    {
        System.out.println("cleaning names in p_architecture...");
        try
        {
            String sql = "SELECT id, name FROM p_architecture";
            ResultSet rs = db.executeQuery(sql);
            while(rs.next())
            {
                int id = rs.getInt(1);
                String name = rs.getString(2);
                comparisons++;
                if(null != name)
                {
                    String cleaned = cleanupString(name);
                    if(false == cleaned.equals(name))
                    {
                        String sqlFix = "UPDATE p_architecture SET name = \"" + cleaned + "\" WHERE id = " + id;
                        if(true == verbose)
                        {
                            System.out.println("In db: _" + name + "_ cleaned : " + cleaned);
                            System.out.println("SQL: " + sqlFix);
                        }
                        db.executeUpdate(sqlFix);
                        fixes++;
                    }
                    // else no change -> OK
                }
                // else null == null -> OK
            }
            return true;
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
        return false;
    }

}
