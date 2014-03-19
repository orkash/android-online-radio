package org.nkuznetsov.onlineradio;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONObject;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.vending.billing.IInAppBillingService;

public class DonateActivity extends Activity implements OnItemClickListener
{
	private static final String DONATE_1 = "donate_1";
	private static final String DONATE_2 = "donate_2";
	private static final String DONATE_3 = "donate_3";
	
	private static final  ArrayList<String> skuList = new ArrayList<String>();
	
	static
	{
		skuList.add(DONATE_1);
		skuList.add(DONATE_2);
		skuList.add(DONATE_3);
	}
	
	ListView listView;
	ArrayList<Sku> resolvedSkuList = new ArrayList<Sku>();
	ArrayList<DonateItem> items = new ArrayList<DonateItem>();
	DonateAdapter donateAdapter;
	boolean skuLoaded = false;
	IInAppBillingService mService;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		listView = new ListView(this);
		setContentView(listView);
		
		donateAdapter = new DonateAdapter();
		listView.setAdapter(donateAdapter);
		listView.setOnItemClickListener(this);
		
		if (!bindService(new Intent("com.android.vending.billing.InAppBillingService.BIND"), mServiceConn, Context.BIND_AUTO_CREATE))
		{
			skuLoaded = true;
		}
		
		refrashDonateItems();
	}
	
	@Override
	protected void onStart() 
	{
		super.onStart();
		
		GA.startActivity("DonateActivity");
	}
	
	@Override
	protected void onStop() 
	{
		GA.stopActivity(true);
		
		super.onStop();
	}
	
	private void refrashDonateItems()
	{
		items.clear();
		
		items.add(new DonateItem(DonateItem.TYPE_HEADER, getString(R.string.donate_01), null));
		
		items.add(new DonateItem(DonateItem.TYPE_CATEGORY, "Google Play", null));
		
		if (skuLoaded)
		{
			if (resolvedSkuList.size() > 0) for (Sku sku : resolvedSkuList) items.add(new DonateItem(DonateItem.TYPE_PLAY, getString(R.string.donate_02, sku.price), sku.sku));
			else items.add(new DonateItem(DonateItem.TYPE_ERROR, getString(R.string.donate_06), null));
		}
		else items.add(new DonateItem(DonateItem.TYPE_PROGRESS, null, null));
		
		
		items.add(new DonateItem(DonateItem.TYPE_CATEGORY, "Yandex Money", null));
		items.add(new DonateItem(DonateItem.TYPE_NORMAL, "410011713202397", null));
		
		items.add(new DonateItem(DonateItem.TYPE_CATEGORY, "Web Money", null));
		items.add(new DonateItem(DonateItem.TYPE_NORMAL, "R614460012146", null));
		items.add(new DonateItem(DonateItem.TYPE_NORMAL, "Z176696166281", null));
		items.add(new DonateItem(DonateItem.TYPE_NORMAL, "E000000000000", null));
		
		items.add(new DonateItem(DonateItem.TYPE_CATEGORY, "PayPal", null));
		items.add(new DonateItem(DonateItem.TYPE_NAVIGATE, getString(R.string.donate_07), "https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=ZLNLUQWLTJ9XE"));
		
		donateAdapter.notifyDataSetChanged();
	}
	
	private class DonateAdapter extends BaseAdapter
	{
		LayoutInflater inflater = LayoutInflater.from(DonateActivity.this);

		@Override
		public int getCount()
		{
			return items.size();
		}

		@Override
		public DonateItem getItem(int position)
		{	
			return items.get(position);
		}

		@Override
		public long getItemId(int position)
		{
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			DonateItem item = getItem(position);
			
			if (item.type == DonateItem.TYPE_HEADER)
			{
				TextView view = (TextView) inflater.inflate(android.R.layout.simple_list_item_1, null);
				view.setGravity(Gravity.CENTER);
				view.setText(item.text);
				
				int dp20 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics());
				
				view.setPadding(view.getPaddingLeft(), dp20, view.getPaddingRight(), dp20);
				
				return view;
			}
			
			if (item.type == DonateItem.TYPE_CATEGORY)
			{
				TextView view = (TextView) inflater.inflate(R.layout.item_divider, null);
				view.setText(item.text);
				return view;
			}
			
			if (item.type == DonateItem.TYPE_PROGRESS)
			{
				FrameLayout fl = new FrameLayout(DonateActivity.this);
				TypedValue value = new TypedValue();
				getTheme().resolveAttribute(android.R.attr.listPreferredItemHeight, value, true);
				DisplayMetrics metrics = new DisplayMetrics();
				getWindowManager().getDefaultDisplay().getMetrics(metrics);
				fl.setMinimumHeight((int)value.getDimension(metrics));
				ProgressBar progress = new ProgressBar(DonateActivity.this, null, android.R.attr.progressBarStyleSmall);
				fl.addView(progress, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
				return fl;
			}
			
			if (item.type == DonateItem.TYPE_ERROR)
			{
				TextView view = (TextView) inflater.inflate(android.R.layout.simple_list_item_1, null);
				view.setGravity(Gravity.CENTER);
				view.setText(item.text);
				view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
				return view;
			}
			
			if (item.type == DonateItem.TYPE_PLAY || item.type == DonateItem.TYPE_NORMAL || item.type == DonateItem.TYPE_NAVIGATE)
			{
				TextView view = (TextView) inflater.inflate(android.R.layout.simple_list_item_1, null);
				view.setText(item.text);
				return view;
			}
			
			return null;
		}
		
		@Override
		public boolean isEnabled(int position)
		{
			DonateItem item = getItem(position);
			return item.type == DonateItem.TYPE_PLAY || item.type == DonateItem.TYPE_NORMAL || item.type == DonateItem.TYPE_NAVIGATE;
		}
	}
	
	private class DonateItem
	{
		static final int TYPE_HEADER = 0;
		static final int TYPE_PLAY = 1;
		static final int TYPE_NORMAL = 2;
		static final int TYPE_CATEGORY = 3;
		static final int TYPE_NAVIGATE = 4;
		static final int TYPE_ERROR = 5;
		static final int TYPE_PROGRESS = 6;
		
		int type;
		String text;
		Object extra;
		
		public DonateItem(int type, String text, Object extra)
		{
			this.type = type;
			this.text = text;
			this.extra = extra;
		}
	}
	
	ServiceConnection mServiceConn = new ServiceConnection() 
	{
	   @Override
	   public void onServiceConnected(ComponentName name, IBinder service) 
	   {
	       mService = IInAppBillingService.Stub.asInterface(service);
	       new GetSkuTask().execute();
	   }
	   
	   @Override
	   public void onServiceDisconnected(ComponentName name) 
	   {
	       mService = null;
	   }
	};
	
	@Override
	public void onDestroy() 
	{
	    super.onDestroy();
	    if (mServiceConn != null) 
	    	unbindService(mServiceConn);
	}
	
	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3)
	{
		DonateItem item = items.get(position);
		
		if (item.type == DonateItem.TYPE_PLAY)
		{
			String sku = (String) item.extra;
			
			try
			{
				Bundle bundle = mService.getBuyIntent(3, getPackageName(), sku, "inapp", "donate_" + System.currentTimeMillis());
				if (bundle.getInt("RESPONSE_CODE", -1) == 0)
				{
					PendingIntent pendingIntent = bundle.getParcelable("BUY_INTENT");
					startIntentSenderForResult(pendingIntent.getIntentSender(), 1001, new Intent(), 0, 0, 0);
					GA.trackClick("DonateActivity > " + sku);
				}
			}
			catch (Exception e)
			{
				GA.trackException(e);
			}
		}
		
		if (item.type == DonateItem.TYPE_NORMAL)
		{
			if (Build.VERSION.SDK_INT < 11)
			{
				android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
				clipboard.setText(item.text);
			}
			else
			{
				android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
				clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.app_name), item.text));
			}
			
			GA.trackClick("DonateActivity > " + item.text);
			
			Toast.makeText(this, R.string.donate_05, Toast.LENGTH_SHORT).show();
		}
		
		if (item.type == DonateItem.TYPE_NAVIGATE)
		{
			GA.trackClick("DonateActivity > " + item.text);
			
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setData(Uri.parse((String)item.extra));
			startActivity(intent);
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if (requestCode == 1001) 
		{
			if (resultCode == RESULT_OK) 
			{
				try
				{
					JSONObject json = new JSONObject(data.getStringExtra("INAPP_PURCHASE_DATA"));
					GA.trackClick("DonateActivity > Success > " + json.optString("productId"));
					
					Toast.makeText(this, R.string.donate_03, Toast.LENGTH_LONG).show();
					finish();
				}
				catch (Exception e) 
				{
					GA.trackException(e);
				}
			}
			else 
			{
				GA.trackException("DonateActivity > Failure(" + resultCode + ")");
				Toast.makeText(this, R.string.donate_04, Toast.LENGTH_LONG).show();
			}
		}
	}
	
	private class GetSkuTask extends AsyncTask<Void, Void, HashMap<String, Sku>>
	{
		@Override
		protected void onPreExecute() {}
		
		@Override
		protected HashMap<String, Sku> doInBackground(Void... params)
		{
			Bundle bundle = new Bundle();
			bundle.putStringArrayList("ITEM_ID_LIST", skuList);

			try
			{
				bundle = mService.getSkuDetails(3, getPackageName(), "inapp", bundle);
				if (bundle.getInt("RESPONSE_CODE", -1) == 0)
				{
					HashMap<String, Sku> skus = new HashMap<String, Sku>();
					
					ArrayList<String> skuJsons = bundle.getStringArrayList("DETAILS_LIST");
					for (String skuJson : skuJsons)
					{
						Sku sku = Sku.parse(skuJson);
						if (sku != null) skus.put(sku.sku, sku);
					}
					
					return skus;
				}
			}
			catch (Exception e) 
			{
				GA.trackException(e);
			}
			
			return null;
		}
		
		@Override
		protected void onPostExecute(HashMap<String, Sku> skus)
		{
			if (skus != null)
			{
				for (String mySku : skuList)
				{
					Sku sku = skus.get(mySku);
					if (sku != null) resolvedSkuList.add(sku);
				}
			}
			
			skuLoaded = true;
			refrashDonateItems();
		}
	}
	
	
	private static class Sku
	{
		public String sku, price;
		
		public Sku(String sku, String price)
		{
			this.sku = sku;
			this.price = price;
		}
		
		public static Sku parse(String json)
		{
			try
			{
				JSONObject o = new JSONObject(json);
		        String sku = o.optString("productId");
		        String price = o.optString("price");
		        return new Sku(sku, price);
			}
			catch (Exception e)
			{
				GA.trackException(e);
			}
			
			return null;
		}
	}
}
