package de.nomagic.database_cleanup.checks;

import de.nomagic.database_cleanup.DataBaseWrapper;

public abstract class BasicCheck
{
    protected final boolean verbose;
    protected int comparisons = 0;
    protected int fixes = 0;
    protected int inconsistencies = 0;
    protected final DataBaseWrapper db;

    public BasicCheck(boolean verbose, DataBaseWrapper db)
    {
        this.verbose = verbose;
        this.db = db;
    }

    public abstract boolean execute();

    public int getNumberComparissons()
    {
        return comparisons;
    }

    public int getFixes()
    {
        return fixes;
    }

    public int getInconsistencies()
    {
        return inconsistencies;
    }

}
