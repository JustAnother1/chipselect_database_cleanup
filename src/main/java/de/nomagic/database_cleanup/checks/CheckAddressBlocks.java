package de.nomagic.database_cleanup.checks;

import de.nomagic.database_cleanup.DataBaseWrapper;

public class CheckAddressBlocks extends BasicCheck 
{

	public CheckAddressBlocks(boolean verbose, DataBaseWrapper db) 
	{
		super(verbose, db);
	}

	@Override
	public boolean execute() 
	{
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public String getName() 
	{
		return "Address Block";
	}

}
