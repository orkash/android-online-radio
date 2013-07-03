package org.nkuznetsov.onlineradio.comparators;

import java.util.Comparator;

import org.nkuznetsov.onlineradio.OwnList;
import org.nkuznetsov.onlineradio.classes.Station;

public class StationsComparatorByOwn implements Comparator<Station>
{
	@Override
	public int compare(Station lhs, Station rhs)
	{		
		Boolean l = OwnList.isOwn(lhs.getId());
		Boolean r = OwnList.isOwn(rhs.getId());
		return r.compareTo(l);
	}
}
