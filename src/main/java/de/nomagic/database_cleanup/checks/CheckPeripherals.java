package de.nomagic.database_cleanup.checks;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Vector;

import de.nomagic.database_cleanup.DataBaseWrapper;
import de.nomagic.database_cleanup.RangeCheck;

public class CheckPeripherals extends BasicCheck
{
    private long derivedChips = 0;
    private HashMap<Integer, String> fullChips = new HashMap<Integer, String>();
    private HashMap<Integer, String> noRegisterChips = new HashMap<Integer, String>();
    private HashMap<Integer, Integer> svdLink = new HashMap<Integer, Integer>();  // source, destination
    private HashMap<String, Integer> chipNames = new HashMap<String, Integer>();

    public CheckPeripherals(boolean verbose, DataBaseWrapper db)
    {
        super(verbose, db);
    }

    private void check_one_chip(Integer dev_id) throws SQLException
    {
    	boolean size_always_bigger_as_offset = true;
    	boolean never_out_of_bounds = true;
    	int numErrors = 0;
    	int num_Registers = 0;
        // get all peripheral instances for the device
        String sql = "SELECT id, name, peripheral_id FROM p_peripheral_instance INNER JOIN  pl_peripheral_instance ON pl_peripheral_instance.per_in_id  = p_peripheral_instance.id "
                + "WHERE pl_peripheral_instance.dev_id = " + dev_id;
        ResultSet rs = db.executeQuery(sql);
        int per_num = 0;
        while(rs.next())
        {
            int per_in_id = rs.getInt(1);
            String per_in_name = rs.getString(2);
            int peripheral_id = rs.getInt(3);
            // get all registers for this peripheral
            String register_sql = "SELECT id, name, size FROM p_register INNER JOIN pl_register ON pl_register.reg_id = p_register.id WHERE pl_register.per_id = " + peripheral_id;
            ResultSet register_rs = db.executeQuery(register_sql);
            while(register_rs.next())
            {
                int reg_id = register_rs.getInt(1);
                String reg_name = register_rs.getString(2);
                int reg_size = register_rs.getInt(3);
                num_Registers++;
                if(1>reg_size)
                {
                    System.err.println("The chip " + fullChips.get(dev_id) + " has a invalid register size(" + reg_size + ") for the register " + reg_name + " of the peripheral " + per_in_name + " !");
                }
                else
                {
                    RangeCheck collisionChecker = new RangeCheck(reg_size);
                    // get all fields for this register
                    String field_sql = "SELECT id, name, bit_offset, size_bit FROM p_field INNER JOIN pl_field ON pl_field.field_id = p_field.id WHERE pl_field.reg_id = " + reg_id;
                    ResultSet field_rs = db.executeQuery(field_sql);
                    while(field_rs.next())
                    {
                        // check for collisions
                        int field_id = field_rs.getInt(1);
                        String field_name = field_rs.getString(2);
                        int field_bit_offset = field_rs.getInt(3);
                        int field_size_bit = field_rs.getInt(4);
                        if(field_size_bit < field_bit_offset)
                        {
                        	size_always_bigger_as_offset = false;
                        }
                        if(field_bit_offset > (reg_size-1))
                        {
                        	never_out_of_bounds = false;
                        	System.err.println("ERROR: peripheral: " + per_in_name + ", register: " + reg_name + ", field: " + field_name + ", offset: " + field_bit_offset + ", size: " + field_size_bit + ", regSize: " + reg_size);
                        }
                        if(field_size_bit > reg_size)
                        {
                        	never_out_of_bounds = false;
                        	System.err.println("ERROR: peripheral: " + per_in_name + ", register: " + reg_name + ", field: " + field_name + ", offset: " + field_bit_offset + ", size: " + field_size_bit + ", regSize: " + reg_size);
                        }
                        if( 1 > field_size_bit)
                        {
                            inconsistencies++;
                            System.err.println("The chip " + fullChips.get(dev_id) + " has a invalid size(" + field_size_bit + ") of the field " + field_name + " in the register " + reg_name +  "(" + reg_size + ") of the peripheral " + per_in_name + " !");
                        }
                        else
                        {
                        	collisionChecker.add(field_bit_offset, field_size_bit, field_name);
                        }
                    }
                    comparisons++;
                    if(true == collisionChecker.hasError())
                    {
                    	numErrors++;
                        inconsistencies++;
                        if(true == collisionChecker.hasCollision())
                        {
                            System.err.println("The chip " + fullChips.get(dev_id) + " has a field collision in the register " + reg_name +  "(" + reg_size + ") of the peripheral " + per_in_name + " !");
                        }
                        if(true == collisionChecker.hasOutOfBoundsError())
                        {
                            System.err.println("The chip " + fullChips.get(dev_id) + " has a field out of bounds error in the register " + reg_name + "(" + reg_size + ") of the peripheral " + per_in_name + " !");
                        }
                        // System.err.println("The error was: " + collisionChecker.getErrorMessage());
                    }
                }
            }
            per_num++;
        }
        if(1 > per_num)
        {
        	String chipName = fullChips.get(dev_id);
            // System.err.println("The chip " + chipName + " has no peripherals !");
            noRegisterChips.put(dev_id, chipName);
            // fullChips.remove(dev_id);
            inconsistencies++;
        }
        else
        {
            // else -> OK
        	if(0 < numErrors)
        	{
        		float percentile = numErrors;
        		percentile = percentile / num_Registers;
        		percentile = percentile * 100;
        		System.out.println("Registers : " + num_Registers + " Errors: " + numErrors + " percentile error: " + percentile);
        		if(true == size_always_bigger_as_offset)
        		{
        			System.out.println("size is always bigger than the offset!");
        		}
        		if(true == never_out_of_bounds)
        		{
        			System.out.println("never out of bounds!");
        		}
        	}
        }
    }
    
