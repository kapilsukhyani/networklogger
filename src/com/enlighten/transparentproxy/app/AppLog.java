package com.enlighten.transparentproxy.app;

import android.util.Log;

import com.stericson.RootTools.RootTools;

public class AppLog {
	private static final String APP_TAG = "TransparentProxy";
	private static boolean debugEnabled = true;

	static {
		RootTools.debugMode = true;
	}

	public static int logInfo(String message) {
		return Log.i(APP_TAG, message);
	}

	public static int logDebug(String tag, String message) {
		if (debugEnabled) {
			return Log.d(tag, message);
		}

		return -1;
	}

}
