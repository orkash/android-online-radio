package org.nkuznetsov.onlineradio;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONObject;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import com.android.vending.billing.IInAppBillingService;

public class DonateActivity extends Activity
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
	
	View progress;
	LinearLayout buttons;
	TextView labelDonate;
	
	IInAppBillingService mService;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_donate);
		
		progress = findViewById(R.id.progress);
		buttons = (LinearLayout) findViewById(R.id.buttons);
		labelDonate = (TextView) findViewById(R.id.label_donate);
		
		bindService(new Intent("com.android.vending.billing.InAppBillingService.BIND"), mServiceConn, Context.BIND_AUTO_CREATE);
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
	    unbindService(mServiceConn);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if (requestCode == 1001) 
		{
			if (resultCode == RESULT_OK) 
			{
				Toast.makeText(this, R.string.donate_03, Toast.LENGTH_LONG).show();
				finish();
			}
			else labelDonate.setText(R.string.donate_04);
		}
	}
	
	private class GetSkuTask extends AsyncTask<Void, Void, HashMap<String, Sku>>
	{
		@Override
		protected void onPreExecute()
		{
			progress.setVisibility(View.VISIBLE);
			buttons.setVisibility(View.GONE);
		}
		
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
			progress.setVisibility(View.GONE);
			buttons.setVisibility(View.VISIBLE);
			
			if (skus != null)
			{
				int dp10 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics());
				int dp190 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 190, getResources().getDisplayMetrics());
				
				LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
				lp.topMargin = dp10;
				
				for (String mySku : skuList)
				{
					Sku sku = skus.get(mySku);
					if (sku != null)
					{
						Button btn = new Button(DonateActivity.this);
						btn.setText(getString(R.string.donate_02, sku.price));
						btn.setMinWidth(dp190);
						btn.setOnClickListener(new OnDonateClickListener(mySku));
						buttons.addView(btn, new LayoutParams(lp));
					}
				}
			}
			else labelDonate.setText(R.string.donate_04);
		}
	}
	
	private class OnDonateClickListener implements OnClickListener
	{
		String sku;
		
		public OnDonateClickListener(String sku)
		{
			this.sku = sku;
		}

		@Override
		public void onClick(View v)
		{
			try
			{
				Bundle bundle = mService.getBuyIntent(3, getPackageName(), sku, "inapp", "donate_" + System.currentTimeMillis());
				if (bundle.getInt("RESPONSE_CODE", -1) == 0)
				{
					PendingIntent pendingIntent = bundle.getParcelable("BUY_INTENT");
					startIntentSenderForResult(pendingIntent.getIntentSender(), 1001, new Intent(), 0, 0, 0);
				}
			}
			catch (Exception e)
			{
				GA.trackException(e);
			}
		}
	}
	
	
	private static class Sku
	{
		public String sku, price, title, description;
		
		public Sku(String sku, String price, String title, String description)
		{
			this.sku = sku;
			this.price = price;
			this.title = title;
			this.description = description;
		}
		
		public static Sku parse(String json)
		{
			try
			{
				JSONObject o = new JSONObject(json);
		        String sku = o.optString("productId");
		        String price = o.optString("price");
		        String title = o.optString("title");
		        String description = o.optString("description");
		        
		        return new Sku(sku, price, title, description);
			}
			catch (Exception e)
			{
				GA.trackException(e);
			}
			
			return null;
		}
	}
}
