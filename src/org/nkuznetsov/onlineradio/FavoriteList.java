package org.nkuznetsov.onlineradio;

import java.util.Vector;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class FavoriteList
{	
	private static final String FAVORITES_KEY = "favorites";
	private static final String FAVORITES_SEPARATOR = ",";
	
	private static Vector<String> favorites = new Vector<String>();
	private static SharedPreferences preferences;
	
	public static void init(Context context)
	{
		preferences = PreferenceManager.getDefaultSharedPreferences(context);
		if (preferences.contains(FAVORITES_KEY))
		{
			String[] tmpFavs = preferences.getString(FAVORITES_KEY, "")
					.split(FAVORITES_SEPARATOR);
			for (String tmpFav : tmpFavs) favorites.add(tmpFav);
		}
	}
	
	private static void save()
	{
		String tmpFav = "";
		
		if (favorites.size() > 0)
		{
			tmpFav = tmpFav.concat(favorites.get(0));
			
			for (int i = 1; i < favorites.size(); i++)
			{
				tmpFav = tmpFav.concat(FAVORITES_SEPARATOR);
				tmpFav = tmpFav.concat(favorites.get(i));
			}
		}
		
		SharedPreferences.Editor editor = preferences.edit();
		
		if (tmpFav.equals("")) editor.remove(FAVORITES_KEY);
		else editor.putString(FAVORITES_KEY, tmpFav);
		
		editor.commit();
	}
	
	public static boolean isFavorite(String id)
	{
		return favorites.contains(id);
	}
	
	public static void add(String id)
	{
		if (!isFavorite(id)) 
		{
			favorites.add(id);
			save();
		}
	}
	
	public static void remove(String id)
	{
		while (isFavorite(id)) favorites.remove(id);
		save();
	}
}