package com.enlighten.transparentproxy.app;

import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.TimeoutException;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.util.Log;

import com.enlighten.transparentproxy.constants.Constants;
import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.exceptions.RootDeniedException;
import com.stericson.RootTools.execution.CommandCapture;

public class ProxyApplication extends Application {

	private static final String TAG = "TransparentProxyApp";

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
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
	}

	@Override
	public void onTerminate() {
		super.onTerminate();
	}

	public void initIPTable() {
		try {
			ApplicationInfo info = getPackageManager().getApplicationInfo(
					getPackageName(), PackageManager.GET_META_DATA);

			CommandCapture initCommand = new CommandCapture(
					Constants.IPTABLES_INIT_COMMAND_ID,
					Constants.IPTABLES_INIT_COMMAND.replace(Constants.UID,
							info.uid + "")) {

				@Override
				protected void commandFinished() {
					super.commandFinished();
					String result = this.toString();
					AppLog.logDebug(TAG, "Init Ip Table commad result "
							+ result);

				}

				@Override
				public void commandTerminated(int id, String reason) {
					super.commandTerminated(id, reason);
					AppLog.logDebug(TAG, "Init Ip Table commad terminated "
							+ reason);
				}

				@Override
				public void commandCompleted(int id, int exitcode) {
					super.commandCompleted(id, exitcode);
					AppLog.logDebug(TAG, "Init Ip Table commad completed "
							+ exitcode);
				}
			};

			RootTools.getShell(true).add(initCommand);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (TimeoutException e) {
			e.printStackTrace();
		} catch (RootDeniedException e) {
			e.printStackTrace();
		} catch (NameNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

	}
}
