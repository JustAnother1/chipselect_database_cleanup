package de.nomagic.database_cleanup.checks;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Vector;

import de.nomagic.database_cleanup.DataBaseWrapper;
import de.nomagic.database_cleanup.checks.helpers.AddressBlock;

public class CheckAddressBlocks extends BasicCheck
{

    public CheckAddressBlocks(DataBaseWrapper db)
    {
        super(db);
    }

    @Override
    public String getName()
    {
        return "Address Block";
    }

    @Override
    public boolean execute()
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
                        System.out.println("peripheral " + last_per + " has " + num_addr + " address blocks !");
                        checkDeviceBlocks(last_per, addrBlockIds);
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
                System.out.println("peripheral " + last_per + " has " + num_addr + " address blocks !");
                checkDeviceBlocks(last_per, addrBlockIds);
            }
            else
            {
                // System.out.println("peripheral " + last_per + " has " + num_addr + " address blocks !");
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private void checkDeviceBlocks(int last_per, Vector<Integer> addrBlockIds) throws SQLException
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
                    System.out.println("This           " + a.toString());
                    System.out.println("is the same as " + block.toString());
                    isDuplicate = true;
                    inconsistencies++;
                    break;
                }
            }
            if(false == isDuplicate)
            {
                addrBlocks.add(block);
                // System.out.println("adding " + block.toString());
            }
            else
            {
                // delete link
                String sql = String.format("DELETE FROM pl_address_block WHERE per_id = %d and addr_id = %d LIMIT 1", last_per, block.getId());
                db.executeUpdate(sql);
                fixes++;
            }
        }
    }

}
