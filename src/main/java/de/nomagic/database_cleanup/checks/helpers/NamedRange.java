package de.nomagic.database_cleanup.checks.helpers;

public interface NamedRange
{
    String getName();
    int getOffset();
    int getSize();
    int getId();
}
