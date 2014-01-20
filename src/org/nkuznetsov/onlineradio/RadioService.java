package org.nkuznetsov.onlineradio;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
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
import android.support.v4.app.NotificationCompat;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;

public class RadioService extends Service implements OnErrorListener, OnCompletionListener
{
	public final static String ACTION_START = "StartService";
	public final static String ACTION_STOP = "StopService";
	public final static String ACTION_USERPAUSE = "PauseService";
	public final static String EXTRA_STRING_NOTIFICATION = "ExtraNotification";
	public final static String EXTRA_STRING_URL = "ExtraURL";
	public final static String EXTRA_STRING_URLISFINAL = "ExtraURLisFinal";
	
	public final static int STATE_STARTED = 1;
	public final static int STATE_STOPPED = 2;
	public final static int STATE_PREPARING = 3;
	
	public final static int STATE_AUTOPAUSED = 4;
	public final static int STATE_USERPAUSED = 5;
	
	public final static int AP_REASON_INTERNET = 1;
	public final static int AP_REASON_CALL = 2;
	public final static int AP_REASON_RECONNECT = 3;
	
	public static int GROP;
	public static int CHILD;
	public static int STATE = STATE_STOPPED;
	public static int APREASON;
	public static Runnable stateChangeListener;
	
	private static final int NOTIFICATION_ID = 234231;
	
	private MediaPlayer mediaPlayer;
	private WakeLock wakeLock;
	private WifiLock wifiLock;
	
	private long timestamp;
	private ServiceReceiver serviceReceiver;
	private MediaButtonReceiver mediaButtonReceiver;
	private Intent lastIntent;
	
	private PendingIntent mainActivityPendingIntent, 
						stopPendingIntent, pausePendingIntent,
						startPendingIntent;
	private String notificationText;
	
	@Override
	public void onCreate() 
	{
		super.onCreate();
		serviceReceiver = new ServiceReceiver();
		
		mediaButtonReceiver = new MediaButtonReceiver();
		mediaButtonReceiver.register();
		
		Intent newIntent = new Intent(this, RadioActivity.class);
		newIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		
		mainActivityPendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, newIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		
		newIntent = new Intent(this, RadioService.class);
		newIntent.setAction(ACTION_STOP);
		
		stopPendingIntent = PendingIntent.getService(this, 0, newIntent, 0);
		
		newIntent = new Intent(this, RadioService.class);
		newIntent.setAction(ACTION_USERPAUSE);
		
		pausePendingIntent = PendingIntent.getService(this, 0, newIntent, 0);
	}
	
	@Override
	public void onDestroy() 
	{
		mediaButtonReceiver.unregister();
		super.onDestroy();
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
		
		NotificationCompat.Builder nb = null;
		
		if (state == STATE_PREPARING)
		{
			nb = getDefaultNotificationBuilder();
			nb.setContentText("Подключение");
			nb.setSmallIcon(R.drawable.icon);
			nb.setProgress(0, 0, true);
		}
		
		if (state == STATE_STARTED)
		{
			nb = getDefaultNotificationBuilder();
			nb.setSmallIcon(R.drawable.ic_play);
			nb.setContentText("Воспроизведение");
			nb.addAction(R.drawable.ic_pause, "Пауза", pausePendingIntent);
		}
		
		if (state == STATE_AUTOPAUSED) 
		{
			nb = getDefaultNotificationBuilder();
			nb.setSmallIcon(R.drawable.ic_pause);
			nb.setContentText(APREASON == AP_REASON_INTERNET ? "Ожидаю подключения к Интернету" : "Ожидаю завершение вызова");
		}
		
		if (state == STATE_USERPAUSED) 
		{
			nb = getDefaultNotificationBuilder();
			nb.setSmallIcon(R.drawable.ic_pause);
			nb.setContentText("Пауза");
			nb.addAction(R.drawable.ic_play, "Продолжить", startPendingIntent);
		}
		
		if (nb != null)
		{
			nb.addAction(R.drawable.ic_stop, "Стоп", stopPendingIntent);
			
			NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
			nm.cancel(NOTIFICATION_ID);
			nm.notify(NOTIFICATION_ID, nb.build());
		}
		
		String AVRCP_PLAYSTATE_CHANGED = "com.android.music.playstatechanged";
		String AVRCP_META_CHANGED = "com.android.music.metachanged";

	    Intent i = new Intent(AVRCP_PLAYSTATE_CHANGED);
	    i.putExtra("id", Long.valueOf(1));
	    i.putExtra("artist", "Artist");
	    i.putExtra("album", "Album");
	    i.putExtra("track", "Track");
	    i.putExtra("playing", state == STATE_PREPARING || state == STATE_STARTED);        
	    sendBroadcast(i);
	}
	
