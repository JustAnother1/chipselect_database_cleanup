package de.nomagic.database_cleanup;

import java.util.Vector;

import de.nomagic.database_cleanup.checks.BasicCheck;
import de.nomagic.database_cleanup.checks.CheckAddressBlocks;
import de.nomagic.database_cleanup.checks.CheckAlternativeUsages;
import de.nomagic.database_cleanup.checks.CheckPeripherals;
import de.nomagic.database_cleanup.checks.CleanupStrings;
import de.nomagic.database_cleanup.checks.RAMandFlashSizes;
import de.nomagic.database_cleanup.checks.RemoveOrphans;

public class CleanupMain
{
    private String dbLocation;
    private String dbUser;
    private String dbPassword;
    private boolean verbose = false;
    private boolean allTests = true;
    private boolean ramTest = false;
    private boolean orphanTest = false;
    private boolean stringTest = false;
    private boolean altUsageTest = false;
    private boolean peripheralsTest = false;
    private boolean addressBlockTest = false;

    public CleanupMain()
    {
    }

    private void parseConfig(String[] args)
    {
        if(args.length < 3)
        {
            System.out.println("Usage: <database host> <db user> <db password> <verbose> <test>");
            System.exit(1);
        }
        else
        {
            System.out.println("host : " + args[0] + " !");
            System.out.println("user : " + args[1] + " !");
            System.out.println("password : " + args[2] + " !");
            dbLocation = args[0];
            dbUser = args[1];
            dbPassword = args[2];
            if(args.length > 3)
            {
                if(true == Boolean.parseBoolean(args[3]))
                {
                    System.out.println("enabled verbose mode !");
                    verbose = true;
                }
                else
                {
                    System.out.println("disabled verbose mode !");
                    verbose = false;
                }
            }
            if(args.length > 4)
            {
                switch(args[4])
                {
                case "all":
                    allTests = true;
                    break;

                case "RAM":
                    ramTest = true;
                    allTests = false;
                    break;

                case "Orphans":
                    orphanTest = true;
                    allTests = false;
                    break;

                case "cleanup":
                    stringTest = true;
                    allTests = false;
                    break;

                case "alt":
                    altUsageTest = true;
                    allTests = false;
                    break;

                case "peripherals":
                    peripheralsTest = true;
                    allTests = false;
                    break;
                    
                case "address_block":
                    addressBlockTest = true;
                    allTests = false;
                    break;

                default:
                    System.err.println("invalid test specification (" + args[4] + ") !");
                    allTests = false;
                    break;
                }
            }
        }
    }

    public void doIt(String[] args)
    {
        boolean run = true;
        int global_comparisions = 0;
        int global_fixes = 0;
        int global_inconsistencies = 0;
        Vector<BasicCheck> allTest = new Vector<BasicCheck>();

        parseConfig(args);

        DataBaseWrapper db = new DataBaseWrapper();
        db.connectToDataBase(dbLocation, dbUser, dbPassword);

        if(true == allTests)
        {
            allTest.add( new RAMandFlashSizes(verbose, db));
            allTest.add( new RemoveOrphans(verbose, db));
            allTest.add( new CleanupStrings(verbose, db));
            allTest.add( new CheckAlternativeUsages(verbose, db));
            allTest.add( new CheckPeripherals(verbose, db));
            allTest.add( new CheckAddressBlocks(verbose, db));
        }
        else
        {
            if(ramTest)
            {
                allTest.add( new RAMandFlashSizes(verbose, db));
            }
            if(orphanTest)
            {
                allTest.add( new RemoveOrphans(verbose, db));
            }
            if(stringTest)
            {
                allTest.add( new CleanupStrings(verbose, db));
            }
            if(altUsageTest)
            {
                allTest.add( new CheckAlternativeUsages(verbose, db));
            }
            if(peripheralsTest)
            {
                allTest.add( new CheckPeripherals(verbose, db));
            }
            if(addressBlockTest)
            {
            	 allTest.add( new CheckAddressBlocks(verbose, db));
            }
        }

        for(int i = 0; i < allTest.size(); i++)
        {
            BasicCheck curCheck = allTest.get(i);
            System.out.println("Now starting test " + curCheck.getName() + " ...");
            run = curCheck.execute();
            global_comparisions += curCheck.getNumberComparissons();
            global_fixes += curCheck.getFixes();
            global_inconsistencies += curCheck.getInconsistencies();
            if(false == run)
            {
                break;
            }
        }

        // all done
        db.closeDataBaseConnection();

        System.out.printf("In this session:\n"
                + "comaprisons: %,d\n"
                + "fixes: %,d (%3.2f %%)\n"
                + "inconsistencies: %,d (%3.2f %%)\n",
                global_comparisions,
                global_fixes, ((global_fixes * 100.0)/global_comparisions),
                global_inconsistencies, ((global_inconsistencies * 100.0)/global_comparisions));
    }

    public static void main(String[] args)
    {
        CleanupMain cm = new CleanupMain();
        cm.doIt(args);
    }

}
