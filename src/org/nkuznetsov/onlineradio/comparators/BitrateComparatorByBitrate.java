package org.nkuznetsov.onlineradio.comparators;

import java.util.Comparator;

import org.nkuznetsov.onlineradio.classes.Bitrate;

public class BitrateComparatorByBitrate implements Comparator<Bitrate>
{
	@Override
	public int compare(Bitrate arg0, Bitrate arg1)
	{
		int res = ((Integer)arg0.getBitrate().length()).compareTo(arg1.getBitrate().length());
		if (res != 0) return res;
		return arg0.getBitrate().compareTo(arg1.getBitrate());
	}
}
