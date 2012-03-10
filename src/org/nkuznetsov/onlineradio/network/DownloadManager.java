package org.nkuznetsov.onlineradio.network;

import java.io.FileNotFoundException;
import java.io.InputStream;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.nkuznetsov.onlineradio.exceptions.BadGatewayException;
import org.nkuznetsov.onlineradio.exceptions.GatewayTimeoutException;
import org.nkuznetsov.onlineradio.exceptions.ServerErrorException;

public class DownloadManager 
{
	public static InputStream getStreamHttpGETRequest(String url) throws FileNotFoundException, BadGatewayException, GatewayTimeoutException, ServerErrorException
	{
	    return executeRequest(new HttpGet(url));
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
