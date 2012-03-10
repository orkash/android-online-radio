package org.nkuznetsov.onlineradio.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class IOUtils 
{
	public static byte[] readBytes(InputStream inputStream) throws IOException 
	{
		ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
		int bufferSize = 1024*8;
		byte[] buffer = new byte[bufferSize];
		int len = 0;
		while ((len = inputStream.read(buffer)) != -1) 
			byteBuffer.write(buffer, 0, len);
		return byteBuffer.toByteArray();
	}
}
