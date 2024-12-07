package de.nomagic.database_cleanup.checks;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.nomagic.database_cleanup.DataBaseWrapper;
import de.nomagic.database_cleanup.HexString;

public class RAMandFlashSizes extends BasicCheck
{
    private final Logger log = LoggerFactory.getLogger(this.getClass().getName());

    public RAMandFlashSizes(DataBaseWrapper db)
    {
        super(db);
    }

    @Override
    public String getName()
    {
        return "RAM + Flash sizes";
    }

    public boolean execute()
    {
        boolean run = true;

        if(true == run)
        {
            run = ramSize();
        }

        if(true == run)
        {
            run = flashSize();
        }

        return run;
    }

    private boolean ramSize()
    {
        System.out.println("testing consistency of RAM size information ...");
        try
        {
            String sql = "SELECT id, RAM_size_kB, RAM_size_byte, name FROM microcontroller";
            ResultSet rs = db.executeQuery(sql);
            while(rs.next())
            {
                int id = rs.getInt(1);
                int sizeKB = rs.getInt(2);
                HexString hex = new HexString(rs.getString(3));
                long sizeByte =  hex.asLong();
                String name = rs.getString(4);
                comparisons++;
                if(0 == sizeKB)
                {
                    // we have a byte exact size but no kb size -> so add that
                    long bytes = sizeByte / 1024;
                    if(0 != bytes)
                    {
                        String updateSql = "UPDATE microcontroller SET RAM_size_kB = " + bytes + " WHERE id =" + id;
                        db.executeUpdate(updateSql);
                        fixes++;
                    }
                }
                else
                {
                    int bytes = sizeKB * 1024;
                    if(0 == sizeByte)
                    {
                        // we have a kb size but no byte exact size
                        String updateSql = "UPDATE microcontroller SET RAM_size_byte = " + bytes + " WHERE id =" + id;
                        db.executeUpdate(updateSql);
                        fixes++;
                    }
                    else
                    {
                        // we have both values
                        if(sizeByte == bytes)
                        {
                            // and they are the same -> OK
                        }
                        else
                        {
                            long diff = sizeByte - bytes;
                            diff = Math.abs(diff);
                            if(diff > 1024)
                            {
                                inconsistencies++;
                                log.trace("Inconsistency found in " + name + " !!!");
                                log.trace("RAM bytes: " + sizeByte + " (" +  hex.toString() + ") RAM kb: " + sizeKB + " difference: " + (sizeByte - bytes) + " !");
                            }
                            // else -> difference due to resolution / rounding to full kb.
                        }
                    }
                }
            }
            return true;
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
        return false;
    }

    private boolean flashSize()
    {
        System.out.println("testing consistency of FLASH size information ...");
        try
        {
            String sql = "SELECT id, name, Flash_size_kB FROM microcontroller";
            ResultSet rs = db.executeQuery(sql);
            while(rs.next())
            {
                int id = rs.getInt(1);
                String name = rs.getString(2);
                int sizeKB = rs.getInt(3);

                String FBsql = "SELECT size"
                        + " FROM p_flash_bank"
                        + " INNER JOIN  pl_flash_bank ON pl_flash_bank.flash_id  = p_flash_bank.id"
                        + " WHERE pl_flash_bank.dev_id = " + id;
                ResultSet FBrs = db.executeQuery(FBsql);
                long sizeByte = 0;
                while(FBrs.next())
                {
                    // it may have more than one Flash bank,..
                    String entry = FBrs.getString(1);
                    HexString hex = new HexString(entry);
                    sizeByte += hex.asLong();
                }

                comparisons++;

                if(0 == sizeKB)
                {
                    long bytes = sizeByte / 1024;
                    if(0 != bytes)
                    {
                        // we have a byte exact size but no kb size -> so add that
                        String updateSql = "UPDATE microcontroller SET Flash_size_kB = " + bytes + " WHERE id =" + id;
                        db.executeUpdate(updateSql);
                        fixes++;
                    }
                }
                else
                {
                    int bytes = sizeKB * 1024;
                    if((0 != sizeByte) && (0 != bytes))
                    {
                        // we have both values
                        if(sizeByte == bytes)
                        {
                            // and they are the same -> OK
                        }
                        else
                        {
                            long diff = sizeByte - bytes;
                            diff = Math.abs(diff);
                            if(diff > 1024)
                            {
                                inconsistencies++;
                                log.trace("Inconsistency found in " + name + " !!!");
                                log.trace("FLASH bytes: " + sizeByte + " ( " + (sizeByte/1024) + "kb) FLASH kb: " + sizeKB + " difference: " + (sizeByte - bytes) + " !");
                            }
                            // else -> difference due to resolution / rounding to full kb.
                        }
                    }
                }
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
