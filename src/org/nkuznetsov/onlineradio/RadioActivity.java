package org.nkuznetsov.onlineradio;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.regex.PatternSyntaxException;
import org.json.JSONException;
import org.nkuznetsov.onlineradio.R;
import org.nkuznetsov.onlineradio.classes.Bitrate;
import org.nkuznetsov.onlineradio.classes.Station;
import org.nkuznetsov.onlineradio.exceptions.BadGatewayException;
import org.nkuznetsov.onlineradio.exceptions.GatewayTimeoutException;
import org.nkuznetsov.onlineradio.exceptions.ServerErrorException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class RadioActivity extends Activity implements OnChildClickListener
{
    private static final int ACTION_ACTIVITY_START = 0;
    private static final int ACTION_UPDATE_LIST = 1;
    private static final String SELIALIZE_TO_FILE = "stations.object";
    private static final int RESULT_OK_READED = 1;
    private static final int RESULT_OK_DOWNLOADED = 2;
    private static final int RESULT_FAILED = 3;
    public static final int PLAYING_STATE_CHANGED_EVENT = 5548756;
    
	private TextView message;
    private ProgressBar progress;
    private ExpandableListView list;
    
    private List<Station> stations;
    
    private StationLoader stationLoader;
    
	@Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_radio);
        
        // get views
        message = (TextView) findViewById(R.id.activity_radio_message);
        progress = (ProgressBar) findViewById(R.id.activity_radio_progress);
        list = (ExpandableListView) findViewById(R.id.activity_radio_list);
    }
	
	@Override
	protected void onResume() 
	{
		super.onResume();
		loadStations(ACTION_ACTIVITY_START);
		RadioService.StateChangeListener = new OnStateChangeListener();
	}
	
	@Override
	protected void onPause() 
	{
		cancelLoadStations();
		RadioService.StateChangeListener = null;
		super.onPause();
	}
	
	private static final int UPDATE_STATIONS_ITEM_ID = 1;
	private static final int STOP_RADIO_ITEM_ID = 2;
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		menu.add(0, UPDATE_STATIONS_ITEM_ID, 0, getString(R.string.menu_update));
		menu.add(0, STOP_RADIO_ITEM_ID, 1, getString(R.string.menu_stop));
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) 
	{
		switch (item.getItemId())
		{
			case UPDATE_STATIONS_ITEM_ID:
				loadStations(ACTION_UPDATE_LIST);
			case STOP_RADIO_ITEM_ID:
				if (RadioService.STATE != RadioService.STATE_STOPPED)
				{
					RadioService.stopService(getApplicationContext());
				}
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
				RadioService.GROP = groupPosition;
				RadioService.CHILD = childPosition;
				Intent newIntent = new Intent(getApplicationContext(), RadioService.class);
				newIntent.setAction(RadioService.ACTION_START);
				newIntent.putExtra(RadioService.EXTRA_STRING_URL, bitrate.getUrl());
				newIntent.putExtra(RadioService.EXTRA_STRING_NOTIFICATION, String.format("%s (%s  Ѕит/сек)", station.getName(), bitrate.getBitrate()));
				startService(newIntent);
			}
		}
		return false;
	}
	
	private class OnStateChangeListener extends Handler
	{	
		@Override
		public void handleMessage(Message msg) 
		{
			if (msg.what == PLAYING_STATE_CHANGED_EVENT)
			{
				StationsAdapter adapter = ((StationsAdapter)list.getExpandableListAdapter());
				if (adapter != null) adapter.notifyDataSetChanged();
			}
		}
	}
	
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
			try 
			{
				File stationsFile = new File(getFilesDir(), SELIALIZE_TO_FILE);
				if (params[0] == ACTION_UPDATE_LIST)
				{
					stationsFile.delete();
				}
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
					list.setAdapter(new StationsAdapter(RadioActivity.this, stations));
					list.setOnChildClickListener(RadioActivity.this);
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
	
	private class StationsAdapter extends BaseExpandableListAdapter
	{
		private Context context;
		private List<Station> stations;
		
		public StationsAdapter(Context context, List<Station> stations)
		{
			this.context = context;
			this.stations = stations;
		}
		
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
				convertView = ((LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.item_station, null);
				holder = new StationViewHolder(convertView);
				convertView.setTag(holder);
			}
			
			Station station = stations.get(groupPosition);
			if (station != null)
			{
				holder.name.setText(station.getName());
				holder.progress.setVisibility(View.GONE);
				if (RadioService.STATE == RadioService.STATE_PREPARING
						&& RadioService.GROP == groupPosition) holder.progress.setVisibility(View.VISIBLE);
				holder.play.setVisibility(View.GONE);
				if (RadioService.STATE == RadioService.STATE_STARTED
						&& RadioService.GROP == groupPosition) holder.play.setVisibility(View.VISIBLE);
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
				convertView = ((LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.item_bitrate, null);
				holder = new BitrateViewHolder(convertView);
				convertView.setTag(holder);
			}
			
			Bitrate bitrate = stations.get(groupPosition).getBitrates().get(childPosition);
			if (bitrate != null)
			{
				holder.name.setText(String.format("%s  Ѕит/сек", bitrate.getBitrate()));
				holder.progress.setVisibility(View.GONE);
				if (RadioService.STATE == RadioService.STATE_PREPARING
						&& RadioService.GROP == groupPosition
						&& RadioService.CHILD == childPosition) holder.progress.setVisibility(View.VISIBLE);
				holder.play.setVisibility(View.GONE);
				if (RadioService.STATE == RadioService.STATE_STARTED
						&& RadioService.GROP == groupPosition
						&& RadioService.CHILD == childPosition) holder.play.setVisibility(View.VISIBLE);
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
		
		public StationViewHolder(View view)
		{
			name = (TextView) view.findViewById(R.id.item_station_name);
			progress = (ProgressBar) view.findViewById(R.id.item_station_progress);
			play = (ImageView) view.findViewById(R.id.item_station_play);
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