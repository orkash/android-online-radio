package org.nkuznetsov.onlineradio;

import java.util.HashSet;
import java.util.Set;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Build;
import android.preference.PreferenceManager;
import android.text.TextUtils;

public class FavoriteList
{	
	private static final String FAVORITES_KEY_10 = "favorites";
	private static final String FAVORITES_SEPARATOR = ",";
	
	private static final String FAVORITES_KEY_11 = "favoritesSET";
	
	private static final Set<String> favorites = new HashSet<String>();
	private static SharedPreferences preferences;
	
	public static void init(Context context)
	{
		preferences = PreferenceManager.getDefaultSharedPreferences(context);
		
		if (preferences.contains(FAVORITES_KEY_10)) 
		{
			String[] favs = preferences.getString(FAVORITES_KEY_10, "").split(FAVORITES_SEPARATOR);	
			for (String fav : favs) favorites.add(fav);
		}
		
		if (Build.VERSION.SDK_INT >= 11 && preferences.contains(FAVORITES_KEY_11)) 
			favorites.addAll(preferences.getStringSet(FAVORITES_KEY_11, new HashSet<String>()));
	}
	
	private static void save()
	{
		Editor editor = preferences.edit();
		
		if (favorites.size() > 0)
		{
			if (Build.VERSION.SDK_INT < 11) editor.putString(FAVORITES_KEY_10, TextUtils.join(FAVORITES_SEPARATOR, favorites));
			else editor.putStringSet(FAVORITES_KEY_11, favorites);
		}
		else 
		{
			editor.remove(FAVORITES_KEY_11);
			editor.remove(FAVORITES_KEY_10);
		}
		
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