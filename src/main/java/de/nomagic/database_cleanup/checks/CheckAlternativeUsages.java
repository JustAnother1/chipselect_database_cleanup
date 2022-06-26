package de.nomagic.database_cleanup.checks;

import java.sql.ResultSet;
import java.sql.SQLException;

import de.nomagic.database_cleanup.DataBaseWrapper;

public class CheckAlternativeUsages extends BasicCheck
{

    public CheckAlternativeUsages(boolean verbose, DataBaseWrapper db)
    {
        super(verbose, db);
    }

    public boolean execute()
    {
        boolean run = true;

        if(true == run)
        {
            run = alternativeVendor();
        }

        return run;
    }

    private boolean alternativeVendor()
    {
        // get all Vendors
        // find the one with Alternative != 0
        // find all microcontroller that have this vendor as vendor_id
        // set the vendor_id to the alternative.
        System.out.println("removing links to Vendors that have an alternative...");
        try
        {
            String sql = "SELECT id, alternative FROM p_vendor";
            ResultSet rs = db.executeQuery(sql);
            while(rs.next())
            {
                int vendorId = rs.getInt(1);
                int alternative = rs.getInt(2);
                comparisons++;
                if(0  != alternative)
                {
                    String sqlFind = "SELECT id FROM microcontroller WHERE vendor_id =" + vendorId;
                    ResultSet dev_rs = db.executeQuery(sqlFind);
                    while(dev_rs.next())
                    {
                        int id = dev_rs.getInt(1);
                        comparisons++;
                        String sqlFix = "UPDATE microcontroller SET vendor_id = \"" + alternative + "\" WHERE id = " + id;
                        if(true == verbose)
                        {
                            System.out.println("In db: device " + id + " changed to vendor " + alternative);
                            System.out.println("SQL: " + sqlFix);
                        }
                        db.executeUpdate(sqlFix);
                        fixes++;
                    }
                }
                // else -> OK
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
