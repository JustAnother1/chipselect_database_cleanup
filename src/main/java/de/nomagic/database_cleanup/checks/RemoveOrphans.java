package de.nomagic.database_cleanup.checks;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.nomagic.database_cleanup.DataBaseWrapper;

public class RemoveOrphans extends BasicCheck
{
    private final Logger log = LoggerFactory.getLogger(this.getClass().getName());

    public RemoveOrphans(DataBaseWrapper db)
    {
        super(db);
    }

    @Override
    public String getName()
    {
        return "remove orphans";
    }

    @Override
    public boolean execute(boolean dryRun)
    {
        // are all element linked ? remove unlinked elements (they are not accessible anyway)
        boolean run = true;
        // peripheral Instances
        if(run == true)
        {
            db.commit();
            run = removeOrphanPeripheralInstances_device(dryRun);
        }
        if(run == true)
        {
            db.commit();
            run = removeOrphanPeripheralInstances_null(dryRun);
        }
        if(run == true)
        {
            db.commit();
            run = removeOrphanPeripheralInstances(dryRun);
        }

        // Peripherals
        if(run == true)
        {
            db.commit();
            run = removeOrphanPeripherals(dryRun);
        }

        // Address Blocks
        if(run == true)
        {
            db.commit();
            run = removeOrphanAddressBlocks_peripheral(dryRun);
        }
        if(run == true)
        {
            db.commit();
            run = removeOrphanAddressBlocks_null(dryRun);
        }
        if(run == true)
        {
            db.commit();
            run = removeOrphanAddressBlocks(dryRun);
        }

        // Interrupts
        if(run == true)
        {
            db.commit();
            run = removeOrphanInterrupts_peripheral(dryRun);
        }
        if(run == true)
        {
            db.commit();
            run = removeOrphanInterrupts_null(dryRun);
        }
        if(run == true)
        {
            db.commit();
            run = removeOrphanInterrupts(dryRun);
        }

        // Registers
        if(run == true)
        {
            db.commit();
            run = removeOrphanRegisters_peripheral(dryRun);
        }
        if(run == true)
        {
            db.commit();
            // there is a link in pl_register to a register that has been removed
            // -> remove the link
            run = removeOrphanRegisters_nullPointer(dryRun);
        }
        if(run == true)
        {
            db.commit();
            run = removeOrphanRegisters(dryRun);
        }

        // Fields
        if(run == true)
        {
            db.commit();
            run = removeOrphanFields_register(dryRun);
        }
        if(run == true)
        {
            db.commit();
            run = removeOrphanFields_null(dryRun);
        }
        if(run == true)
        {
            db.commit();
            run = removeOrphanFields(dryRun);
        }


        return run;
    }


    public boolean removeOrphanAddressBlocks_peripheral(boolean dryRun)
    {
        log.info("removing orphan (unused / unlinked) address blocks ...");
        return findUnlinkedEntries("per_id", "pl_address_block",  // item
                                   "peripheral_id", "p_peripheral_instance", false, dryRun); // link
    }

    public boolean removeOrphanAddressBlocks_null(boolean dryRun)
    {
        log.info("removing orphan (unused / unlinked) address blocks ...");
        return findUnlinkedEntries("addr_id", "pl_address_block", // item
                                   "id", "p_address_block", false, dryRun);  // link
    }

    public boolean removeOrphanAddressBlocks(boolean dryRun)
    {
        log.info("removing orphan (unused / unlinked) address blocks ...");
        return findUnlinkedEntries("id", "p_address_block",  // item
                                   "addr_id", "pl_address_block", false, dryRun);  // link
    }

    private boolean removeOrphanInterrupts_null(boolean dryRun)
    {
        log.info("removing orphan (unused / unlinked) address blocks ...");
        return findUnlinkedEntries("irq_id", "pl_interrupt", // item)
                                   "id", "p_interrupt", false, dryRun);  // link
    }

    private boolean removeOrphanInterrupts(boolean dryRun)
    {
        log.info("removing orphan (unused / unlinked) address blocks ...");
        return findUnlinkedEntries("id", "p_interrupt",  // item
                                   "irq_id", "pl_interrupt", false, dryRun);  // link
    }

    private boolean removeOrphanInterrupts_peripheral(boolean dryRun)
    {
        log.info("removing orphan (unused / unlinked) address blocks ...");
        return findUnlinkedEntries("per_in_id", "pl_interrupt",  // item
                                   "id", "p_peripheral_instance", false, dryRun);  // link
    }

    public boolean removeOrphanPeripheralInstances_null(boolean dryRun)
    {
        log.info("removing orphan (unused / unlinked) peripheral instances ...");
        return findUnlinkedEntries("per_in_id","pl_peripheral_instance",
                                   "id", "p_peripheral_instance", false, dryRun);
    }

    public boolean removeOrphanPeripheralInstances(boolean dryRun)
    {
        log.info("removing orphan (unused / unlinked) peripheral instances ...");
        return findUnlinkedEntries("id", "p_peripheral_instance",
                                   "per_in_id","pl_peripheral_instance", false, dryRun);
    }

    public boolean removeOrphanPeripheralInstances_device(boolean dryRun)
    {
        log.info("removing orphan (missing device) peripheral instances ...");
        return findUnlinkedEntries("dev_id", "pl_peripheral_instance",
                                   "id", "microcontroller", false, dryRun);
    }

