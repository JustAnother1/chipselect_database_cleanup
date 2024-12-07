package de.nomagic.database_cleanup.checks;

import de.nomagic.database_cleanup.DataBaseWrapper;

public abstract class BasicCheck
{
    protected int comparisons = 0;
    protected int fixes = 0;
    protected int inconsistencies = 0;
    protected final DataBaseWrapper db;

    public BasicCheck(DataBaseWrapper db)
    {
        this.db = db;
    }

    public abstract boolean execute(boolean dryRun);
    public abstract String getName();

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
