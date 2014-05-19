package com.enlighten.transparentproxy.app;

import java.lang.Thread.UncaughtExceptionHandler;

import android.app.Application;
import android.content.res.Configuration;
import android.util.Log;

import com.enlighten.transparentproxy.constants.Constants;

public class ProxyApplication extends Application {

	private static final String TAG = "TransparentProxyApp";

	// set from application init
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public void onCreate() {
		super.onCreate();

		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {

			@Override
			public void uncaughtException(Thread thread, Throwable ex) {
				ex.printStackTrace();
				Log.d(TAG, "caught unhandeled exception, exiting gracefully");
				System.exit(1);

			}
		});

		// It has to be called before app init as all paths are defined there
		Constants.initPaths(getFilesDir().getAbsolutePath());

	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
		AppLog.logDebug(TAG, "Low memory condition is detected");
	}

	@Override
	public void onTerminate() {
		super.onTerminate();
	}

}
