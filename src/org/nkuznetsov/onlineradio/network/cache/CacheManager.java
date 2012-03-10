package org.nkuznetsov.onlineradio.network.cache;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.nkuznetsov.onlineradio.exceptions.CacheObjectExpiredExeption;

import android.content.Context;
import android.os.Environment;

public class CacheManager 
{
	private Context context;
	private HashMap<String, CacheObjectInfo> cacheList;
	
	public CacheManager(Context context)
	{
		this.context = context;
		
		cacheList = new HashMap<String, CacheObjectInfo>();
		
		// chahce dirs: 
		ArrayList<File> chacheDirs = new ArrayList<File>();
		chacheDirs.add(getInternalDeviceCacheDirectory());
		chacheDirs.add(getSDCacheDirectory());
		
		// chache files:
		ArrayList<File> cacheFiles = new ArrayList<File>();
		
		for (File cacheDir : chacheDirs) 
		{
			if (cacheDir != null)
			{
				File[] list = cacheDir.listFiles();
				if (list != null) cacheFiles.addAll(Arrays.asList(list));
			}
		}
		
		for (File x : cacheFiles)
		{
			if (x.isFile())
			{
				if (x.getName().length() == 43)
				{
					String key = x.getName().substring(0, 32);
					
					if (Long.valueOf(x.getName().substring(33)) > System.currentTimeMillis() / 1000L)
					{
						CacheObjectInfo fileInfo = new CacheObjectInfo(x.getName(), x.getAbsoluteFile(), x.lastModified(), x.length());
						cacheList.put(key, fileInfo);
					} else x.delete();
				} //else x.delete();
			}
		}
	}
	
	public byte[] put(String fName, long expires, byte[] data) throws FileNotFoundException, IOException
	{
		CacheObject cache = new CacheObject(expires, data);
		
		File file;
		if (getSDCacheDirectory() != null) file = new File(getSDCacheDirectory(), fName + "_" + String.valueOf(expires));
		else file = new File(getInternalDeviceCacheDirectory(), fName);
		
		ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(file));
		
		os.writeObject(cache);
		
		os.flush();
		os.close();
		
		cacheList.put(fName, new CacheObjectInfo(fName, file.getAbsoluteFile(), file.lastModified(), file.length()));
		
		return data;
	}
	
	public InputStream get(String fName) throws StreamCorruptedException, FileNotFoundException, IOException, ClassNotFoundException, CacheObjectExpiredExeption
	{
		File file = cacheList.get(fName).getPath();
		
		ObjectInputStream is = new ObjectInputStream(new FileInputStream(file));
		
		CacheObject cache = (CacheObject) is.readObject();
		
		is.close();
		
		if (cache.isExpired()) 
		{
			file.delete();
			cacheList.remove(file.getName());
			throw new CacheObjectExpiredExeption();
		}
		
		return new ByteArrayInputStream(cache.getData());
	}
	
	public boolean exists(String fName)
	{
		return cacheList.containsKey(fName);
	}
	
	public void clear()
	{
		for (File x : getInternalDeviceCacheDirectory().listFiles()) x.delete();
		File externalCache = getSDCacheDirectory();
		if (externalCache != null) for (File x : externalCache.listFiles()) x.delete();
		
		cacheList.clear();
	}
	
	private File getInternalDeviceCacheDirectory()
	{
		File path = context.getCacheDir();
		if (!path.exists()) 
			path.mkdirs();
		return path;
	}
	
	private File getSDCacheDirectory()
	{
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state))
		{
			File path = new File(Environment.getExternalStorageDirectory(), "/Android/data/"+context.getPackageName()+"/cache/");
			if (!path.exists()) 
				path.mkdirs();
			return path;
		}
		return null;
	}
	
	/*
	public void removeExpired()
	{
		Thread thread = new Thread()
		{
			@Override
			public void run() 
			{
				for (File x : getInternalDeviceCacheDirectory().listFiles()) 
					if (x.isFile() && Long.valueOf(x.getName().substring(33)) <= System.currentTimeMillis() / 1000L) 
						x.delete();
				
				File externalCache = getSDCacheDirectory();
				if (externalCache != null)
				{
					File[] cacheFiles = externalCache.listFiles();
					if (cacheFiles != null)
					{
						for (File x : cacheFiles) 
							if (Long.valueOf(x.getName().substring(33)) <= System.currentTimeMillis() / 1000L) 
								x.delete();
					}
				}
				
				if (interrupted()) return;
			}
		};
		
		thread.run();
	}*/
	
	public long getCachSize()
	{
		long size = 0;
		for (CacheObjectInfo x : cacheList.values()) 
		{
			size += x.getSize();
		}
		return size;
	}
	
	public void remove(String fName)
	{
		if (cacheList.containsKey(fName))
		{
			cacheList.get(fName).getPath().delete();
			cacheList.remove(fName);
		}
	}
}
