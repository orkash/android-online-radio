package org.nkuznetsov.onlineradio;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.List;
import java.util.regex.PatternSyntaxException;

import org.json.JSONException;
import org.nkuznetsov.onlineradio.classes.Bitrate;
import org.nkuznetsov.onlineradio.classes.Station;
import org.nkuznetsov.onlineradio.comparators.BitrateComparatorByBitrate;
import org.nkuznetsov.onlineradio.comparators.StationsComparatorByFavorite;
import org.nkuznetsov.onlineradio.comparators.StationsComparatorByName;
import org.nkuznetsov.onlineradio.exceptions.BadGatewayException;
import org.nkuznetsov.onlineradio.exceptions.GatewayTimeoutException;
import org.nkuznetsov.onlineradio.exceptions.ServerErrorException;

import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public abstract class AbstractRadioActivity extends SherlockActivity implements OnChildClickListener
{
    private static final int ACTION_ACTIVITY_START = 0;
    private static final int ACTION_UPDATE_LIST = 1;
    private static final String SELIALIZE_TO_FILE = "stations.object";
    private static final int RESULT_OK_READED = 1;
    private static final int RESULT_OK_DOWNLOADED = 2;
    private static final int RESULT_FAILED = 3;
    
	private TextView message;
    private ProgressBar progress;
    private ExpandableListView list;
    
    private List<Station> stations;
    private StationsAdapter adapter;
    private StationLoader stationLoader;
    
	protected abstract Class<?> getServiceClass();
    
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_radio);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE, 
        		ActionBar.DISPLAY_SHOW_TITLE | 
        		ActionBar.DISPLAY_SHOW_HOME | 
        		ActionBar.DISPLAY_USE_LOGO);
        
        // get views
        message = (TextView) findViewById(R.id.activity_radio_message);
        progress = (ProgressBar) findViewById(R.id.activity_radio_progress);
        list = (ExpandableListView) findViewById(R.id.activity_radio_list);
    }
	
	@Override
	protected void onResume() 
	{
		super.onResume();
		RadioService.stateChangeListener = onStateChangeListener;
		if (adapter != null) adapter.notifyDataSetChanged();
	}
	
	@Override
	protected void onPause() 
	{
		RadioService.stateChangeListener = null;
		super.onPause();
	}
	
	@Override
	protected void onStart()
	{
		super.onStart();
		if (adapter == null) loadStations(ACTION_ACTIVITY_START);
	}
	
	@Override
	protected void onStop()
	{
		cancelLoadStations();
		super.onStop();
	}
	
	private static final int MENU_UPDATE = 1;
	private static final int MENU_STOP = 2;
	private static final int MENU_RATE = 3;
	private static final int MENU_SHARE = 4;
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuItem share = menu.add(0, MENU_SHARE, 0, getString(R.string.menu_share));
		share.setIcon(R.drawable.ic_share);
		share.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		
		MenuItem stop = menu.add(0, MENU_STOP, 1, getString(R.string.menu_stop));
		stop.setIcon(R.drawable.ic_stop);
		stop.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		
		MenuItem update = menu.add(0, MENU_UPDATE, 2, getString(R.string.menu_update));
		update.setIcon(R.drawable.ic_refresh);
		update.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
		
		MenuItem rate = menu.add(0, MENU_RATE, 3, getString(R.string.menu_rate));
		rate.setIcon(R.drawable.ic_rate);
		rate.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
			case MENU_UPDATE:
				loadStations(ACTION_UPDATE_LIST);
			case MENU_STOP:
				if (RadioService.STATE != RadioService.STATE_STOPPED) 
					RadioService.stopService(getApplicationContext(), getServiceClass());
				break;
			case MENU_SHARE:
				Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
				shareIntent.setType("text/plain");
				shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getString(R.string.app_name));
				shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, getString(R.string.share));
				startActivity(Intent.createChooser(shareIntent, getString(R.string.menu_share)));
				break;
			case MENU_RATE:
				startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getPackageName())));
				break;
		}
		return true;
	}
	
	@Override
	public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) 
	{
		Station station = stations.get(groupPosition);
		if (station != null)
		{
			Bitrate bitrate = station.getBitrates().get(childPosition);
			if (bitrate != null)
			{
				RadioService.GROP = station.hashCode();
				RadioService.CHILD = bitrate.hashCode();
				Intent newIntent = new Intent(getApplicationContext(), getServiceClass());
				newIntent.setAction(RadioService.ACTION_START);
				newIntent.putExtra(RadioService.EXTRA_STRING_URL, bitrate.getUrl());
				newIntent.putExtra(RadioService.EXTRA_STRING_NOTIFICATION, String.format("%s (%s  Ѕит/сек)", station.getName(), bitrate.getBitrate()));
				startService(newIntent);
			}
		}
		return false;
	}
	
	private Runnable onStateChangeListener = new Runnable()
	{
		@Override
		public void run()
		{
			runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					StationsAdapter adapter = ((StationsAdapter)list.getExpandableListAdapter());
					if (adapter != null) adapter.notifyDataSetChanged();
				}
			});
		}
	};
	
	private void loadStations(int action)
	{
		StationLoader stationLoader = new StationLoader();
		stationLoader.execute(action);
	}
	
	private void cancelLoadStations()
	{
		if (stationLoader != null && stationLoader.getStatus() == Status.RUNNING) stationLoader.cancel(true);
	}
	
	private class StationLoader extends AsyncTask<Integer, Void, Void>
	{	
		private int what = 0;
		private String mes = "";
		
		@Override
		protected void onPreExecute() 
		{
			message.setVisibility(View.GONE);
	        progress.setVisibility(View.GONE);
	        list.setVisibility(View.GONE);
	        progress.setVisibility(View.VISIBLE);
		}
		
		@SuppressWarnings("unchecked")
		@Override
		protected Void doInBackground(Integer... params) 
		{
			FavoriteList.init(AbstractRadioActivity.this);
			
			try 
			{
				File stationsFile = new File(getFilesDir(), SELIALIZE_TO_FILE);
				if (params[0] == ACTION_UPDATE_LIST) stationsFile.delete();
				if (stationsFile.exists())
				{
					ObjectInputStream is = new ObjectInputStream(new FileInputStream(stationsFile));
					stations = (List<Station>) is.readObject();
					is.close();
					what = RESULT_OK_READED;
				}
				else
				{
					stations = API.getStations();
					ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(stationsFile));
					os.writeObject(stations);
					os.flush();
					os.close();
					what = RESULT_OK_DOWNLOADED;
				}
				
				for (Station station : stations) 
					Collections.sort(station.getBitrates(), new BitrateComparatorByBitrate());
				
				Collections.sort(stations, new StationsComparatorByName());
				Collections.sort(stations, new StationsComparatorByFavorite());
			}
			catch (PatternSyntaxException e) {} 
			catch (BadGatewayException e) 
			{
				what = RESULT_FAILED;
				mes = getString(R.string.error_server);
			} 
			catch (GatewayTimeoutException e) 
			{
				what = RESULT_FAILED;
				mes = getString(R.string.error_server);
			} 
			catch (ServerErrorException e) 
			{
				what = RESULT_FAILED;
				mes = getString(R.string.error_server);
			} 
			catch (IOException e) 
			{
				what = RESULT_FAILED;
				mes = getString(R.string.error_io);
			} 
			catch (JSONException e) 
			{
				what = RESULT_FAILED;
				mes = getString(R.string.error_json);
			}
			catch (ClassNotFoundException e) {}
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) 
		{
			if (!isCancelled())
			{
				if (what == RESULT_OK_READED || what == RESULT_OK_DOWNLOADED)
				{
					progress.setVisibility(View.GONE);
					message.setVisibility(View.GONE);
					list.setVisibility(View.VISIBLE);
					adapter = new StationsAdapter();
					list.setAdapter(adapter);
					list.setOnChildClickListener(AbstractRadioActivity.this);
				}
				if (what == RESULT_FAILED)
				{
					progress.setVisibility(View.GONE);
					list.setVisibility(View.GONE);
					message.setVisibility(View.VISIBLE);
					message.setText(mes);
				}
			}
		}
	}
	
	private class OnFavoriteChangeListener implements OnCheckedChangeListener
	{
		private String id;
		
		public OnFavoriteChangeListener(String id)
		{
			this.id = id;
		}
		
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
		{
			if (isChecked) FavoriteList.add(id);
			else FavoriteList.remove(id);
			
			Collections.sort(stations, new StationsComparatorByName());
			Collections.sort(stations, new StationsComparatorByFavorite());
			
			adapter.notifyDataSetChanged();
		}
	}
	
	private class StationsAdapter extends BaseExpandableListAdapter
	{	
		@Override
		public Bitrate getChild(int groupPosition, int childPosition) 
		{
			return stations.get(groupPosition).getBitrates().get(childPosition);
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) 
		{
			return stations.get(groupPosition).getBitrates().get(childPosition).hashCode();
		}

		@Override
		public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) 
		{
			StationViewHolder holder;
			if (convertView != null) holder = (StationViewHolder) convertView.getTag();
			else
			{
				convertView = LayoutInflater.from(AbstractRadioActivity.this).inflate(R.layout.item_station, null);
				holder = new StationViewHolder(convertView);
				convertView.setTag(holder);
			}
			
			Station station = stations.get(groupPosition);
			
			holder.name.setText(station.getName());
			
			holder.favorite.setOnCheckedChangeListener(null);
			holder.favorite.setChecked(FavoriteList.isFavorite(station.getId()));
			holder.favorite.setOnCheckedChangeListener(new OnFavoriteChangeListener(station.getId()));
			
			holder.progress.setVisibility(View.GONE);
			holder.play.setVisibility(View.GONE);
			
			if (RadioService.GROP == station.hashCode())
			{
				if (RadioService.STATE == RadioService.STATE_PREPARING)
					holder.progress.setVisibility(View.VISIBLE);
				if (RadioService.STATE == RadioService.STATE_STARTED)
					holder.play.setVisibility(View.VISIBLE);
			}
			return convertView;
		}
		
		@Override
		public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) 
		{
			BitrateViewHolder holder;
			if (convertView != null) holder = (BitrateViewHolder) convertView.getTag();
			else
			{
				convertView =  LayoutInflater.from(AbstractRadioActivity.this).inflate(R.layout.item_bitrate, null);
				holder = new BitrateViewHolder(convertView);
				convertView.setTag(holder);
			}
			
			Station station = stations.get(groupPosition);
			Bitrate bitrate = station.getBitrates().get(childPosition);
			
			holder.name.setText(String.format("%s  Ѕит/сек", bitrate.getBitrate()));
			
			holder.progress.setVisibility(View.GONE);
			holder.play.setVisibility(View.GONE);
			
			if (RadioService.CHILD == bitrate.hashCode())
			{
				if (RadioService.STATE == RadioService.STATE_PREPARING)
					holder.progress.setVisibility(View.VISIBLE);
				if (RadioService.STATE == RadioService.STATE_STARTED)
					holder.play.setVisibility(View.VISIBLE);
			}
			
			return convertView;
		}

		@Override
		public int getChildrenCount(int groupPosition) 
		{
			return stations.get(groupPosition).getBitrates().size();
		}

		@Override
		public Station getGroup(int groupPosition) 
		{
			return stations.get(groupPosition);
		}

		@Override
		public int getGroupCount() 
		{
			return stations.size();
		}

		@Override
		public long getGroupId(int groupPosition) 
		{
			return stations.get(groupPosition).hashCode();
		}

		@Override
		public boolean hasStableIds() 
		{
			return true;
		}

		@Override
		public boolean isChildSelectable(int groupPosition, int childPosition) 
		{
			return true;
		}
	}
	
	private class StationViewHolder
	{
		TextView name;
		ProgressBar progress;
		ImageView play;
		CheckBox favorite;
		
		public StationViewHolder(View view)
		{
			name = (TextView) view.findViewById(R.id.item_station_name);
			progress = (ProgressBar) view.findViewById(R.id.item_station_progress);
			play = (ImageView) view.findViewById(R.id.item_station_play);
			favorite = (CheckBox) view.findViewById(R.id.item_station_favorite);
		}
	}
	
	private class BitrateViewHolder
	{
		TextView name;
		ProgressBar progress;
		ImageView play;
		
		public BitrateViewHolder(View view)
		{
			name = (TextView) view.findViewById(R.id.item_bitrate_name);
			progress = (ProgressBar) view.findViewById(R.id.item_bitrate_progress);
			play = (ImageView) view.findViewById(R.id.item_bitrate_play);
		}
	}
}