package de.nomagic.database_cleanup.checks;

import java.sql.ResultSet;
import java.sql.SQLException;

import de.nomagic.database_cleanup.DataBaseWrapper;

public class RemoveOrphans extends BasicCheck
{

    public RemoveOrphans(boolean verbose, DataBaseWrapper db)
    {
        super(verbose, db);
    }
    
	@Override
	public String getName() 
	{
		return "remove orphans";
	}

    @Override
    public boolean execute()
    {
        // are all element linked ? remove unlinked elements (they are not accessible anyway)
        boolean run = true;
        // peripheral Instances
        if(run == true)
        {
            db.commit();
            run = removeOrphanPeripheralInstances_device();
        }
        if(run == true)
        {
            db.commit();
            run = removeOrphanPeripheralInstances_null();
        }
        if(run == true)
        {
            db.commit();
            run = removeOrphanPeripheralInstances();
        }

        // Peripherals
        if(run == true)
        {
            db.commit();
            run = removeOrphanPeripherals();
        }

        // Address Blocks
        if(run == true)
        {
            db.commit();
            run = removeOrphanAddressBlocks_peripheral();
        }
        if(run == true)
        {
            db.commit();
            run = removeOrphanAddressBlocks_null();
        }
        if(run == true)
        {
            db.commit();
            run = removeOrphanAddressBlocks();
        }

        // Interrupts
        if(run == true)
        {
            db.commit();
            run = removeOrphanInterrupts_peripheral();
        }
        if(run == true)
        {
            db.commit();
            run = removeOrphanInterrupts_null();
        }
        if(run == true)
        {
            db.commit();
            run = removeOrphanInterrupts();
        }

        // Registers
        if(run == true)
        {
            db.commit();
            run = removeOrphanRegisters_peripheral();
        }
        if(run == true)
        {
            db.commit();
            // there is a link in pl_register to a register that has been removed
            // -> remove the link
            run = removeOrphanRegisters_nullPointer();
        }
        if(run == true)
        {
            db.commit();
            run = removeOrphanRegisters();
        }

        // Fields
        if(run == true)
        {
            db.commit();
            run = removeOrphanFields_register();
        }
        if(run == true)
        {
            db.commit();
            run = removeOrphanFields_null();
        }
        if(run == true)
        {
            db.commit();
            run = removeOrphanFields();
        }


        return run;
    }


    public boolean removeOrphanAddressBlocks_peripheral()
    {
        System.out.println("removing orphan (unused / unlinked) address blocks ...");
        return findUnlinkedEntries("per_id", "pl_address_block",  // item
                                   "peripheral_id", "p_peripheral_instance", false); // link
    }

    public boolean removeOrphanAddressBlocks_null()
    {
        System.out.println("removing orphan (unused / unlinked) address blocks ...");
        return findUnlinkedEntries("addr_id", "pl_address_block", // item
                                   "id", "p_address_block", false);  // link
    }

    public boolean removeOrphanAddressBlocks()
    {
        System.out.println("removing orphan (unused / unlinked) address blocks ...");
        return findUnlinkedEntries("id", "p_address_block",  // item
                                   "addr_id", "pl_address_block", false);  // link
    }

    private boolean removeOrphanInterrupts_null()
    {
        System.out.println("removing orphan (unused / unlinked) address blocks ...");
        return findUnlinkedEntries("irq_id", "pl_interrupt", // item)
                                   "id", "p_interrupt", false);  // link
    }

    private boolean removeOrphanInterrupts()
    {
        System.out.println("removing orphan (unused / unlinked) address blocks ...");
        return findUnlinkedEntries("id", "p_interrupt",  // item
                                   "irq_id", "pl_interrupt", false);  // link
    }

    private boolean removeOrphanInterrupts_peripheral()
    {
        System.out.println("removing orphan (unused / unlinked) address blocks ...");
        return findUnlinkedEntries("per_in_id", "pl_interrupt",  // item
                                   "id", "p_peripheral_instance", false);  // link
    }

    public boolean removeOrphanPeripheralInstances_null()
    {
        System.out.println("removing orphan (unused / unlinked) peripheral instances ...");
        return findUnlinkedEntries("per_in_id","pl_peripheral_instance",
                                   "id", "p_peripheral_instance", false);
    }

    public boolean removeOrphanPeripheralInstances()
    {
        System.out.println("removing orphan (unused / unlinked) peripheral instances ...");
        return findUnlinkedEntries("id", "p_peripheral_instance",
                                   "per_in_id","pl_peripheral_instance", false);
    }

    public boolean removeOrphanPeripheralInstances_device()
    {
        System.out.println("removing orphan (missing device) peripheral instances ...");
        return findUnlinkedEntries("dev_id", "pl_peripheral_instance",
                                   "id", "microcontroller", false);
    }