    @Override
    public boolean execute()
    {
        try
        {
            // get all chips
            String sql = "SELECT id, name, svd_id FROM microcontroller";
            ResultSet rs = db.executeQuery(sql);
            while(rs.next())
            {
                int id = rs.getInt(1);
                String name = rs.getString(2);
                int svdId = rs.getInt(3);
                chipNames.put(name, id);
                if(0 == svdId)
                {
                    // this is a real chip
                    fullChips.put(id, name);
                }
                else
                {
                    // this chip is derived of another chip
                    derivedChips++;
                    svdLink.put(id, svdId);
                }
            }
            System.out.println("found " + fullChips.size() + " chips with peripherals.");
            System.out.println("found " + derivedChips + " chip variants");
            // check that all chips referred to by svd_id exist
            for(Integer source: svdLink.keySet())
            {
                comparisons++;
                Integer destination = svdLink.get(source);
                if(false == fullChips.containsKey(destination))
                {
                	Integer nextHop = svdLink.get(destination);
                	if(null == nextHop)
                	{
                		System.err.println("Chip refered(svd_id) to a not existing chip(" + destination + ") from (" + source + ")!");
                		inconsistencies++;
                	}
                	else
                	{
                		// fix
                		System.err.println("Fixing svd_id link");
	                    String sqlFix = "UPDATE microcontroller SET svd_id = \"" + nextHop + "\" WHERE id = " + source;
	                    try 
	                    {
							db.executeUpdate(sqlFix);
						} 
	                    catch (SQLException e) 
	                    {
							e.printStackTrace();
						}
						fixes++;
                	}
                }
                // else -> OK
            }
            // for every device
            for(Integer dev_id : fullChips.keySet())
            {
            	String name = fullChips.get(dev_id);
        		System.out.println("now checking Chip " + name);
        		check_one_chip(dev_id);
            }
            System.out.println("found " + noRegisterChips.size() + " chips without registers.");
            if( 0 < noRegisterChips.size())
            {
            	fix_try_to_find_original_for_register_less_chips();
            }
            return true;
        }
        catch (SQLException e)
        {
            e.printStackTrace();
            return false;
        }
    }

	private void fix_try_to_find_original_for_register_less_chips() 
	{
		for(Integer dev_id : noRegisterChips.keySet())
		{
			// get name of chip,
			String name = noRegisterChips.get(dev_id);
			if(null == name)
			{
				continue;
			}
			// System.out.println("trying to fix " + name + " ...");
			int checklength = name.length() -2;
			for(int i = 1; i < checklength; i++)
			{
				String nameToTest = name.substring(0, name.length() - i);
				// System.out.println("testing " + nameToTest);
				Integer origId = chipNames.get(nameToTest);
				if(null != origId)
				{
					String origName = fullChips.get(origId);
					if(null != origName)
					{
						System.out.println("Found Family Definition (" + origName + ") for " + name + "!");
	                    String sqlFix = "UPDATE microcontroller SET svd_id = \"" + origId + "\" WHERE id = " + dev_id;
	                    try 
	                    {
							db.executeUpdate(sqlFix);
						} 
	                    catch (SQLException e) 
	                    {
							e.printStackTrace();
						}
						fixes++;
						break;
					}
					else
					{
						System.out.println("ERROR: Chip is missing!!");
					}
				}
			}
		}
	}

}