    private boolean removeOrphanPeripherals(boolean dryRun)
    {
        log.info("removing orphan (unused / unlinked) peripherals ...");
        return findUnlinkedEntries("id", "p_peripheral",
                                   "peripheral_id", "p_peripheral_instance", false, dryRun);
    }

    public boolean removeOrphanRegisters_peripheral(boolean dryRun)
    {
        log.info("removing orphan links to registers ...");
        return findUnlinkedEntries("per_id", "pl_register",
                                   "id", "p_peripheral", false, dryRun);
    }

    public boolean removeOrphanRegisters_nullPointer(boolean dryRun)
    {
        log.info("removing orphan (null pointer) register links ...");
        return findUnlinkedEntries("reg_id", "pl_register",
                                   "id", "p_register", false, dryRun);
    }

    public boolean removeOrphanRegisters(boolean dryRun)
    {
        log.info("removing orphan (unused / unlinked) registers ...");
        return findUnlinkedEntries("id", "p_register",
                                   "reg_id", "pl_register", false, dryRun);
    }

    public boolean removeOrphanFields_register(boolean dryRun)
    {
        log.info("removing orphan (unused / unlinked) fields ...");
        return findUnlinkedEntries("reg_id", "pl_field",
                                   "id", "p_register", false, dryRun);
    }

    public boolean removeOrphanFields_null(boolean dryRun)
    {
        log.info("removing orphan (unused / unlinked) fields ...");
        return findUnlinkedEntries("field_id", "pl_field",
                                   "id", "p_field", false, dryRun);
    }

    public boolean removeOrphanFields(boolean dryRun)
    {
        log.info("removing orphan (unused / unlinked) fields ...");
        return findUnlinkedEntries("id", "p_field",
                                   "field_id", "pl_field", false, dryRun);
    }


    // "SELECT dev_id FROM pl_peripheral_instance ORDER BY dev_id ASC", // item
    // "SELECT id FROM microcontroller ORDER BY id ASC",                // link
    // "DELETE FROM pl_peripheral_instance WHERE dev_id = %d"           // action

    private boolean findUnlinkedEntries(String itemColumn, String itemTable,
                                        String linkColumn, String linkTable,
                                        boolean schouldLog, boolean dryRun)
    {
        try
        {
            ResultSet rs_items = db.executeQuery(String.format("SELECT %s FROM %s ORDER BY %s ASC", itemColumn, itemTable, itemColumn));
            rs_items.first();
            ResultSet rs_links = db.executeQuery(String.format("SELECT %s FROM %s ORDER BY %s ASC", linkColumn, linkTable, linkColumn));
            rs_links.first();

            log.info("received data");
            int i;
            int f = 0;
            int itemVal = rs_items.getInt(1);
            if( true == schouldLog) {log.info("item = " + itemVal);}
            int linkValue = rs_links.getInt(1);
            if( true == schouldLog) {log.info("link = " + linkValue);}
            for(i = 0; ; i++)
            {
                if(linkValue == itemVal)
                {
                    if( true == schouldLog) {log.info("equal!");}
                    // this item is linked
                    rs_items.next();
                    if(rs_items.isAfterLast())
                    {
                        break;
                    }
                    itemVal = rs_items.getInt(1);
                    if( true == schouldLog) {log.info("item = " + itemVal);}
                }
                else if(linkValue > itemVal)
                {
                    if( true == schouldLog) {log.info("link greater!");}
                    rs_items.next();
                    // fix:
                    String sql = String.format("DELETE FROM %s WHERE %s = %d", itemTable, itemColumn, itemVal);
                    log.trace("Unlinked Item: {}", itemVal);
                    log.trace("Fixing by execuing: {}", sql);
                    if(false == dryRun)
                    {
                        f++;
                        db.executeUpdate(sql);
                    }
                    else
                    {
                        log.info("dry run: would have done: {}", sql);
                    }
                    if(rs_items.isAfterLast())
                    {
                        break;
                    }
                    itemVal = rs_items.getInt(1);
                    if( true == schouldLog) {log.info("item = " + itemVal);}
                }
                else if(linkValue < itemVal)
                {
                    if( true == schouldLog) {log.info("item greater!");}
                    rs_links.next();
                    if(rs_links.isAfterLast())
                    {
                        do {
                            // last item(s) is(/are) not linked !
                            // fix:
                            String sql = String.format("DELETE FROM %s WHERE %s = %d", itemTable, itemColumn, itemVal);
                            log.trace("Unlinked Item: {}", itemVal);
                            log.trace("Fixing by execuing: {}", sql);
                            if(false == dryRun)
                            {
                                f++;
                                db.executeUpdate(sql);
                            }
                            else
                            {
                                log.info("dry run: would have done: {}", sql);
                            }
                            rs_items.next();
                            if(false == rs_items.isAfterLast())
                            {
                                itemVal = rs_items.getInt(1);
                            }
                        } while(false == rs_items.isAfterLast());
                        break;
                    }
                    linkValue = rs_links.getInt(1);
                    if( true == schouldLog) {System.out.println("link = " + linkValue);}
                }
            }
            log.info("Done %,d comparisons!\n", i);
            log.info("found %,d unlinked elements!\n", f);
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
