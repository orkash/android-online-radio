package org.nkuznetsov.onlineradio;

import java.net.URL;

public class Utils
{
	public static boolean isCorrectUrl(String url)
	{
		try
		{
			new URL(url);
		}
		catch (Exception e)
		{
			return false;
		}
		
		return true;
	}
}
