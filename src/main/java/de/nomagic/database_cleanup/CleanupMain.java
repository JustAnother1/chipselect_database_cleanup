package de.nomagic.database_cleanup;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import de.nomagic.database_cleanup.checks.BasicCheck;
import de.nomagic.database_cleanup.checks.CheckAddressBlocks;
import de.nomagic.database_cleanup.checks.CheckAlternativeUsages;
import de.nomagic.database_cleanup.checks.CheckPeripherals;
import de.nomagic.database_cleanup.checks.CleanupStrings;
import de.nomagic.database_cleanup.checks.RAMandFlashSizes;
import de.nomagic.database_cleanup.checks.RemoveOrphans;

public class CleanupMain
{
    private final Logger log = LoggerFactory.getLogger(this.getClass().getName());
    private String dbLocation;
    private String dbUser;
    private String dbPassword;
    private boolean dryRun = false;
    private boolean listTests = false;
    // tests
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

    public static String getCommitID()
    {
        try
        {
            final InputStream s = CleanupMain.class.getResourceAsStream("/git.properties");
            final BufferedReader in = new BufferedReader(new InputStreamReader(s));

            String id = "";

            String line = in.readLine();
            while(null != line)
            {
                if(line.startsWith("git.commit.id.full"))
                {
                    id = line.substring(line.indexOf('=') + 1);
                }
                line = in.readLine();
            }
            in.close();
            s.close();
            return id;
        }
        catch( Exception e )
        {
            return e.toString();
        }
    }

    private void startLogging(final String[] args)
    {
        boolean colour = true;
        int numOfV = 0;
        for(int i = 0; i < args.length; i++)
        {
            if(true == "-v".equals(args[i]))
            {
                numOfV ++;
            }
            // -noColour
            if(true == "-noColour".equals(args[i]))
            {
                colour = false;
            }
        }

        // configure Logging
        // Levels are ERROR - WARN - INFO - DEBUG - TRACE
        switch(numOfV)
        {
        case 0: setLogLevel("warn", colour); break;
        case 1: setLogLevel("info", colour);break;
        case 2: setLogLevel("debug", colour);break;
        default:
            setLogLevel("trace", colour);
            log.trace("Build from {}", getCommitID());
            break;
        }
    }

    private void setLogLevel(String LogLevel, boolean colour)
    {
        final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        try
        {
            final JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(context);
            context.reset();
            final String logCfg;
            if(true == colour)
            {
                logCfg =
                "<configuration>" +
                  "<appender name='STDERR' class='ch.qos.logback.core.ConsoleAppender'>" +
                  "<target>System.err</target>" +
                    "<encoder>" +
                       "<pattern>%date{HH:mm:ss.SSS} %highlight(%-5level) [%36.36logger] %msg%n</pattern>" +
                    "</encoder>" +
                  "</appender>" +
                  "<root level='" + LogLevel + "'>" +
                    "<appender-ref ref='STDERR' />" +
                  "</root>" +
                "</configuration>";
            }
            else
            {
                logCfg =
                "<configuration>" +
                  "<appender name='STDERR' class='ch.qos.logback.core.ConsoleAppender'>" +
                  "<target>System.err</target>" +
                    "<encoder>" +
                      "<pattern>%date{HH:mm:ss.SSS} %-5level [%36.36logger] %msg%n</pattern>" +
                    "</encoder>" +
                  "</appender>" +
                  "<root level='" + LogLevel + "'>" +
                    "<appender-ref ref='STDERR' />" +
                  "</root>" +
                "</configuration>";
            }
            ByteArrayInputStream bin;
            bin = new ByteArrayInputStream(logCfg.getBytes(StandardCharsets.UTF_8));
            configurator.doConfigure(bin);
        }
        catch (JoranException je)
        {
          // StatusPrinter will handle this
        }
        StatusPrinter.printInCaseOfErrorsOrWarnings(context);
    }

    public void printHelp()
    {
        System.out.println("Parameters:");
        System.out.println("-h / --help                : print this message.");
        System.out.println("-v                         : verbose output for even more messages use -v -v or even -v -v -v");
        System.out.println("-noColour                  : do not highlight the output.");
        System.out.println("-db_host <database host>   : the location of the db server");
        System.out.println("-db_user <user name>       : the databse user to use");
        System.out.println("-db_password <password>    : the password of the databse user");
        System.out.println("-test <test name>          : execute the named test ");
        System.out.println("                           : (this can appear more than once to do many different tests)");
        System.out.println("-dry-run                   : do not change data on the server.");
        System.out.println("-list                      : list all possible tests ");
    }

    public void printListofTests()
    {
        System.out.println("all possible tests:");
        System.out.println("all                      : execute all possible tests");
        System.out.println("RAM                      : test RAM settings");
        System.out.println("Orphans                  : elements that are not linked");
        System.out.println("cleanup                  : check for strange characters in strings");
        System.out.println("alt                      : vendor name alternatives");
        System.out.println("peripherals              : peripheral settings");
        System.out.println("address_block            : address blocks");
    }

