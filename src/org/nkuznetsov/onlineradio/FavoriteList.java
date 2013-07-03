package org.nkuznetsov.onlineradio;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

public class FavoriteList
{	
	private static final String FAVORITES_KEY = "favorites";
	private static final String FAVORITES_SEPARATOR = ",";
	
	private static final List<String> favorites = new ArrayList<String>();
	private static SharedPreferences preferences;
	
	public static void init(Context context)
	{
		preferences = PreferenceManager.getDefaultSharedPreferences(context);
		if (preferences.contains(FAVORITES_KEY))
		{
			String[] tmpFavs = 
					preferences.getString(FAVORITES_KEY, "").split(FAVORITES_SEPARATOR);
			for (String tmpFav : tmpFavs) favorites.add(tmpFav);
		}
	}
	
	private static void save()
	{
		SharedPreferences.Editor editor = preferences.edit();
		if (favorites.size() > 0)
		{
			String tmpFav = TextUtils.join(FAVORITES_SEPARATOR, favorites.toArray());
			editor.putString(FAVORITES_KEY, tmpFav);
		}
		else editor.remove(FAVORITES_KEY);
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