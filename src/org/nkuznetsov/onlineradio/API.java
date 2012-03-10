package org.nkuznetsov.onlineradio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.nkuznetsov.onlineradio.classes.Bitrate;
import org.nkuznetsov.onlineradio.classes.Station;
import org.nkuznetsov.onlineradio.exceptions.BadGatewayException;
import org.nkuznetsov.onlineradio.exceptions.GatewayTimeoutException;
import org.nkuznetsov.onlineradio.exceptions.ServerErrorException;
import org.nkuznetsov.onlineradio.network.DownloadManager;

public class API 
{
	private static final String HTTP_HOST = "http://audio.rambler.ru";
	private static final String URL_STATIONS = HTTP_HOST + "/json/stations.js";
	
	public static String getStream(String url) throws BadGatewayException, GatewayTimeoutException, ServerErrorException, IOException
	{
		InputStream is = DownloadManager.getStreamHttpGETRequest(url);
		BufferedReader reader = new BufferedReader(new InputStreamReader(is, "utf-8"), 8 * 1024);
		return reader.readLine();
	}
	
	public static List<Station> getStations() throws BadGatewayException, GatewayTimeoutException, ServerErrorException, IOException, PatternSyntaxException, JSONException
	{
		List<Station> stations = new ArrayList<Station>();
		
		InputStream is = DownloadManager.getStreamHttpGETRequest(URL_STATIONS);
	
		if (is == null) throw new ServerErrorException();
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(is, "utf-8"), 8 * 1024);
		StringBuilder sb = new StringBuilder();
		String line = null;
		while ((line = reader.readLine()) != null) 
		{
			sb.append(line + "\n");
		}
		is.close();
		
		Pattern pattern = Pattern.compile("(\\[.*\\])");
		Matcher matcher = pattern.matcher(sb.toString());
		if (matcher.find())
		{
			JSONArray json = new JSONArray(matcher.group(1));
			
			for (int i = 0; i < json.length(); i++)
			{
				JSONObject json_staticon = json.getJSONObject(i);
				
				JSONObject json_bitrates = json_staticon.getJSONObject("bitrates");
				
				List<Bitrate> bitrates = new ArrayList<Bitrate>();
			
				@SuppressWarnings("unchecked")
				Iterator<String> keys = json_bitrates.keys();
				while (keys.hasNext())
				{
					String bitrate = keys.next();
					bitrates.add(new Bitrate(bitrate, json_bitrates.getString(bitrate)));
				}
				
				stations.add(new Station(json_staticon.getString("id"), 
										json_staticon.getString("name"), 
										json_staticon.getString("icon"), 
										bitrates));
			}
		}
		
		return stations;
	}
}
