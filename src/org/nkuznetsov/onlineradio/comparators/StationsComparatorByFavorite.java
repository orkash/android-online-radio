package org.nkuznetsov.onlineradio.comparators;

import java.util.Comparator;

import org.nkuznetsov.onlineradio.FavoriteList;
import org.nkuznetsov.onlineradio.classes.Station;

public class StationsComparatorByFavorite implements Comparator<Station>
{
	@Override
	public int compare(Station lhs, Station rhs)
	{		
		Boolean l = FavoriteList.isFavorite(lhs.getId());
		Boolean r = FavoriteList.isFavorite(rhs.getId());
		return r.compareTo(l);
	}
}
