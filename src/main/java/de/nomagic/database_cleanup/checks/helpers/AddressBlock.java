package de.nomagic.database_cleanup.checks.helpers;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

import de.nomagic.database_cleanup.DataBaseWrapper;

public class AddressBlock 
{
	private final int id;
	private String address_offset = "";
	private String size = "";
	private String mem_usage = "";
	private String protection = "";

	public AddressBlock(int id, DataBaseWrapper db) 
	{
		this.id = id;
        String sql = "SELECT address_offset, size, mem_usage, protection FROM p_address_block WHERE id =" + id;
        ResultSet rs;
		try {
			rs = db.executeQuery(sql);
	        rs.first();
	    	address_offset = rs.getString(1);
	    	size = rs.getString(2);
	    	mem_usage = rs.getString(3);
	    	protection = rs.getString(4);
		} 
		catch (SQLException e) 
		{
			e.printStackTrace();
		}
	}

	@Override
	public int hashCode() 
	{
		return Objects.hash(address_offset, mem_usage, protection, size);
	}

	@Override
	public boolean equals(Object obj) 
	{
		if (this == obj)
		{
			return true;
		}
		if (obj == null)
		{	
			return false;
		}
		if (getClass() != obj.getClass())
		{
			return false;
		}
		AddressBlock other = (AddressBlock) obj;
		return     Objects.equals(address_offset, other.address_offset) 
				&& Objects.equals(mem_usage, other.mem_usage)
				&& Objects.equals(protection, other.protection) 
				&& Objects.equals(size, other.size);
	}

	@Override
	public String toString() 
	{
		return "[" + address_offset + ", " + size + ", " + mem_usage + ", " + protection + "]";
	}

	public int getId() 
	{
		return id;
	}

}
