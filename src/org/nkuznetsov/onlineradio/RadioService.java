package org.nkuznetsov.onlineradio;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Handler;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import org.nkuznetsov.onlineradio.R;

public class RadioService extends Service implements OnErrorListener, OnCompletionListener
{
	public final static String ACTION_START = "StartService";
	public final static String ACTION_STOP = "StopService";
	public final static String EXTRA_STRING_NOTIFICATION = "ExtraNotification";
	public final static String EXTRA_STRING_URL = "ExtraURL";
	
	public final static int STATE_STARTED = 1;
	public final static int STATE_STOPPED = 2;
	public final static int STATE_PREPARING = 3;
	
	public static int GROP;
	public static int CHILD;
	public static int STATE = STATE_STOPPED;
	public static Handler StateChangeListener;
	
	private static final int NOTIFICATION_ID = 234231;
	
	private MediaPlayer mediaPlayer;
	//private WakeLock wakeLock;
	private WifiLock wifiLock;
	
	private long timestamp;
	private CallStateBroadcastReceiver callStateBroadcastReceiver;
	private Intent lastIntent;
	
	@Override
	public void onCreate() 
	{
		super.onCreate();
		callStateBroadcastReceiver = new CallStateBroadcastReceiver();
	}
	
	@Override
	public IBinder onBind(Intent arg0) 
	{
		return null;
	}

	private void setState(int state)
	{
		STATE = state;
		if (StateChangeListener != null) 
			StateChangeListener.sendMessage(StateChangeListener.obtainMessage(RadioActivity.PLAYING_STATE_CHANGED_EVENT));
	}
	
	public void registerCallStateBroadcastReceiver()
	{
		IntentFilter filter = new IntentFilter();
		filter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
		filter.addAction(Intent.ACTION_NEW_OUTGOING_CALL);
		registerReceiver(callStateBroadcastReceiver, filter);
	}
	
	public void unRegisterCallStateBroadcastReceiver()
	{
		try
		{
			unregisterReceiver(callStateBroadcastReceiver);
		}
		catch (Exception e) {}
	}
	
	private void startForeground(String text)
	{
		Intent newIntent = new Intent(getApplicationContext(), RadioActivity.class);
		newIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0, newIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		Notification notification = new Notification(R.drawable.icon, text, 0);
		notification.flags = notification.flags | Notification.FLAG_FOREGROUND_SERVICE;
		notification.setLatestEventInfo(getApplicationContext(), getString(R.string.app_name), text, pi);
		startForeground(NOTIFICATION_ID, notification);
	}
	
	private void lock()
	{
		/*
		if (wakeLock == null)
		{
			PowerManager pm = (PowerManager)getSystemService(POWER_SERVICE);
			wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, String.valueOf(NOTIFICATION_ID));
		}
		if (!wakeLock.isHeld()) wakeLock.acquire();
		*/
		if (wifiLock == null)
		{
			WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
			wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL, String.valueOf(NOTIFICATION_ID));
		}
		if (!wifiLock.isHeld()) wifiLock.acquire();
	}
	
	private void unlock()
	{
		//if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
		if (wifiLock != null && wifiLock.isHeld()) wifiLock.release();
	}
	
	private void stopPlayback()
	{
		timestamp = System.currentTimeMillis();
		if (mediaPlayer != null)
		{
			mediaPlayer.release();
			mediaPlayer = null;
		}
	}
	
	@Override
	public void onCompletion(MediaPlayer arg0) 
	{
		startService(lastIntent);
	}

	@Override
	public boolean onError(MediaPlayer arg0, int arg1, int arg2) 
	{
		startService(lastIntent);
		return false;
	}
	
	@Override
	public int onStartCommand(final Intent intent, int flags, int startId) 
	{
		if (intent != null)
		{
			if (intent.getAction().equals(ACTION_START))
			{
				lastIntent = intent;
				timestamp = System.currentTimeMillis();
				startForeground(intent.getStringExtra(EXTRA_STRING_NOTIFICATION));
				lock();
				registerCallStateBroadcastReceiver();
				new Thread()
				{
					@Override
					public void run() 
					{
						long time = timestamp;
						int r = 0;
						if (mediaPlayer != null)
						{
							mediaPlayer.release();
							mediaPlayer = null;
						}
						while (mediaPlayer == null && r < 10)
						{
							try
							{
								setState(STATE_PREPARING);
								String url = API.getStream(intent.getStringExtra(EXTRA_STRING_URL));
								MediaPlayer mp = MediaPlayer.create(getApplicationContext(), Uri.parse(url));
								if (time == timestamp && STATE == STATE_PREPARING)
								{
									mediaPlayer = mp;
									mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
									mediaPlayer.setOnErrorListener(RadioService.this);
									mediaPlayer.setOnCompletionListener(RadioService.this);
									setState(STATE_STARTED);
									mediaPlayer.start();
									return;
								} else return;
							}
							catch (Exception e) {}
							r++;
						}
						RadioService.stopService(getApplicationContext());
					}
				}.start();
			}
			if (intent.getAction().equals(ACTION_STOP))
			{
				stopPlayback();
				unRegisterCallStateBroadcastReceiver();
				unlock();
				stopForeground(true);
				stopSelf();
				setState(STATE_STOPPED);
			}
		}
		return START_STICKY;
	}
	
	private class CallStateBroadcastReceiver extends BroadcastReceiver
	{		
		@Override
		public void onReceive(Context context, Intent intent) 
		{
			// входящий вызов и сброс
			if (intent.getAction().equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED))
			{
				String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
				// входящий
				if (state.equalsIgnoreCase(TelephonyManager.EXTRA_STATE_RINGING))
				{
					stopPlayback();
				}
				
				// сброс
				if (state.equals(TelephonyManager.EXTRA_STATE_IDLE))
				{
					if (lastIntent != null)
					{
						startService(lastIntent);
						lastIntent = null;
					}
				}
			}
			
			// исходящий вызов
			if (intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL))
			{
				String number = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
				if (number != null && !number.matches("[\\*#][0-9\\*#]{2,}#"))
					stopPlayback();
			}
		}
	}
	
	public static void stopService(Context context)
	{
		if (STATE != STATE_STOPPED)
		{
			Intent newIntent = new Intent(context, RadioService.class);
			newIntent.setAction(RadioService.ACTION_STOP);
			context.startService(newIntent);
		}
	}
}