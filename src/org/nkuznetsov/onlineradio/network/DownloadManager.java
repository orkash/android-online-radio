package org.nkuznetsov.onlineradio.network;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.nkuznetsov.onlineradio.exceptions.BadGatewayException;
import org.nkuznetsov.onlineradio.exceptions.GatewayTimeoutException;
import org.nkuznetsov.onlineradio.exceptions.ServerErrorException;
import org.nkuznetsov.onlineradio.network.cache.CacheManager;
import org.nkuznetsov.onlineradio.utils.IOUtils;
import org.nkuznetsov.onlineradio.utils.MD5;

import android.util.Log;

import java.io.ByteArrayInputStream;

public class DownloadManager 
{
	public static CacheManager cacheManager;
	
	public static InputStream getStreamHttpGETRequest(String url, int chaceExpires) throws FileNotFoundException, BadGatewayException, GatewayTimeoutException, ServerErrorException
	{
		if (chaceExpires > 0 && cacheManager != null && cacheManager.exists(MD5.MD5Hash(url))) 
		{
			try
			{
				return cacheManager.get(MD5.MD5Hash(url));
			}
			catch (Exception e) {}
		}
		if (chaceExpires > 0 && cacheManager != null)
		{
			try
			{
				InputStream is = executeRequest(new HttpGet(url));
				return new ByteArrayInputStream(cacheManager.put(MD5.MD5Hash(url), System.currentTimeMillis() / 1000L + chaceExpires, IOUtils.readBytes(is)));
			}
			catch (Exception e) 
			{
				Log.e("DownloadManager", e.toString());
			}
		}
		return executeRequest(new HttpGet(url));
	}
	
	public static InputStream getStreamHttpGETRequest(String url) throws FileNotFoundException, BadGatewayException, GatewayTimeoutException, ServerErrorException
	{

	    return executeRequest(new HttpGet(url));
	}
	
	public static InputStream getStreamHttpPOSTRequest(String url, HashMap<String, String> params)
	{
        HttpPost request = new HttpPost(url);
        if (params != null)
        {
	        HttpParams p = new BasicHttpParams();
	        for (String key : params.keySet()) 
	        	p.setParameter(key, params.get(key));
	        request.setParams(p);
        }
        return executeRequest(request);
	}
	
	private static InputStream executeRequest(HttpUriRequest request)
	{
		InputStream is = null;
		try
		{
			HttpResponse response = new DefaultHttpClient().execute(request);
			
	        switch(response.getStatusLine().getStatusCode())
	        {
	        	case HttpStatus.SC_OK:
	        		is = response.getEntity().getContent();
	        		break;
	        	case HttpStatus.SC_BAD_GATEWAY:
		        	throw new BadGatewayException();
		        case HttpStatus.SC_GATEWAY_TIMEOUT:
		        	throw new GatewayTimeoutException();
		        case HttpStatus.SC_NOT_FOUND:
		        	throw new FileNotFoundException();
		        case HttpStatus.SC_INTERNAL_SERVER_ERROR:
		        	throw new ServerErrorException();
	        }
		}
		catch (Exception e) {}
		return is;
	}
}
