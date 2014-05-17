package com.enlighten.transparentproxy.utils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import android.content.Context;

public class Utility {
	public static String getLocalIpAddress() {
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface
					.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf
						.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress()) {
						return inetAddress.getHostAddress().toString();
					}
				}
			}
		} catch (SocketException ex) {
			ex.printStackTrace();
		}
		return null;
	}

	public static String convertStreamToString(InputStream is) {
		/*
		 * To convert the InputStream to String we use the Reader.read(char[]
		 * buffer) method. We iterate until the Reader return -1 which means
		 * there's no more data to read. We use the StringWriter class to
		 * produce the string.
		 */
		if (is != null) {
			Writer writer = new StringWriter();

			char[] buffer = new char[1024];
			try {
				Reader reader = new BufferedReader(new InputStreamReader(is,
						"UTF-8"));
				int n;
				while ((n = reader.read(buffer)) != -1) {
					writer.write(buffer, 0, n);
				}
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			return writer.toString();
		} else {
			return "";
		}
	}

	public static String openHTMLString(Context context, int id) {
		InputStream is = context.getResources().openRawResource(id);

		return Utility.convertStreamToString(is);
	}

	/**
	 * Copies the content of stream to the give file
	 * 
	 * @param stream
	 *            stream to be copied
	 * @param file
	 *            to which stream to be copied, creates file if not existed
	 * @return true if copy is successful
	 */
	public static boolean copyStreamToFile(InputStream stream, File file) {
		boolean copied = false;
		FileOutputStream fos = null;
		BufferedInputStream bis = null;
		try {
			if (file.exists()) {
				file.delete();
			}
			
			file.createNewFile();
			fos = new FileOutputStream(file);
			bis = new BufferedInputStream(stream);

			byte[] buffer = new byte[1024];
			while (-1 != bis.read(buffer)) {
				fos.write(buffer);
			}

			copied = true;
		} catch (IOException e) {
			e.printStackTrace();
			copied = false;
		} finally {
			try {
				if (null != bis) {
					bis.close();
				}
				if (null != fos) {
					fos.close();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return copied;
	}
}
