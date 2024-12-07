package de.nomagic.database_cleanup.checks;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.nomagic.database_cleanup.DataBaseWrapper;
import de.nomagic.database_cleanup.checks.helpers.AddressBlock;

public class CheckAddressBlocks extends BasicCheck
{
    private final Logger log = LoggerFactory.getLogger(this.getClass().getName());

    public CheckAddressBlocks(DataBaseWrapper db)
    {
        super(db);
    }

    @Override
    public String getName()
    {
        return "Address Block";
    }

    public void addParameter(String name, String value)
    {
        // no parameters accepted !
        valid = false;
    }

    @Override
    public boolean execute(boolean dryRun)
    {
        try
        {
            String sql = "SELECT per_id, addr_id FROM pl_address_block ORDER BY per_id";
            ResultSet rs = db.executeQuery(sql);
            int last_per = 0;
            int num_addr = 0;
            Vector<Integer> addrBlockIds = new Vector<Integer>();
            while(rs.next())
            {
                int per = rs.getInt(1);
                int addr = rs.getInt(2);
                if(per != last_per)
                {
                    if(1 < num_addr)
                    {
                        log.info("peripheral " + last_per + " has " + num_addr + " address blocks !");
                        checkDeviceBlocks(last_per, addrBlockIds, dryRun);
                    }
                    else
                    {
                        // System.out.println("peripheral " + last_per + " has " + num_addr + " address blocks !");
                    }
                    last_per = per;
                    num_addr = 1;
                    addrBlockIds.clear();
                    addrBlockIds.add(addr);
                }
                else
                {
                    num_addr++;
                    addrBlockIds.add(addr);
                }
            }
            // last peripheral
            if(1 < num_addr)
            {
                log.info("peripheral " + last_per + " has " + num_addr + " address blocks !");
                checkDeviceBlocks(last_per, addrBlockIds, dryRun);
            }
            else
            {
                // log.info("peripheral " + last_per + " has " + num_addr + " address blocks !");
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private void checkDeviceBlocks(int last_per, Vector<Integer> addrBlockIds, boolean dryRun) throws SQLException
    {
        Vector<AddressBlock> addrBlocks = new Vector<AddressBlock>();
        for (Integer x : addrBlockIds)
        {
            AddressBlock block = new AddressBlock(x, db);
            boolean isDuplicate = false;
            for(AddressBlock a : addrBlocks)
            {
                comparisons++;
                if(true == a.equals(block))
                {
                    log.info("This           {}", a.toString());
                    log.info("is the same as {}", block.toString());
                    isDuplicate = true;
                    break;
                }
            }
            if(false == isDuplicate)
            {
                addrBlocks.add(block);
                // log.info("adding " + block.toString());
            }
            else
            {
                if(false == dryRun)
                {
                    // delete link
                    String sql = String.format("DELETE FROM pl_address_block WHERE per_id = %d and addr_id = %d LIMIT 1", last_per, block.getId());
                    db.executeUpdate(sql);
                    fixes++;
                }
                else
                {
                    log.info("dry run: Would have deleted link from address block {} to peripheral {} !", block.getId(), last_per);
                }
                if(false == dryRun)
                {
                    // delete address block
                    String sql = String.format("DELETE FROM p_address_block WHERE id = %d LIMIT 1", block.getId());
                    db.executeUpdate(sql);
                    fixes++;
                }
                else
                {
                    log.info("dry run: Would have deleted address block {} !", block.getId());
                }
            }
        }
    }

}
