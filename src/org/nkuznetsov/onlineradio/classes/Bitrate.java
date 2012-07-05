package org.nkuznetsov.onlineradio.classes;

import java.io.Serializable;

public class Bitrate implements Serializable
{
	private static final long serialVersionUID = 5134357113029042219L;
	
	private String bitrate;
	private String url;
	
	public Bitrate(String bitrate, String url)
	{
		this.bitrate = bitrate;
		this.url = url;
	}
	
	public String getBitrate() 
	{
		return bitrate;
	}
	
	public String getUrl() 
	{
		return url;
	}
	
	@Override
	public int hashCode()
	{
		int hash = 61;
		hash = 17 * hash + bitrate.hashCode();
		hash = 17 * hash + url.hashCode();
		return hash;
	}
}