package org.nkuznetsov.onlineradio.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.StringTokenizer;
import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.message.BasicHttpResponse;
import android.util.Log;

public class StreamProxy implements Runnable 
{
	private int port = 0;
	
	public int getPort() 
	{
		return port;
	}

	private boolean isRunning = true;
	private ServerSocket socket;
	private Thread thread;

	public void init() 
	{
		try 
		{
			socket = new ServerSocket(port, 0, InetAddress.getLocalHost());
			socket.setSoTimeout(5000);
			port = socket.getLocalPort();
			Log.d(getClass().getName(), "port " + port + " obtained");
		} 
		catch (UnknownHostException e) 
		{
			Log.e(getClass().getName(), "Error initializing server", e);
		} 
		catch (IOException e) 
		{
			Log.e(getClass().getName(), "Error initializing server", e);
		}
	}

	public void start() 
	{
		thread = new Thread(this);
		thread.start();
	}

	public void stop() 
	{
		isRunning = false;
		thread.interrupt();
		try 
		{
			thread.join(5000);
		} 
		catch (InterruptedException e) 
		{
			e.printStackTrace();
		}
	}

	public void run() 
	{
		Log.d(getClass().getName(), "running");
		while (isRunning) 
		{
			try 
			{
				Socket client = socket.accept();
				if (client == null) continue;
				Log.d(getClass().getName(), "client connected");
				HttpRequest request = readRequest(client);
				processRequest(request, client);
			}
			catch(java.net.UnknownHostException e1)
			{
				Log.d("MS","asdasd");  
			}
			catch (SocketTimeoutException e) {} 
			catch (IOException e) 
			{
				Log.e(getClass().getName(), "Error connecting to client", e);
			}
		}
		Log.d(getClass().getName(), "Proxy interrupted. Shutting down.");
	}

	private HttpRequest readRequest(Socket client) 
	{
		HttpRequest request = null;
		InputStream is;
		String firstLine;
		try 
		{
			is = client.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(is));
			firstLine = reader.readLine();
		} 
		catch (IOException e) 
		{
			Log.e(getClass().getName(), "Error parsing request", e);
			return request;
		}

		StringTokenizer st = new StringTokenizer(firstLine);
		String method = st.nextToken();
		String uri = st.nextToken();
		Log.d(getClass().getName(), uri);
		String realUri = uri.substring(1);
		Log.d(getClass().getName(), realUri);
		request = new BasicHttpRequest(method, realUri);
		return request;
	}

	private HttpResponse download(String url) 
	{
		DefaultHttpClient seed = new DefaultHttpClient();
		SchemeRegistry registry = new SchemeRegistry();
		registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
		SingleClientConnManager mgr = new SingleClientConnManager(seed.getParams(), registry);
		DefaultHttpClient http = new DefaultHttpClient(mgr, seed.getParams());
		HttpGet method = new HttpGet(url);
		HttpResponse response = null;
		try 
		{
			Log.d(getClass().getName(), "starting download");
			response = http.execute(method);
			Log.d(getClass().getName(), "downloaded");   
		}
		catch(java.net.UnknownHostException e)
		{
			Log.e(getClass().getName(), "Error downloading", e);
		}
		catch (ClientProtocolException e) 
		{
			Log.e(getClass().getName(), "Error downloading", e);
		} 
		catch (IOException e) 
		{
			Log.e(getClass().getName(), "Error downloading", e);
		}
		return response;
	}

	private void processRequest(HttpRequest request, Socket client)
			throws IllegalStateException, IOException 
	{
		if (request == null) return;
		Log.d(getClass().getName(), "processing");
		String url = request.getRequestLine().getUri();
		HttpResponse realResponse = download(url);
		if (realResponse == null) return;

		Log.d(getClass().getName(), "downloading...");

		InputStream data = realResponse.getEntity().getContent();
		StatusLine line = realResponse.getStatusLine();
		HttpResponse response = new BasicHttpResponse(line);
		response.setHeaders(realResponse.getAllHeaders());

		Log.d(getClass().getName(), "reading headers");
		StringBuilder httpString = new StringBuilder();
		httpString.append(response.getStatusLine().toString());
 
		httpString.append("\n");
		for (Header h : response.getAllHeaders()) 
			httpString.append(h.getName()).append(": ").append(h.getValue()).append("\n");
		httpString.append("\n");
		Log.d(getClass().getName(), "headers done");
		//Log.d("MS","Response "+httpString);
		try 
		{
			byte[] buffer = httpString.toString().getBytes();
			int readBytes = -1;
			Log.d(getClass().getName(), "writing to client");
			client.getOutputStream().write(buffer, 0, buffer.length);

			// Start streaming content.
			byte[] buff = new byte[1024 * 50];
			while (isRunning && (readBytes = data.read(buff, 0, buff.length)) != -1)
				client.getOutputStream().write(buff, 0, readBytes);
		} 
		catch (Exception e) 
		{
			Log.e("", e.getMessage(), e);
		} 
		finally 
		{
			if (data != null) data.close();
			client.close();
  
		}
	}
}

