package org.nkuznetsov.onlineradio.comparators;

import java.util.Comparator;

import org.nkuznetsov.onlineradio.classes.Station;

public class StationsComparatorByName implements Comparator<Station>
{
	@Override
	public int compare(Station lhs, Station rhs)
	{
		return lhs.getName().compareTo(rhs.getName());
	}
}
