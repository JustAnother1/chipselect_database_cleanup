package de.nomagic.database_cleanup.checks.helpers;

public class Field implements NamedRange
{
    private final int id;
    private final int offset;
    private final int size;
    private final String name;

    public Field(int id, String name, int offset, int size)
    {
        this.name = name;
        this.id = id;
        this.offset = offset;
        this.size = size;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public int getOffset()
    {
        return offset;
    }

    @Override
    public int getSize()
    {
        return size;
    }

    @Override
    public int getId()
    {
        return id;
    }

}
