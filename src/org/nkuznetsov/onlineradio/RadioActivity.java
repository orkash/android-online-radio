package org.nkuznetsov.onlineradio;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.regex.PatternSyntaxException;

import org.json.JSONException;
import org.nkuznetsov.onlineradio.classes.Bitrate;
import org.nkuznetsov.onlineradio.classes.Station;
import org.nkuznetsov.onlineradio.comparators.BitrateComparatorByBitrate;
import org.nkuznetsov.onlineradio.comparators.StationsComparatorByFavorite;
import org.nkuznetsov.onlineradio.comparators.StationsComparatorByName;
import org.nkuznetsov.onlineradio.comparators.StationsComparatorByOwn;
import org.nkuznetsov.onlineradio.exceptions.BadGatewayException;
import org.nkuznetsov.onlineradio.exceptions.GatewayTimeoutException;
import org.nkuznetsov.onlineradio.exceptions.ServerErrorException;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class RadioActivity extends SherlockActivity implements OnChildClickListener, OnItemLongClickListener, OnClickListener
{
    private static final int ACTION_ACTIVITY_START = 0;
    private static final int ACTION_UPDATE_LIST = 1;
    private static final String SELIALIZE_TO_FILE = "stations.object";
    private static final String SELIALIZE_TO_OWNFILE = "own.object";
    private static final int RESULT_OK_READED = 1;
    private static final int RESULT_OK_DOWNLOADED = 2;
    private static final int RESULT_FAILED = 3;
    
    private static final int ID_DONATEFOOTER = -3165;
    
	private TextView message;
    private ProgressBar progress;
    private ExpandableListView list;
    
    private StationsAdapter adapter;
    private StationLoader stationLoader;
    
	@Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);

        GA.init(this);
        
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
        list.setOnChildClickListener(RadioActivity.this);
		list.setOnItemLongClickListener(RadioActivity.this);
		list.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
		
		// Donate footer
		TextView donateFooter = (TextView) LayoutInflater.from(this).inflate(android.R.layout.simple_list_item_1, null);
		donateFooter.setId(ID_DONATEFOOTER);
		donateFooter.setText(R.string.menu_donate);
		donateFooter.setOnClickListener(this);
		donateFooter.setGravity(Gravity.CENTER);
		list.addFooterView(donateFooter, null, false);
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
		GA.startActivity("RadioActivity");
		if (adapter == null) loadStations(ACTION_ACTIVITY_START);
	}
	
	@Override
	protected void onStop()
	{
		cancelLoadStations();
		GA.stopActivity(RadioService.STATE == RadioService.STATE_STOPPED);
		super.onStop();
	}
	
	@Override
	public void onClick(View v)
	{
		if (v.getId() == ID_DONATEFOOTER)
		{
			startActivity(new Intent(this, DonateActivity.class));
			GA.trackClick("RadioActivity > DonateFooter");
		}
	}
	
	private static final int MENU_UPDATE = 1;
	private static final int MENU_STOP = 2;
	private static final int MENU_RATE = 3;
	private static final int MENU_SHARE = 4;
	private static final int MENU_ADD = 5;
	private static final int MENU_DONATE = 6;
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{	
		MenuItem add = menu.add(0, MENU_ADD, 0, getString(R.string.menu_add));
		add.setIcon(R.drawable.ic_add);
		add.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		
		MenuItem stop = menu.add(0, MENU_STOP, 1, getString(R.string.menu_stop));
		stop.setIcon(R.drawable.ic_stop);
		stop.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		
		MenuItem update = menu.add(0, MENU_UPDATE, 2, getString(R.string.menu_update));
		update.setIcon(R.drawable.ic_refresh);
		update.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
		
		MenuItem share = menu.add(0, MENU_SHARE, 3, getString(R.string.menu_share));
		share.setIcon(R.drawable.ic_share);
		share.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
		
		MenuItem rate = menu.add(0, MENU_RATE, 4, getString(R.string.menu_rate));
		rate.setIcon(R.drawable.ic_rate);
		rate.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
		
		MenuItem donate = menu.add(0, MENU_DONATE, 5, getString(R.string.menu_donate));
		donate.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
		
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
			case MENU_UPDATE:
				loadStations(ACTION_UPDATE_LIST);
				GA.trackClick("RadioActivity > Update");
			case MENU_STOP:
				if (RadioService.STATE != RadioService.STATE_STOPPED) 
					RadioService.stopService(getApplicationContext());
				GA.trackClick("RadioActivity > Stop");
				break;
			case MENU_SHARE:
				Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
				shareIntent.setType("text/plain");
				shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getString(R.string.app_name));
				shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, getString(R.string.share));
				try
				{
					startActivity(Intent.createChooser(shareIntent, getString(R.string.menu_share)));
				}
				catch (Exception e) {}
				GA.trackClick("RadioActivity > Share");
				break;
			case MENU_RATE:
				try
				{
					startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getPackageName())));
				}
				catch (Exception e) {}
				GA.trackClick("RadioActivity > Rate");
				break;
			case MENU_ADD:
				if (adapter != null && adapter.stations != null)
					new AddStationDialog(this).show();
				else Toast.makeText(this, R.string.addstation_wait, Toast.LENGTH_LONG).show();
				GA.trackClick("RadioActivity > Add");
				break;
				
			case MENU_DONATE:
				startActivity(new Intent(this, DonateActivity.class));
				GA.trackClick("RadioActivity > Donate");
				break;
		}
		return true;
	}
	
	@Override
	public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) 
	{
		Station station = adapter.stations.get(groupPosition);
		if (station != null)
		{
			Bitrate bitrate = station.getBitrates().get(childPosition);
			if (bitrate != null)
			{
				RadioService.GROP = station.hashCode();
				RadioService.CHILD = bitrate.hashCode();
				Intent newIntent = new Intent(getApplicationContext(), RadioService.class);
				newIntent.setAction(RadioService.ACTION_START);
				newIntent.putExtra(RadioService.EXTRA_STRING_URL, bitrate.getUrl());
				newIntent.putExtra(RadioService.EXTRA_STRING_URLISFINAL, OwnList.isOwn(station.getId()));
				newIntent.putExtra(RadioService.EXTRA_STRING_NOTIFICATION, getString(R.string.note_message, station.getName(), getString(R.string.kb_sec_format, bitrate.getBitrate())));
				startService(newIntent);
				GA.trackClick("RadioActivity > Play");
			}
		}
		return false;
	}
	
	@Override
	public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2, long id)
	{
        int positionType = ExpandableListView.getPackedPositionType(id);
        final int positionGroup = ExpandableListView.getPackedPositionGroup(id);
        
        if (positionGroup > -1)
        {
        	final Station station = adapter.stations.get(positionGroup);
        
        	if (OwnList.isOwn(station.getId()))
        	{
        		if (positionType == ExpandableListView.PACKED_POSITION_TYPE_GROUP)
        		{
        			AlertDialog.Builder builder = new AlertDialog.Builder(this);
    				builder.setTitle(R.string.remstation_title);
    				builder.setMessage(getString(R.string.remstation_message, station.getName()));
    				builder.setPositiveButton(R.string.remstation_remove, new DialogInterface.OnClickListener()
    				{
    					@Override
    					public void onClick(DialogInterface dialog, int which)
    					{
    						list.collapseGroup(positionGroup);
    						FavoriteList.remove(station.getId());
    						OwnList.remove(station.getId());
    						adapter.stations.remove(station);
    						adapter.notifyDataSetChanged();
    						saveOwnStations(getOwnStations());
    						GA.trackClick("RadioActivity > Remove > Remove");
    					}
    				});
    				builder.setNegativeButton(R.string.remstation_cancel, null);
    				builder.show().setVolumeControlStream(AudioManager.STREAM_MUSIC);
    				GA.trackClick("RadioActivity > Remove");
        			return true;
        		}
        
        		if (positionType == ExpandableListView.PACKED_POSITION_TYPE_CHILD)
        		{
        			int positionChild = ExpandableListView.getPackedPositionChild(id);
        			final Bitrate bitrate = station.getBitrates().get(positionChild);
    				
        			AlertDialog.Builder builder = new AlertDialog.Builder(this);
    				builder.setTitle(R.string.rembitrate_title);
    				builder.setMessage(getString(R.string.rembitrate_message, getString(R.string.kb_sec_format, bitrate.getBitrate()), station.getName()));
    				builder.setPositiveButton(R.string.rembitrate_remove, new DialogInterface.OnClickListener()
    				{
    					@Override
    					public void onClick(DialogInterface dialog, int which)
    					{
    						station.getBitrates().remove(bitrate);
    						adapter.notifyDataSetChanged();
    						saveOwnStations(getOwnStations());
    						GA.trackClick("RadioActivity > Remove > Remove");
    					}
    				});
    				builder.setNegativeButton(R.string.rembitrate_cancel, null);
    				builder.show().setVolumeControlStream(AudioManager.STREAM_MUSIC);
    				GA.trackClick("RadioActivity > Remove");
        			return true;
        		}
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
	
	public void addOwnStation(String name, String bitrate, String url)
	{
		final List<Station> stations = getOwnStations();
		
		boolean added = false;
		
		for (Station station : stations)
		{
			if (station.getName().equals(name))
			{
				station.getBitrates().add(new Bitrate(bitrate, url));
				added = true;
			}
		}
		
		if (!added)
		{
			String id = String.valueOf((new Random().nextInt() + System.currentTimeMillis() % 1000) * -1);
			List<Bitrate> bitrates = new ArrayList<Bitrate>();
			bitrates.add(new Bitrate(bitrate, url));
			Station station = new Station(id, name, bitrates);
			OwnList.add(id);
			stations.add(station);
			adapter.stations.add(station);
		}
		
		adapter.notifyDataSetChanged();
		
		saveOwnStations(stations);
	}
	
	public List<Station> getOwnStations()
	{
		List<Station> stations = new ArrayList<Station>();
		
		for (Station station : adapter.stations)
			if (OwnList.isOwn(station.getId())) stations.add(station);
		
		return stations;
	}
	
	public void saveOwnStations(final List<Station> stations)
	{
		new Thread()
		{
			public void run() 
			{
				try
				{
					File ownFile = new File(getFilesDir(), SELIALIZE_TO_OWNFILE);
					ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(ownFile));
					os.writeObject(stations);
					os.flush();
					os.close();
				}
				catch (Exception e) {}
			}
		}.start();
	}
	
	private void cancelLoadStations()
	{
		if (stationLoader != null && stationLoader.getStatus() == Status.RUNNING) stationLoader.cancel(true);
	}
	
	private class AddStationDialog implements OnClickListener, TextWatcher
	{
		AlertDialog dialog;
		AutoCompleteTextView name;
		EditText bitrate, url;
		boolean needSetOnClickListener = true;
		
		public AddStationDialog(Context context)
		{
			AlertDialog.Builder builder = new AlertDialog.Builder(context);
			
			builder.setTitle(R.string.menu_add);
			
			View view = LayoutInflater.from(context).inflate(R.layout.dialog_add, null);
			
			name = (AutoCompleteTextView) view.findViewById(R.id.input_name);
			List<String> names = new ArrayList<String>();
			for (Station station : getOwnStations()) names.add(station.getName());
			name.setAdapter(new ArrayAdapter<String>(context, R.layout.item_dropdown, names));
			name.addTextChangedListener(this);
			bitrate = (EditText) view.findViewById(R.id.input_bitrate);
			bitrate.addTextChangedListener(this);
			url = (EditText) view.findViewById(R.id.input_url);
			url.addTextChangedListener(this);
			
			builder.setView(view);
			
			builder.setPositiveButton(R.string.addstation_add, null);
			builder.setNegativeButton(R.string.addstation_cancel, null);
			
			dialog = builder.create();
			dialog.setCanceledOnTouchOutside(false);
			dialog.setVolumeControlStream(AudioManager.STREAM_MUSIC);
		}
		
		public void show()
		{
			dialog.show();
		}

		@Override
		public void afterTextChanged(Editable arg0) {}

		@Override
		public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}

		@Override
		public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) 
		{
			if (!needSetOnClickListener) return;
			
			needSetOnClickListener = false;
			
			Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
			
			if (positive == null)
			{
				try
				{
					Field f = AlertDialog.class.getDeclaredField("mAlert");
					f.setAccessible(true);
					Object mAlert = f.get(dialog);
					f = mAlert.getClass().getDeclaredField("mButtonPositive");
					f.setAccessible(true);
					positive = (Button) f.get(mAlert);
				}
				catch (Exception e)
				{
					GA.trackException(e);
				}
			}
			
			if (positive == null)
				positive = (Button) dialog.findViewById(android.R.id.button1);
			
			positive.setOnClickListener(this);
		}
		
		@Override
		public void onClick(View arg0)
		{
			if (name.length() == 0)
				Toast.makeText(RadioActivity.this, R.string.addstation_wrongname, Toast.LENGTH_SHORT).show();
			else if (bitrate.length() == 0)
				Toast.makeText(RadioActivity.this, R.string.addstation_wrongbitrate, Toast.LENGTH_SHORT).show();
			else if (!Utils.isCorrectUrl(url.getText().toString()))
				Toast.makeText(RadioActivity.this, R.string.addstation_wrongurl, Toast.LENGTH_SHORT).show();
			else
			{
				addOwnStation(name.getText().toString(), bitrate.getText().toString(), url.getText().toString());
				dialog.dismiss();
				GA.trackClick("RadioActivity > Add > Add");
			}
		}
	}
	
	private class StationLoader extends AsyncTask<Integer, Void, Void>
	{	
		private int what = 0;
		private String mes = "";
		private List<Station> stations;
		
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
			FavoriteList.init(RadioActivity.this);
			OwnList.init(RadioActivity.this);
			
			ObjectInputStream is;
			ObjectOutputStream os;
			
			try 
			{
				File stationsFile = new File(getFilesDir(), SELIALIZE_TO_FILE);
				File ownFile = new File(getFilesDir(), SELIALIZE_TO_OWNFILE);
				
				if (params[0] == ACTION_UPDATE_LIST) stationsFile.delete();
				if (stationsFile.exists())
				{
					is = new ObjectInputStream(new FileInputStream(stationsFile));
					stations = (List<Station>) is.readObject();
					is.close();
					
					if (ownFile.exists())
					{
						try
						{
							is = new ObjectInputStream(new FileInputStream(ownFile));
							stations.addAll((List<Station>) is.readObject());
							is.close();
						}
						catch (Exception e)
						{
							ownFile.delete();
						}
					}
					
					what = RESULT_OK_READED;
				}
				else
				{
					stations = API.getStations();
					os = new ObjectOutputStream(new FileOutputStream(stationsFile));
					os.writeObject(stations);
					os.flush();
					os.close();
					
					if (ownFile.exists())
					{
						try
						{
							is = new ObjectInputStream(new FileInputStream(ownFile));
							stations.addAll((List<Station>) is.readObject());
							is.close();
						}
						catch (Exception e)
						{
							ownFile.delete();
						}
					}
					
					what = RESULT_OK_DOWNLOADED;
				}
				
				for (Station station : stations) 
					Collections.sort(station.getBitrates(), new BitrateComparatorByBitrate());
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
					adapter = new StationsAdapter(stations);
					list.setAdapter(adapter);
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
			if (isChecked) 
			{
				FavoriteList.add(id);
				GA.trackClick("RadioActivity > Favorite > Add");
			}
			else 
			{
				FavoriteList.remove(id);
				GA.trackClick("RadioActivity > Favorite > Remove");
			}
			
			adapter.notifyDataSetChanged();
		}
	}
	
	private class StationsAdapter extends BaseExpandableListAdapter
	{	
		private static final String ID_DIVIDER = "#dfsddslssdf#";
		
		List<Station> stations;
		
		public StationsAdapter(List<Station> stations)
		{
			this.stations = stations;
			
			notifyDataSetChanged();
		}
		
		@Override
		public void notifyDataSetChanged()
		{
			for (Iterator<Station> sts = stations.iterator(); sts.hasNext();)
			{
				Station station = sts.next();
				if (station.getId().equals(ID_DIVIDER)) sts.remove();
			}
			
			Collections.sort(stations, new StationsComparatorByName());
			Collections.sort(stations, new StationsComparatorByOwn());
			Collections.sort(stations, new StationsComparatorByFavorite());
			
			List<Integer> dividerPositions = new ArrayList<Integer>();
			
			if (stations.size() > 0)
			{
				dividerPositions.add(0);
				
				
				for (int i = 0; i < stations.size() - 1; i ++)
				{
					Station current = stations.get(i);
					Station next = stations.get(i + 1);
					
					boolean isCurrentFavorite = FavoriteList.isFavorite(current.getId());
					boolean isNextFavorite = FavoriteList.isFavorite(next.getId());
					
					boolean isCurrentOwn = OwnList.isOwn(current.getId());
					boolean isNextOwn = OwnList.isOwn(next.getId());
					
					if ((isCurrentFavorite && isNextFavorite) || (isCurrentOwn && isNextOwn)) continue;
					
					if (isCurrentFavorite && !isNextFavorite) dividerPositions.add(i + 1 + dividerPositions.size());
					else if (isCurrentOwn && !isNextOwn) dividerPositions.add(i + 1 + dividerPositions.size());
				}
				
				for (Integer integer : dividerPositions)
				{
					Station next = stations.get(integer);
					
					if (FavoriteList.isFavorite(next.getId()))
						stations.add(integer, new Station(ID_DIVIDER, getString(R.string.divider_fav), new ArrayList<Bitrate>()));
					else if (OwnList.isOwn(next.getId()))
						stations.add(integer, new Station(ID_DIVIDER, getString(R.string.divider_own), new ArrayList<Bitrate>()));
					else stations.add(integer, new Station(ID_DIVIDER, getString(R.string.divider_rambler), new ArrayList<Bitrate>()));
				}
			}
			
			super.notifyDataSetChanged();
		}
		
		@Override
		public Bitrate getChild(int groupPosition, int childPosition) 
		{
			return stations.get(groupPosition).getBitrates().get(childPosition);
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) 
		{
			return childPosition; //stations.get(groupPosition).getBitrates().get(childPosition).hashCode();
		}

		@Override
		public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) 
		{
			Station station = stations.get(groupPosition);
			
			if (station.getId().equals(ID_DIVIDER))
			{
				convertView = LayoutInflater.from(RadioActivity.this).inflate(R.layout.item_divider, null);
				TextView tw = (TextView) convertView;
				tw.setText(station.getName());
			}
			else
			{
				StationViewHolder holder;
				if (convertView != null && convertView.getTag() instanceof StationViewHolder) 
					holder = (StationViewHolder) convertView.getTag();
				else
				{
					convertView = LayoutInflater.from(RadioActivity.this).inflate(R.layout.item_station, null);
					holder = new StationViewHolder(convertView);
					convertView.setTag(holder);
				}
				
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
				convertView =  LayoutInflater.from(RadioActivity.this).inflate(R.layout.item_bitrate, null);
				holder = new BitrateViewHolder(convertView);
				convertView.setTag(holder);
			}
			
			Station station = stations.get(groupPosition);
			Bitrate bitrate = station.getBitrates().get(childPosition);
			
			holder.name.setText(getString(R.string.kb_sec_format, bitrate.getBitrate()));
			
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
			return groupPosition; //stations.get(groupPosition).hashCode();
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