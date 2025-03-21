package de.nomagic.database_cleanup.checks;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.nomagic.database_cleanup.DataBaseWrapper;
import de.nomagic.database_cleanup.RangeCheck;
import de.nomagic.database_cleanup.checks.helpers.Field;

public class CheckPeripherals extends BasicCheck
{
    private final Logger log = LoggerFactory.getLogger(this.getClass().getName());

    private long derivedChips = 0;
    private HashMap<Integer, String> fullChips = new HashMap<Integer, String>();
    private HashMap<Integer, String> noRegisterChips = new HashMap<Integer, String>();
    private HashMap<Integer, Integer> svdLink = new HashMap<Integer, Integer>();  // source, destination
    private HashMap<String, Integer> chipNames = new HashMap<String, Integer>();
    private boolean singleChipMode = false;
    private String singleChipName = null;


    public CheckPeripherals(DataBaseWrapper db)
    {
        super(db);
    }

    @Override
    public String getName()
    {
        return "check peripherals";
    }

    public void addParameter(String name, String value)
    {
        if("chip".equals(name))
        {
            singleChipMode = true;
            singleChipName = value;
        }
        else
        {
            // invalid setting
            valid = false;
        }
    }

    private void check_one_chip(Integer dev_id) throws SQLException
    {
        boolean size_always_bigger_as_offset = true;
        boolean never_out_of_bounds = true;
        int numErrors = 0;
        int num_Registers = 0;
        // get all peripheral instances for the device
        String sql = "SELECT id, name, peripheral_id "
                + "FROM p_peripheral_instance "
                + "INNER JOIN  pl_peripheral_instance "
                + "ON pl_peripheral_instance.per_in_id  = p_peripheral_instance.id "
                + "WHERE pl_peripheral_instance.dev_id = " + dev_id;
        ResultSet rs = db.executeQuery(sql);
        int per_num = 0;
        while(rs.next())
        {
            int per_in_id = rs.getInt(1);
            String per_in_name = rs.getString(2);
            int peripheral_id = rs.getInt(3);
            // get all registers for this peripheral
            String register_sql = "SELECT id, name, size "
                    + "FROM p_register "
                    + "INNER JOIN pl_register "
                    + "ON pl_register.reg_id = p_register.id "
                    + "WHERE pl_register.per_id = " + peripheral_id;
            ResultSet register_rs = db.executeQuery(register_sql);
            while(register_rs.next())
            {
                int reg_id = register_rs.getInt(1);
                String reg_name = register_rs.getString(2);
                int reg_size = register_rs.getInt(3);
                num_Registers++;
                if(1>reg_size)
                {
                    log.error("The chip " + fullChips.get(dev_id)
                    + " has a invalid register size(" + reg_size + ")"
                    + " for the register " + reg_name + "(" + reg_id + ")"
                    + " of the peripheral " + per_in_name + "(" + peripheral_id + ")!");
                }
                else
                {
                    RangeCheck collisionChecker = new RangeCheck(reg_size);
                    // get all fields for this register
                    String field_sql = "SELECT id, name, bit_offset, size_bit "
                            + "FROM p_field "
                            + "INNER JOIN pl_field "
                            + "ON pl_field.field_id = p_field.id "
                            + "WHERE pl_field.reg_id = " + reg_id;
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
                            log.error("ERROR: peripheral: " + per_in_name + "(" + peripheral_id + "),"
                                    + " register: " + reg_name + "(" + reg_id + "),"
                                    + " field: " + field_name + ","
                                    + " offset: " + field_bit_offset + ","
                                    + " size: " + field_size_bit + ","
                                    + " regSize: " + reg_size);
                        }
                        if(field_size_bit > reg_size)
                        {
                            never_out_of_bounds = false;
                            log.error("ERROR: peripheral: " + per_in_name + "(" + peripheral_id + "),"
                                    + " register: " + reg_name + "(" + reg_id + ")" + ","
                                    + " field: " + field_name + ","
                                    + " offset: " + field_bit_offset + ","
                                    + " size: " + field_size_bit + ","
                                    + " regSize: " + reg_size);
                        }
                        if( 1 > field_size_bit)
                        {
                            inconsistencies++;
                            log.error("The chip " + fullChips.get(dev_id)
                                    + " has a invalid size(" + field_size_bit + ") "
                                    + "of the field " + field_name
                                    + " in the register " + reg_name + "(" + reg_id + ")" + "[" + reg_size + "]"
                                    + " of the peripheral " + per_in_name + "(" + peripheral_id + ")!");
                        }
                        else
                        {
                            Field f = new Field(field_id, field_name, field_bit_offset, field_size_bit);
                            collisionChecker.add(f);
                        }
                    }
                    comparisons++;
                    if(true == collisionChecker.hasError())
                    {
                        numErrors++;
                        inconsistencies++;
                        if(true == collisionChecker.hasCollision())
                        {
                            log.error("The chip " + fullChips.get(dev_id)
                                    + " has a field collision in the register " + reg_name + "(" + reg_id + ")" +  "[" + reg_size + "]"
                                    + " of the peripheral " + per_in_name + "(" + peripheral_id + ")!");
                        }
                        if(true == collisionChecker.hasOutOfBoundsError())
                        {
                            log.error("The chip " + fullChips.get(dev_id)
                                    + " has a field out of bounds error in the register " + reg_name + "(" + reg_id + ")" + "[" + reg_size + "]"
                                    + " of the peripheral " + per_in_name + "(" + peripheral_id + ")!");
                        }
                        log.error("The error was: " + collisionChecker.getErrorMessage());
                    }
                }
            }
            per_num++;
        }
        if(1 > per_num)
        {
            String chipName = fullChips.get(dev_id);
            // log.error("The chip " + chipName + " has no peripherals !");
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
                log.info("Registers : " + num_Registers + " Errors: " + numErrors + " percentile error: " + percentile);
                if(true == size_always_bigger_as_offset)
                {
                    log.info("size is always bigger than the offset!");
                }
                if(true == never_out_of_bounds)
                {
                    log.info("never out of bounds!");
                }
            }
        }
    }

    private boolean singleChipMode(boolean dryRun) throws SQLException
    {
        log.info("Single Chip mode for {} !", singleChipName);

        // get chip
        String sql = "SELECT id, svd_id "
                + "FROM microcontroller "
                + "WHERE name='" + singleChipName + "'";
        ResultSet rs = db.executeQuery(sql);
        int num = 0;
        int microcontroller_id = 0;
        int svdId = 0;
        while(rs.next())
        {
            microcontroller_id = rs.getInt(1);
            svdId = rs.getInt(2);
            num++;
        }

        if(num > 1)
        {
            log.error("found {} chips.", num);
            return false;
        }
        if(0 == num)
        {
            log.error("found {} chips.", num);
            return false;
        }
        if(0 != svdId)
        {
            log.info("Chip is a derived chip (no own SVD information)");
            sql = "SELECT id, name, svd_id "
                    + "FROM microcontroller "
                    + "WHERE id = " + svdId;
            rs = db.executeQuery(sql);
            num = 0;
            String orig_name = singleChipName;
            while(rs.next())
            {
                int orig_id = rs.getInt(1);
                orig_name = rs.getString(2);
                log.info("linked to {}", orig_name);
                int orig_svdId = rs.getInt(3);
                if(orig_id != svdId)
                {
                    log.error("id != svdId !");
                    return false;
                }
                if(orig_svdId != 0)
                {
                    log.error("linked to linked chip (svdId = {})!", orig_svdId);
                    return false;
                }
                num++;
            }
            if(1 != num)
            {
                log.error("found {} referenced chips.", num);
                return false;
            }
            fullChips.put(svdId, orig_name);
            check_one_chip(svdId);
        }
        else
        {
            log.info("Chip has own SVD information (not derived)");
            fullChips.put(microcontroller_id, singleChipName);
            check_one_chip(microcontroller_id);
        }

        return true;
    }

    @Override
    public boolean execute(boolean dryRun)
    {
        try
        {
            if(true == singleChipMode)
            {
                return singleChipMode(dryRun);
            }
            else
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
                log.info("found {} chips with peripherals.", fullChips.size());
                log.info("found {} chip variants", derivedChips);
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
                            log.error("Chip refered(svd_id) to a not existing chip({}) from ({})!", destination, source);
                            inconsistencies++;
                        }
                        else
                        {
                            if(false == dryRun)
                            {
                                // fix
                                log.info("Fixing svd_id link");
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
                            else
                            {
                                log.info("dry run: would have would hvae set the svd id to {} for microcontroller {} !", nextHop, source);
                            }
                        }
                    }
                    // else -> OK
                }
                // for every device
                for(Integer dev_id : fullChips.keySet())
                {
                    String name = fullChips.get(dev_id);
                    log.info("now checking Chip {}", name);
                    check_one_chip(dev_id);
                }
                log.info("found {} chips without registers.", noRegisterChips.size());
                if(0 < noRegisterChips.size())
                {
                    fix_try_to_find_original_for_register_less_chips(dryRun);
                }
                return true;
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
            return false;
        }
    }

    private void fix_try_to_find_original_for_register_less_chips(boolean dryRun)
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
                        log.info("Found Family Definition ({}) for {}!", origName, name);
                        if(false == dryRun)
                        {
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
                        }
                        else
                        {
                            log.info("dry run: would have set the SVD id to {} for microcontrolle {}", origId, dev_id);
                        }
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
