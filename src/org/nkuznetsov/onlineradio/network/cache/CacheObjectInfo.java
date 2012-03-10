package org.nkuznetsov.onlineradio.network.cache;

import java.io.File;

public class CacheObjectInfo 
{
	private String name;
	private File path;
	private long date;
	private long size;
	
	public CacheObjectInfo(String name, File path, long date, long size)
	{
		this.name = name;
		this.path = path;
		this.date = date;
		this.size = size;
	}
	
	public String getName()
	{
		return name;
	}
	
	public File getPath()
	{
		return path;
	}
	
	public long getDate()
	{
		return date;
	}
	
	public long getSize()
	{
		return size;
	}
}