    private boolean removeOrphanPeripherals()
    {
        System.out.println("removing orphan (unused / unlinked) peripherals ...");
        return findUnlinkedEntries("id", "p_peripheral",
                                   "peripheral_id", "p_peripheral_instance", false);
    }

    public boolean removeOrphanRegisters_peripheral()
    {
        System.out.println("removing orphan links to registers ...");
        return findUnlinkedEntries("per_id", "pl_register",
                                   "id", "p_peripheral", false);
    }

    public boolean removeOrphanRegisters_nullPointer()
    {
        System.out.println("removing orphan (null pointer) register links ...");
        return findUnlinkedEntries("reg_id", "pl_register",
                                   "id", "p_register", false);
    }

    public boolean removeOrphanRegisters()
    {
        System.out.println("removing orphan (unused / unlinked) registers ...");
        return findUnlinkedEntries("id", "p_register",
                                   "reg_id", "pl_register", false);
    }

    public boolean removeOrphanFields_register()
    {
        System.out.println("removing orphan (unused / unlinked) fields ...");
        return findUnlinkedEntries("reg_id", "pl_field",
                                   "id", "p_register", false);
    }

    public boolean removeOrphanFields_null()
    {
        System.out.println("removing orphan (unused / unlinked) fields ...");
        return findUnlinkedEntries("field_id", "pl_field",
                                   "id", "p_field", false);
    }

    public boolean removeOrphanFields()
    {
        System.out.println("removing orphan (unused / unlinked) fields ...");
        return findUnlinkedEntries("id", "p_field",
                                   "field_id", "pl_field", false);
    }


    // "SELECT dev_id FROM pl_peripheral_instance ORDER BY dev_id ASC", // item
    // "SELECT id FROM microcontroller ORDER BY id ASC",                // link
    // "DELETE FROM pl_peripheral_instance WHERE dev_id = %d"           // action

    private boolean findUnlinkedEntries(String itemColumn, String itemTable,
                                        String linkColumn, String linkTable, boolean log)
    {
        try
        {
            ResultSet rs_items = db.executeQuery(String.format("SELECT %s FROM %s ORDER BY %s ASC", itemColumn, itemTable, itemColumn));
            rs_items.first();
            ResultSet rs_links = db.executeQuery(String.format("SELECT %s FROM %s ORDER BY %s ASC", linkColumn, linkTable, linkColumn));
            rs_links.first();

            System.out.println("received data");
            int i;
            int f = 0;
            int itemVal = rs_items.getInt(1);
            if( true == log) {System.out.println("item = " + itemVal);}
            int linkValue = rs_links.getInt(1);
            if( true == log) {System.out.println("link = " + linkValue);}
            for(i = 0; ; i++)
            {
                if(linkValue == itemVal)
                {
                    if( true == log) {System.out.println("equal!");}
                    // this item is linked
                    rs_items.next();
                    if(rs_items.isAfterLast())
                    {
                        break;
                    }
                    itemVal = rs_items.getInt(1);
                    if( true == log) {System.out.println("item = " + itemVal);}
                }
                else if(linkValue > itemVal)
                {
                    if( true == log) {System.out.println("link greater!");}
                    f++;
                    rs_items.next();
                    // fix:
                    String sql = String.format("DELETE FROM %s WHERE %s = %d", itemTable, itemColumn, itemVal);
                    if(true == verbose)
                    {
                        System.err.println("Unlinked Item: " + itemVal);
                        System.out.println("Fixing by execuing: " + sql);
                    }
                    db.executeUpdate(sql);
                    if(rs_items.isAfterLast())
                    {
                        break;
                    }
                    itemVal = rs_items.getInt(1);
                    if( true == log) {System.out.println("item = " + itemVal);}
                }
                else if(linkValue < itemVal)
                {
                    if( true == log) {System.out.println("item greater!");}
                    rs_links.next();
                    if(rs_links.isAfterLast())
                    {
                        do {
                            // last item(s) is(/are) not linked !
                            // fix:
                            f++;
                            String sql = String.format("DELETE FROM %s WHERE %s = %d", itemTable, itemColumn, itemVal);
                            if(true == verbose)
                            {
                                System.err.println("Unlinked Item: " + itemVal);
                                System.out.println("Fixing by execuing: " + sql);
                            }
                            db.executeUpdate(sql);
                            rs_items.next();
                            if(false == rs_items.isAfterLast())
                            {
                                itemVal = rs_items.getInt(1);
                            }
                        } while(false == rs_items.isAfterLast());
                        break;
                    }
                    linkValue = rs_links.getInt(1);
                    if( true == log) {System.out.println("link = " + linkValue);}
                }
            }
            System.out.printf("Done %,d comparisons!\n", i);
            System.out.printf("found %,d unlinked elements!\n", f);
            comparisons += i;
            fixes +=f;
            return true;
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
        return false;
    }

}
