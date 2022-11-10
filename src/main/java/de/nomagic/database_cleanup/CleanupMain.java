package de.nomagic.database_cleanup;

import de.nomagic.database_cleanup.checks.BasicCheck;
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

    public CleanupMain()
    {
    }

    private void parseConfig(String[] args)
    {
        if(args.length < 3)
        {
            System.out.println("Usage: <database host> <db user> <db password>");
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
        }
    }

    public void doIt(String[] args)
    {
        boolean run = true;
        int global_comparisions = 0;
        int global_fixes = 0;
        int global_inconsistencies = 0;

        parseConfig(args);

        DataBaseWrapper db = new DataBaseWrapper();
        db.connectToDataBase(dbLocation, dbUser, dbPassword);

        BasicCheck[] allTest = {
                new RAMandFlashSizes(verbose, db),
                new RemoveOrphans(verbose, db),
                new CleanupStrings(verbose, db),
                new CheckAlternativeUsages(verbose, db),
                new CheckPeripherals(verbose, db),
                };

        for(int i = 0; i < allTest.length; i++)
        {
            BasicCheck curCheck = allTest[i];
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
