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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.telephony.TelephonyManager;
import android.util.Log;

public class RadioService extends Service implements OnErrorListener, OnCompletionListener
{
	public final static String ACTION_START = "StartService";
	public final static String ACTION_STOP = "StopService";
	public final static String EXTRA_STRING_NOTIFICATION = "ExtraNotification";
	public final static String EXTRA_STRING_URL = "ExtraURL";
	public final static String EXTRA_STRING_URLISFINAL = "ExtraURLisFinal";
	
	public final static int STATE_STARTED = 1;
	public final static int STATE_STOPPED = 2;
	public final static int STATE_PREPARING = 3;
	
	public static int GROP;
	public static int CHILD;
	public static int STATE = STATE_STOPPED;
	public static Runnable stateChangeListener;
	
	private static final int NOTIFICATION_ID = 234231;
	
	private MediaPlayer mediaPlayer;
	private WakeLock wakeLock;
	private WifiLock wifiLock;
	
	private long timestamp;
	private ServiceReceiver serviceReceiver;
	private Intent lastIntent;
	
	@Override
	public void onCreate() 
	{
		super.onCreate();
		serviceReceiver = new ServiceReceiver(this);
	}
	
	@Override
	public IBinder onBind(Intent arg0) 
	{
		return null;
	}

	private void setState(int state)
	{
		STATE = state;
		if (stateChangeListener != null) stateChangeListener.run();
	}
	
	public void registerServiceReceiver()
	{
		unregisterServiceReceiver();
		
		IntentFilter filter = new IntentFilter();
		filter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
		filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		registerReceiver(serviceReceiver, filter);
	}
	
	public void unregisterServiceReceiver()
	{
		try
		{
			unregisterReceiver(serviceReceiver);
		}
		catch (Exception e) {}
	}
	
	@SuppressWarnings("deprecation")
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
		if (wifiLock == null)
		{
			WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
			int lockMode = (Build.VERSION.SDK_INT >= 12) ? WifiManager.WIFI_MODE_FULL_HIGH_PERF : WifiManager.WIFI_MODE_FULL;
			wifiLock = wm.createWifiLock(lockMode, String.valueOf(NOTIFICATION_ID));
		}
		if (!wifiLock.isHeld()) wifiLock.acquire();
		if (wakeLock == null)
		{
			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, String.valueOf(NOTIFICATION_ID));
		}
		if (!wakeLock.isHeld()) wakeLock.acquire();
	}
	
	private void unlock()
	{
		if (wifiLock != null && wifiLock.isHeld()) wifiLock.release();
		if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
	}
	
	private void stopPlayback()
	{
		timestamp = System.currentTimeMillis();
		if (mediaPlayer != null)
		{
			mediaPlayer.release();
			mediaPlayer = null;
		}
		setState(STATE_PREPARING);
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
				registerServiceReceiver();
				if (serviceReceiver.isAllowPlay(this)) new PrepareAndPlay(intent).start();
				else stopPlayback();
				return START_STICKY;
			}
			
			if (intent.getAction().equals(ACTION_STOP))
			{
				stopPlayback();
				unregisterServiceReceiver();
				unlock();
				stopForeground(true);
				stopSelf();
				setState(STATE_STOPPED);
				return START_NOT_STICKY;
			}
		}
		return START_NOT_STICKY;
	}
	
	private class PrepareAndPlay extends Thread
	{
		private Intent intent;
		
		public PrepareAndPlay(Intent intent)
		{
			this.intent = intent;
		}
		
		@Override
		public void run() 
		{
			long time = timestamp;
			int r = 1;
			if (mediaPlayer != null)
			{
				mediaPlayer.release();
				mediaPlayer = null;
			}
			while (mediaPlayer == null && r <= 10)
			{
				try
				{
					setState(STATE_PREPARING);
					String url = intent.getBooleanExtra(EXTRA_STRING_URLISFINAL, false) ? intent.getStringExtra(EXTRA_STRING_URL) : API.getStream(intent.getStringExtra(EXTRA_STRING_URL));
					MediaPlayer mp = MediaPlayer.create(getApplicationContext(), Uri.parse(url));
					if (time == timestamp && STATE == STATE_PREPARING)
					{
						mediaPlayer = mp;
						mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
						mediaPlayer.setOnErrorListener(RadioService.this);
						mediaPlayer.setOnCompletionListener(RadioService.this);
						mediaPlayer.start();
						setState(STATE_STARTED);
						return;
					} else return;
				}
				catch (Exception e) 
				{
					Log.e("RadioService", "Prepare error", e);
				}
				r++;
			}
			RadioService.stopService(getApplicationContext());
		}
	}
	
	private class ServiceReceiver extends BroadcastReceiver
	{		
		private boolean isConnected;
		private boolean isActiveCall;
		
		public ServiceReceiver(Context context)
		{
			isConnected = isActiveConnection(context);
			isActiveCall = isActiveCall(context);
		}
		
		@Override
		public void onReceive(Context context, Intent intent) 
		{
			if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION))
			{
				NetworkInfo ni = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
				boolean newIsConnected = ni.isConnected();
				if (isConnected == newIsConnected) return;
				isConnected = newIsConnected;
			}
			
			if (intent.getAction().equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED))
			{
				boolean newIsActiveCall = isActiveCall(context);
				if (isActiveCall == newIsActiveCall) return;
				isActiveCall = newIsActiveCall;
			}
			
			if (isActiveCall || !isConnected) stopPlayback();
			else if (lastIntent != null)
			{
				startService(lastIntent);
				lastIntent = null;
			}
		}
		
		public boolean isAllowPlay(Context context)
		{
			isConnected = isActiveConnection(context);
			isActiveCall = isActiveCall(context);
			return (!isActiveCall && isConnected);
		}
		
		private boolean isActiveCall(Context context)
		{
			TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
			return (tm.getCallState() == TelephonyManager.CALL_STATE_IDLE) ? false : true;
		}
		
		private boolean isActiveConnection(Context context)
		{
			ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo ni = cm.getActiveNetworkInfo();
			if (ni != null) return ni.isConnected();
			return false;
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