	public void registerServiceReceiver()
	{
		serviceReceiver.unregister();
		serviceReceiver.register();
	}
	
	public void unregisterServiceReceiver()
	{
		serviceReceiver.unregister();
	}
	
	private NotificationCompat.Builder getDefaultNotificationBuilder()
	{
		NotificationCompat.Builder nb = new NotificationCompat.Builder(this);
		
		nb.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.icon));
		
		nb.setTicker(notificationText);
		
		nb.setContentTitle(notificationText);
		nb.setContentText("");
		nb.setContentIntent(mainActivityPendingIntent);
		
		nb.setOnlyAlertOnce(true);
		nb.setOngoing(true);
		nb.setWhen(System.currentTimeMillis());
		
		return nb;
	}
	
	@SuppressLint("NewApi")
	private void startForeground()
	{
		startForeground(NOTIFICATION_ID, getDefaultNotificationBuilder().build());
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
	
	private void stopPlayback(int newState, int apReason)
	{
		APREASON = apReason;
		stopPlayback(newState);
	}
	
	private void stopPlayback(int newState)
	{
		timestamp = System.currentTimeMillis();
		if (mediaPlayer != null)
		{
			mediaPlayer.release();
			mediaPlayer = null;
		}
		setState(newState);
	}
	
	@Override
	public void onCompletion(MediaPlayer arg0) 
	{
		if (lastIntent != null) startService(lastIntent);
	}

	@Override
	public boolean onError(MediaPlayer arg0, int arg1, int arg2) 
	{
		if (lastIntent != null) 
		{
			startService(lastIntent);
			return true;
		}
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
				startPendingIntent = PendingIntent.getService(this, 0, lastIntent, 0);
				timestamp = System.currentTimeMillis();
				notificationText = intent.getStringExtra(EXTRA_STRING_NOTIFICATION);
				startForeground();
				lock();
				registerServiceReceiver();
				if (serviceReceiver.isAllowPlay()) new PrepareAndPlay(intent).start();
				else stopPlayback(STATE_AUTOPAUSED, serviceReceiver.isActiveCall ? AP_REASON_CALL : AP_REASON_INTERNET);
				return START_STICKY;
			}
			
			if (intent.getAction().equals(ACTION_USERPAUSE))
			{
				stopPlayback(STATE_USERPAUSED);
				unregisterServiceReceiver();
				unlock();
				// TODO: set notification
			}
			
			if (intent.getAction().equals(ACTION_STOP))
			{
				lastIntent = null;
				stopPlayback(STATE_STOPPED);
				unregisterServiceReceiver();
				unlock();
				stopForeground(true);
				stopSelf();
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
		private NetworkInfo activeNetwork;
        private boolean isActiveCall;
		
		public ServiceReceiver()
		{
			activeNetwork = getActivNetwork();
            isActiveCall = isActiveCall();
		}
		
		@Override
		public void onReceive(Context context, Intent intent) 
		{
			if (intent == null || intent.getAction() == null) return;
	        
			if (STATE == STATE_USERPAUSED) return;
			
            if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION))
            {
            	NetworkInfo ni = getActivNetwork();
            	
            	if (ni != null)
            	{
            		if (activeNetwork == null)
            		{
            			if (ni.isConnected() && !isActiveCall() && STATE == STATE_AUTOPAUSED && lastIntent != null)
            			{
            				startService(lastIntent);
            				lastIntent = null;
            			}
            		}
            		else
            		{
            			if (activeNetwork.getType() == ni.getType())
            			{
            				if (ni.isConnected() && !activeNetwork.isConnected() && !isActiveCall() && 
            						STATE == STATE_AUTOPAUSED && lastIntent != null)
            				{
                				startService(lastIntent);
                				lastIntent = null;
            				}
            			}
            			else if (ni.isConnected())
            			{
            				stopPlayback(STATE_AUTOPAUSED, AP_REASON_RECONNECT);
            				
            				if (!isActiveCall() && lastIntent != null)
            				{
            					startService(lastIntent);
                				lastIntent = null;
            				}
            				
            				// TODO: set reason if active call
            			}
            		}
            	}
            	else if (activeNetwork != null) stopPlayback(STATE_AUTOPAUSED, AP_REASON_INTERNET);
            	
            	activeNetwork = ni;
            }
           
            if (intent.getAction().equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED))
            {
                boolean newIsActiveCall = isActiveCall();
                if (isActiveCall == newIsActiveCall) return;
                isActiveCall = newIsActiveCall;
            
                if (isActiveCall) stopPlayback(STATE_AUTOPAUSED, AP_REASON_CALL);
                else
                {
                	if (activeNetwork != null && activeNetwork.isConnected() && 
                			STATE == STATE_AUTOPAUSED && lastIntent != null)
                	{
                		startService(lastIntent);
        				lastIntent = null;
                	}
                }
            }
		}
		
		public boolean isAllowPlay()
		{
			activeNetwork = getActivNetwork();
            isActiveCall = isActiveCall();
            return !isActiveCall && (activeNetwork != null && activeNetwork.isConnected());
		}
		
		private boolean isActiveCall()
		{
			TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
			return tm.getCallState() != TelephonyManager.CALL_STATE_IDLE;
		}
		
		private NetworkInfo getActivNetwork()
		{
			ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo[] nis = cm.getAllNetworkInfo();
			for (NetworkInfo ni : nis) if (ni.isConnected()) return ni;
			return null;
		}
		
		public void register()
		{
			try
			{
				IntentFilter intentFilter = new IntentFilter();
				intentFilter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
				intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
				registerReceiver(this, intentFilter);
			}
			catch (Exception e) {}
		}
		
		public void unregister()
		{
			try
			{
				unregisterReceiver(this);
			}
			catch (Exception e) {}
		}
	}
	
	public class MediaButtonReceiver extends BroadcastReceiver 
	{
		public void onReceive(Context context, Intent intent) 
		{
			if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction()))
			{
				KeyEvent key = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
				
				if (key == null || key.getAction() != KeyEvent.ACTION_UP) return;
				
				int code = key.getKeyCode();
				
				if (
						(Build.VERSION.SDK_INT >= 11 && 
							(code == KeyEvent.KEYCODE_MEDIA_PAUSE || code == KeyEvent.KEYCODE_MEDIA_PLAY)
						) || 
						code == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
				{
					if (RadioService.STATE == STATE_USERPAUSED) startService(lastIntent);
					else 
					{
						try
						{
							pausePendingIntent.send();
						}
						catch (Exception e) {}
					}
				}
			}
		}
	
		public void register()
		{
			try
			{
				IntentFilter intentFilter = new IntentFilter();
				intentFilter.addAction(Intent.ACTION_MEDIA_BUTTON);
				registerReceiver(this, intentFilter);
			}
			catch (Exception e) {}
		}
		
		public void unregister()
		{
			try
			{
				unregisterReceiver(this);
			}
			catch (Exception e) {}
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