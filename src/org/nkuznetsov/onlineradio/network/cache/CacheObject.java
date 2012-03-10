package org.nkuznetsov.onlineradio.network.cache;

import java.io.Serializable;

public class CacheObject implements Serializable
{
	private static final long serialVersionUID = -6349913088967782233L;
	
	private long expires;
	private byte[] bytes;
	
	public CacheObject(long expires, byte[] data)
	{
		this.expires = expires;
		this.bytes = data;
	}
	
	public long getExpires()
	{
		return expires;
	}
	
	public boolean isExpired()
	{
		if (expires <= System.currentTimeMillis() / 1000L) return true;
		else return false;
	}

	
	public byte[] getData()
	{
		return bytes;
	}
	
	public long getDataLength()
	{
		return bytes.length;
	}
}
