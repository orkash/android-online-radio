package org.nkuznetsov.onlineradio;

import java.io.PrintWriter;
import java.io.StringWriter;

import android.content.Context;

import com.google.analytics.tracking.android.ExceptionReporter;
import com.google.analytics.tracking.android.GAServiceManager;
import com.google.analytics.tracking.android.GoogleAnalytics;
import com.google.analytics.tracking.android.Tracker;

public class GA
{
	private static Tracker tracker;
	private static boolean activeSession = false;
	
	private static int activityLevel = 0;
	
	public static void init(Context context)
	{
		if (tracker == null)
		{
			GoogleAnalytics ga = GoogleAnalytics.getInstance(context);
			
			tracker = ga.getTracker(context.getString(R.string.ga_trackingId));
			
			GAServiceManager.getInstance().setDispatchPeriod(30);
			
			Thread.setDefaultUncaughtExceptionHandler(new ExceptionReporter(tracker, 
					GAServiceManager.getInstance(), 
					Thread.getDefaultUncaughtExceptionHandler(), context));
		}
	}
	
	public static void startSession()
	{
		if (!activeSession) 
		{
			tracker.setStartSession(true);
			activeSession = true;
		}
	}
	
	public static void closeSession()
	{
		if (activeSession)
		{
			GAServiceManager.getInstance().dispatch();
			activeSession = false;
		}
	}
	
	public static void startActivity(String name)
	{
		trackPage(name);
		activityLevel ++;
	}
	
	public static void stopActivity(boolean closeSessionIfLast)
	{
		activityLevel --;
		if (closeSessionIfLast && activityLevel == 0) closeSession();
	}
	
	public static void trackPage(String page)
	{
		if (!activeSession) startSession();
		tracker.sendView(page);
		GAServiceManager.getInstance().dispatch();
	}
	
	public static void trackClick(String label)
	{
		trackClick(label, 0L);
	}
	
	public static void trackClick(String label, long value)
	{
		if (!activeSession) startSession();
		tracker.sendEvent("ui_action", "click", label, value);
	}
	
	public static void trackButton(String label)
	{
		if (!activeSession) startSession();
		tracker.sendEvent("device_button", "click", label, 0L);
	}
	
	public static void trackEvent(String label)
	{
		if (!activeSession) startSession();
		tracker.sendEvent("ui_action", "event", label, 0L);
	}
	
	public static void trackFatalException(String description)
	{
		if (!activeSession) startSession();
		tracker.sendException(description, true);
		closeSession();
	}
	
	public static void trackException(String description)
	{
		if (!activeSession) startSession();
		tracker.sendException(description, false);
	}
	
	public static void trackException(Exception e)
	{
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		
		if (!activeSession) startSession();
		tracker.sendException(sw.toString(), false);
	}
}
