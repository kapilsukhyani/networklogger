package com.enlighten.transparentproxy.app;

import java.io.File;
import java.lang.Thread.UncaughtExceptionHandler;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.preference.PreferenceManager;
import android.util.Log;

import com.enlighten.transparentproxy.R;
import com.enlighten.transparentproxy.constants.Constants;
import com.enlighten.transparentproxy.utils.Utility;

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

		if (!PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
				Constants.APPLICAITON_INITIALIZED, false)) {
			init();
		}

	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
	}

	@Override
	public void onTerminate() {
		super.onTerminate();
	}

	

	private void init() {

		File file = getFilesDir();
		if (!file.exists()) {
			file.mkdir();
		}

		File opensslDir = new File(Constants.OPENSSL_WORKING_DIRECTORY_PATH);

		if (!opensslDir.exists()) {
			opensslDir.mkdir();

		}

		File rFile = new File(Constants.CA_CERT_FILE_PATH);
		Utility.copyStreamToFile(getResources().openRawResource(R.raw.ca),
				rFile);

		rFile = new File(Constants.CA_KEY_FILE_PATH);
		Utility.copyStreamToFile(getResources().openRawResource(R.raw.cauth),
				rFile);

		rFile = new File(Constants.SERVER_KEY_FILE_PATH);
		Utility.copyStreamToFile(getResources().openRawResource(R.raw.server),
				rFile);

		rFile = new File(Constants.SERIAL_FILE_PATH);
		Utility.copyStreamToFile(getResources().openRawResource(R.raw.serial),
				rFile);

		SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(this);

		preferences.edit().putBoolean(Constants.APPLICAITON_INITIALIZED, true)
				.commit();

	}

}