    private boolean parseCommandLineParameters(String[] args)
    {
        boolean has_host = false;
        boolean has_user = false;
        boolean has_password = false;

        if(null == args)
        {
            return false;
        }
        for(int i = 0; i < args.length; i++)
        {
            if(true == args[i].startsWith("-"))
            {
                if(true == "-h".equals(args[i]))
                {
                    return false;
                }
                else if(true == "-v".equals(args[i]))
                {
                    // already handled -> ignore
                }
                else if(true == "-noColour".equals(args[i]))
                {
                    // already handled -> ignore
                }
                else if(true == "-db_host".equals(args[i]))
                {
                    i++;
                    if(i == args.length)
                    {
                        System.err.println("ERROR: missing parameter for " + args[i-1]);
                        return false;
                    }
                    dbLocation = args[i];
                    has_host = true;
                }
                else if(true == "-db_user".equals(args[i]))
                {
                    i++;
                    if(i == args.length)
                    {
                        System.err.println("ERROR: missing parameter for " + args[i-1]);
                        return false;
                    }
                    dbUser = args[i];
                    has_user = true;
                }
                else if(true == "-db_password".equals(args[i]))
                {
                    i++;
                    if(i == args.length)
                    {
                        System.err.println("ERROR: missing parameter for " + args[i-1]);
                        return false;
                    }
                    dbPassword = args[i];
                    has_password = true;
                }
                else if(true == "-test".equals(args[i]))
                {
                    i++;
                    if(i == args.length)
                    {
                        System.err.println("ERROR: missing parameter for " + args[i-1]);
                        return false;
                    }
                    switch(args[i])
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
                        System.err.println("invalid test specification (" + args[i] + ") !");
                        allTests = false;
                        break;
                    }
                }
                else if(true == "-dry-run".equals(args[i]))
                {
                    dryRun = true;
                }
                else if(true == "-list".equals(args[i]))
                {
                    listTests = true;
                }
                // new parameters go here
            }
            else
            {
                System.err.println("Invalid option : " + args[i]);
                return false;
            }
        }

        if((false == has_host) || (false == has_user) || (false == has_password))
        {
            return false;
        }
        return true;
    }

    public boolean execute()
    {
        boolean run = true;
        int global_comparisions = 0;
        int global_fixes = 0;
        int global_inconsistencies = 0;
        Vector<BasicCheck> allTest = new Vector<BasicCheck>();

        DataBaseWrapper db = new DataBaseWrapper();
        log.trace("host :{} !", dbLocation);
        log.trace("user : {} !", dbUser);
        log.trace("password : {} !", dbPassword);
        db.connectToDataBase(dbLocation, dbUser, dbPassword);

        if(true == dryRun)
        {
            log.info("!!! DRY RUN !!!");
            log.info("There wil be no information saved to the server !");
            log.info("!!! DRY RUN !!!");
        }

        if(true == allTests)
        {
            allTest.add( new RAMandFlashSizes(db));
            allTest.add( new RemoveOrphans(db));
            allTest.add( new CleanupStrings(db));
            allTest.add( new CheckAlternativeUsages(db));
            allTest.add( new CheckPeripherals(db));
            allTest.add( new CheckAddressBlocks(db));
        }
        else
        {
            if(ramTest)
            {
                allTest.add( new RAMandFlashSizes(db));
            }
            if(orphanTest)
            {
                allTest.add( new RemoveOrphans(db));
            }
            if(stringTest)
            {
                allTest.add( new CleanupStrings(db));
            }
            if(altUsageTest)
            {
                allTest.add( new CheckAlternativeUsages(db));
            }
            if(peripheralsTest)
            {
                allTest.add( new CheckPeripherals(db));
            }
            if(addressBlockTest)
            {
                 allTest.add( new CheckAddressBlocks(db));
            }
        }

        for(int i = 0; i < allTest.size(); i++)
        {
            BasicCheck curCheck = allTest.get(i);
            System.out.println("Now starting test " + curCheck.getName() + " ...");
            run = curCheck.execute(dryRun);
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

        return true;
    }

    public static void main(String[] args)
    {
        CleanupMain cm = new CleanupMain();
        cm.startLogging(args);
        if(true == cm.parseCommandLineParameters(args))
        {
            if(true == cm.execute())
            {
                // OK
                System.out.println("Done!");
                System.exit(0);
            }
            else
            {
                // ERROR
                System.err.println("ERROR: Something went wrong!");
                System.exit(1);
            }
        }
        else
        {
            if(true == cm.listTests)
            {
                cm.printListofTests();
                System.exit(0);
            }
            else
            {
                cm.printHelp();
                System.exit(1);
            }
        }
    }

}
