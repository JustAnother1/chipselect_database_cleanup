package de.nomagic.database_cleanup;

import java.util.Vector;

public class RangeCheck
{
    private final int size;
    private String[] used;
    private boolean out_of_bounds = false;
    private boolean overlap_error = false;
    private String errorMessage = "";
    private Vector<String> fields = new Vector<String>();


    public RangeCheck(int size)
    {
        this.size = size;
        used = new String[size];
        for(int i = 0; i < size; i++)
        {
            used[i] = null;
        }
    }


    public void add(int start_offset, int length, String fieldName)
    {
        if(start_offset >= size)
        {
            // we start already out of bounds -> error
            out_of_bounds = true;
            errorMessage = errorMessage + "The field " + fieldName + "(start: " + start_offset + ", size : " + length + ") does not fit the register! ";
        }
        else
        {
            // mark all bits as used
            for(int i = 0; i < length; i++)
            {
                if(start_offset + i < size)
                {
                    if(null != used[start_offset + i])
                    {
                        // this position is already used -> error
                        overlap_error = true;
                        errorMessage = errorMessage + "The field " + fieldName + "(start: " + start_offset + ", size : " + length + ") collides with the field " + used[start_offset + i] + " ";
                    }
                    else
                    {
                        // mark this bit as used.
                        used[start_offset + i] = fieldName;
                    }
                }
                else
                {
                    // added area does not fit completely -> error
                    out_of_bounds = true;
                    errorMessage = errorMessage + "The field " + fieldName + "(start: " + start_offset + ", size : " + length + ") does not fit the register! ";
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
