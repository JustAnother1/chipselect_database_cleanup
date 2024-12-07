package de.nomagic.database_cleanup.checks;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.nomagic.database_cleanup.DataBaseWrapper;

public class CheckAlternativeUsages extends BasicCheck
{
    private final Logger log = LoggerFactory.getLogger(this.getClass().getName());

    public CheckAlternativeUsages(DataBaseWrapper db)
    {
        super(db);
    }

    @Override
    public String getName()
    {
        return "alternative vendor";
    }

    public boolean execute(boolean dryRun)
    {
        boolean run = true;

        if(true == run)
        {
            run = alternativeVendor(dryRun);
        }

        return run;
    }

    private boolean alternativeVendor(boolean dryRun)
    {
        // get all Vendors
        // find the one with Alternative != 0
        // find all microcontroller that have this vendor as vendor_id
        // set the vendor_id to the alternative.
        log.info("removing links to Vendors that have an alternative...");
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
                        if(false == dryRun)
                        {
                            comparisons++;
                            String sqlFix = "UPDATE microcontroller SET vendor_id = \"" + alternative + "\" WHERE id = " + id;
                            log.trace("In db: device " + id + " changed to vendor " + alternative);
                            log.trace("SQL: " + sqlFix);
                            db.executeUpdate(sqlFix);
                            fixes++;
                        }
                        else
                        {
                            log.info("dry run: would have changed the vendor of  microcontroller {} to {}", id, alternative);
                        }
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
