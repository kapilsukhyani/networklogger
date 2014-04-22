package com.enlighten.transparentproxy.app;

import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.TimeoutException;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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

	public boolean initIPTable(final Handler handler) {
		final SharedPreferences sharedPreferences = getSharedPreferences(
				Constants.Shared_Preferences, Context.MODE_PRIVATE);

		boolean initiated = sharedPreferences.getBoolean(
				Constants.IPTABLE_INITIATED, false);
		if (!initiated) {
			try {

				ApplicationInfo info = getPackageManager().getApplicationInfo(
						getPackageName(), PackageManager.GET_META_DATA);

				String command = Constants.IPTABLES_INIT_COMMAND.replace(
						Constants.UID, String.valueOf(info.uid));
				AppLog.logDebug(TAG, "running command " + command);
				CommandCapture initCommand = new CommandCapture(
						Constants.IPTABLES_INIT_COMMAND_ID, command) {

					@Override
					protected void commandFinished() {
						super.commandFinished();
						String result = this.toString();

						AppLog.logDebug(TAG, "Init Ip Table commad result "
								+ result);

						// TODO: write following logic in commandCompleted as it
						// returns a proper exitcode which can be compared to
						// see the response of iptables
						final Message message = Message.obtain(handler,
								Constants.IPTABLE_INITIATED_MESSAGE_ID);
						Bundle obj = new Bundle();
						/*
						 * try { int res = Integer.valueOf(result);
						 * 
						 * // iptables returns zero when ran properly if (res ==
						 * 0) {
						 */
						sharedPreferences.edit()
								.putBoolean(Constants.IPTABLE_INITIATED, true)
								.commit();

						obj.putBoolean(Constants.IPTABLE_INITIATED, true);

						/*
						 * } else { AppLog.logDebug(TAG, "something went wrong "
						 * + result);
						 * obj.putBoolean(Constants.IPTABLE_INITIATED, false);
						 * obj.putString(Constants.FAILURE_REASON,
						 * "Error while running iptables");
						 * 
						 * } } catch (NumberFormatException exception) {
						 * obj.putBoolean(Constants.IPTABLE_INITIATED, false);
						 * obj.putString(Constants.FAILURE_REASON,
						 * "Iptables not available");
						 * 
						 * }
						 */

						message.setData(obj);

						handler.post(new Runnable() {

							@Override
							public void run() {
								handler.handleMessage(message);

							}
						});

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
				return false;
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
		
		return true;

	}

	public void initAppLevelFilter(final Handler handler) {
		final SharedPreferences sharedPreferences = getSharedPreferences(
				Constants.Shared_Preferences, Context.MODE_PRIVATE);
		boolean filterEnabled = sharedPreferences.getBoolean(
				Constants.FILTER_ENABLED, false);
		if (filterEnabled) {
			// TODO delete previous filter first then call from commandCompleted

		} else {
			addSytemLevelFilter(handler);
		}

	}

	private void addSytemLevelFilter(final Handler handler) {
		final SharedPreferences sharedPreferences = getSharedPreferences(
				Constants.Shared_Preferences, Context.MODE_PRIVATE);

		try {
			final String command = Constants.SYSTEM_LEVEL_FILTER_COMMAND;
			AppLog.logDebug(TAG, "running command " + command);
			CommandCapture appFilterCommand = new CommandCapture(
					Constants.IPTABLES_INIT_COMMAND_ID, command) {
				@Override
				public void commandCompleted(int id, int exitcode) {
					super.commandCompleted(id, exitcode);
					sharedPreferences.edit()
							.putBoolean(Constants.FILTER_ENABLED, true)
							.putString(Constants.FILTER, command);

					final Message message = Message.obtain(handler,
							Constants.SYSTEM_LEVEL_FILTER_ENABLED_MESSAGE_ID);
					Bundle obj = new Bundle();

					obj.putBoolean(Constants.FILTER_ENABLED, true);

					message.setData(obj);

					handler.post(new Runnable() {

						@Override
						public void run() {
							handler.handleMessage(message);

						}
					});
				}

				@Override
				protected void commandFinished() {
					super.commandFinished();
				}
			};

			RootTools.getShell(true).add(appFilterCommand);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (TimeoutException e) {
			e.printStackTrace();
		} catch (RootDeniedException e) {
			e.printStackTrace();
		}

	}
}
