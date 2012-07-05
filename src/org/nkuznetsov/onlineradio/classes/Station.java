package org.nkuznetsov.onlineradio.classes;

import java.io.Serializable;
import java.util.List;

public class Station implements Serializable
{
	private static final long serialVersionUID = 2268148290535905983L;
	
	private String id;
	private String name;
	private String icon;
	private List<Bitrate> bitrates;
	
	public Station(String id, String name, String icon, List<Bitrate> bitrates)
	{
		this.id = id;
		this.name = name;
		this.icon = icon;
		this.bitrates = bitrates;
	}
	
	public String getId() 
	{
		return id;
	}
	
	public String getName() 
	{
		return name;
	}
	
	public String getIcon() 
	{
		return icon;
	}
	
	public List<Bitrate> getBitrates() 
	{
		return bitrates;
	}
	
	@Override
	public int hashCode()
	{
		int hash = 27;
		hash = 9 * hash + id.hashCode();
		hash = 9 * hash + name.hashCode();
		hash = 9 * hash + icon.hashCode();
		hash = 9 * hash + bitrates.hashCode();
		return hash;
	}
}