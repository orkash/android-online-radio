package org.nkuznetsov.onlineradio.utils;

import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Stack;

import org.nkuznetsov.onlineradio.network.DownloadManager;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;

public class ImageManager 
{
	private HashMap<String, SoftReference<Bitmap>> cache = new HashMap<String, SoftReference<Bitmap>>();
	Thread imageManagerThread = new ImageManagerThread();
	private ImageQueue imageManagerQueue = new ImageQueue();
	private int stub_id;
	
	public ImageManager(Context context, int defaultImageResourceId)
	{
		imageManagerThread.setPriority(Thread.NORM_PRIORITY - 1);
		stub_id = defaultImageResourceId;
	}
	
	private void setImage(ImageView imageView, int stub)
	{
		setImage((String)imageView.getTag(), imageView, stub);
	}
	
	public void setImage(String url,  ImageView imageView)
	{
		setImage(url, imageView, stub_id);
	}
	
	public void setImage(String url,  ImageView imageView, int stub)
	{
		imageView.setTag(url);
		if (cache.containsKey(url))
		{
			SoftReference<Bitmap> sBitmap = cache.get(url);
			if (sBitmap != null)
			{
				Bitmap bitmap = sBitmap.get();
				if (bitmap != null)
				{
					imageView.setImageBitmap(bitmap);
					return;
				}
			}
			cache.remove(url);
		}
		imageView.setImageResource(stub);
		getImage(url, imageView, stub);
	}
	
	private void getImage(String url,  ImageView imageView, int stub)
	{
		imageManagerQueue.Clean(imageView);
		
		ImageToLoad imageToLoad = new ImageToLoad(url, imageView, stub);
		synchronized (imageManagerQueue.imagesToLoad) 
		{
			imageManagerQueue.imagesToLoad.push(imageToLoad);
			imageManagerQueue.imagesToLoad.notifyAll();
		}
 
		if (imageManagerThread.getState() == Thread.State.NEW) imageManagerThread.start();
	}
	
	private Bitmap getBitmap(String url)
	{
		try 
		{
			InputStream is = DownloadManager.getStreamHttpGETRequest(url, 172800);
			return BitmapFactory.decodeStream(is);
		} 
		catch (Exception e) 
		{
			return null;
		}
	}
	
	//
	// Private classes
	//
	private class ImageToLoad 
	{
		public String url;
		public ImageView imageView;
		public int stub;
 
		public ImageToLoad(String u, ImageView i, int s) 
		{
			url = u;
			imageView = i;
			stub = s;
		}
	}
	
	private class ImageQueue 
	{
		private Stack<ImageToLoad> imagesToLoad = new Stack<ImageToLoad>();
 
		public void Clean(ImageView image) 
		{
			for (int j = 0; j < imagesToLoad.size();) 
			{
				if (j < imagesToLoad.size() && imagesToLoad.get(j).imageView == image) imagesToLoad.remove(j);
				else ++j;
			}
		}
	}
	
	private class ImageManagerThread extends Thread 
	{
		public void run() 
		{
			try {
				while (true)
				{
					if (imageManagerQueue.imagesToLoad.size() == 0)
						synchronized (imageManagerQueue.imagesToLoad) 
						{
							imageManagerQueue.imagesToLoad.wait();
						}
					if (imageManagerQueue.imagesToLoad.size() != 0) 
					{
						ImageToLoad imageToLoad;
						synchronized (imageManagerQueue.imagesToLoad) 
						{
							imageToLoad = imageManagerQueue.imagesToLoad.pop();
						}
						Bitmap bmp = getBitmap(imageToLoad.url);
						cache.put(imageToLoad.url, new SoftReference<Bitmap>(bmp));
						if (imageToLoad.imageView.getTag() != null && ((String) imageToLoad.imageView.getTag()).equals(imageToLoad.url)) 
						{
							BitmapDisplayer bd = new BitmapDisplayer(bmp, imageToLoad.imageView, imageToLoad.stub);
							Activity a = (Activity) imageToLoad.imageView.getContext();
							a.runOnUiThread(bd);
						}
					}
					if (Thread.interrupted()) break;
				}
			} 
			catch (InterruptedException e) {}
		}
	}
	
	class BitmapDisplayer implements Runnable 
	{
		Bitmap bitmap;
		ImageView imageView;
		int stub;
 
		public BitmapDisplayer(Bitmap b, ImageView i, int s) 
		{
			bitmap = b;
			imageView = i;
			stub = s;
		}
 
		public void run() 
		{
			try
			{
				if (bitmap != null && !bitmap.isRecycled())
				{
					imageView.setImageBitmap(bitmap);
					return;
				}
			} catch (Exception e) {}
			setImage(imageView, stub);
		}
	}
}