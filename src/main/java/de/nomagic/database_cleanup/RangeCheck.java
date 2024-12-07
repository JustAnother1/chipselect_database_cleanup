package de.nomagic.database_cleanup;

import de.nomagic.database_cleanup.checks.helpers.NamedRange;

public class RangeCheck
{
    private final int size;
    private String[] used;
    private boolean out_of_bounds = false;
    private boolean overlap_error = false;
    private String errorMessage = "";

    public RangeCheck(int size)
    {
        this.size = size;
        used = new String[size];
        for(int i = 0; i < size; i++)
        {
            used[i] = null;
        }
    }

    public void add(NamedRange range)
    {
        if(range.getOffset() >= size)
        {
            // we start already out of bounds -> error
            out_of_bounds = true;
            errorMessage = errorMessage + "The " + range.getName()
                    + "(start: " + range.getOffset() + ","
                    + " size : " + range.getSize() + ")"
                    + " does not fit!\r\n";
        }
        else
        {
            // mark all bits as used
            for(int i = 0; i < range.getSize(); i++)
            {
                if(range.getOffset() + i < size)
                {
                    if(null != used[range.getOffset() + i])
                    {
                        // this position is already used -> error
                        overlap_error = true;
                        errorMessage = errorMessage + "The " + range.getName() + "(" + range.getId() + ")"
                                + "(start: " + range.getOffset() + ","
                                + " size : " + range.getSize() + ")"
                                + " collides with the " + used[range.getOffset() + i] + "\r\n";
                    }
                    else
                    {
                        // mark this bit as used.
                        used[range.getOffset() + i] = range.getName();
                    }
                }
                else
                {
                    // added area does not fit completely -> error
                    out_of_bounds = true;
                    errorMessage = errorMessage + "The " + range.getName()
                            + "(start: " + range.getOffset() + ","
                            + " size : " + range.getSize() + ")"
                            + " does not fit!\r\n";
                    break;
                }
            }

        }
    }

    public String getErrorMessage()
    {
        return errorMessage;
    }

    public boolean hasError()
    {
        if((true == out_of_bounds) || (true == overlap_error))
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    public boolean hasCollision()
    {
        return overlap_error;
    }

    public boolean hasOutOfBoundsError()
    {
        return out_of_bounds;
    }

}
