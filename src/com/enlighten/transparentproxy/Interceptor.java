package com.enlighten.transparentproxy;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.enlighten.transparentproxy.app.AppLog;
import com.enlighten.transparentproxy.constants.Constants;
import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.execution.Command;
import com.stericson.RootTools.execution.CommandCapture;
import com.stericson.RootTools.execution.Shell;

public class Interceptor extends Service {

	private String domainName;
	private String hostAddress;
	private ApplicationInfo appInfo;
	private static final String TAG = "INTERCEPTOR_SERVICE";
	private HandlerThread interceptorThread;
	private static final int START_INTERCEPTING = 101;
	private static final int STOP_INTERCEPTING = 102;
	private InterceptorHandler mInterceptorHandler;
	private Handler callback;

	private class InterceptorHandler extends Handler {

		public InterceptorHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			if (msg.what == START_INTERCEPTING) {
				if (getIpForDomain()) {
					runComnads();

				}
			} else if (msg.what == STOP_INTERCEPTING) {
				stopShellAndSocat();
			}
		}

	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
		AppLog.logDebug(TAG, "service low memory");
	}

	@Override
	public void onCreate() {
		super.onCreate();
		AppLog.logDebug(TAG, "service created");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		AppLog.logDebug(TAG, "service destroyed");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		AppLog.logDebug(TAG, "service onStartCommand");
		return super.onStartCommand(intent, flags, startId);

	}

	@Override
	public IBinder onBind(Intent intent) {
		domainName = intent.getStringExtra(Constants.DOMAIN_NAME_PARAM);
		appInfo = (ApplicationInfo) intent
				.getParcelableExtra(Constants.APP_INFO);
		if (!TextUtils.isEmpty(domainName) && null != appInfo) {
			return new InterceptorLocalBinder();
		}
		return null;
	}

	public class InterceptorLocalBinder extends Binder {

		public Interceptor getService() {
			return Interceptor.this;
		}

	}

	public void startIntercepting(Handler callback) {
		this.callback = callback;
		interceptorThread = new HandlerThread("interceptor");
		mInterceptorHandler = new InterceptorHandler(
				interceptorThread.getLooper());

		interceptorThread.start();

		Message msg = mInterceptorHandler.obtainMessage(START_INTERCEPTING);
		mInterceptorHandler.sendMessage(msg);

	}

	public void stopIntercepting() {

		Message msg = mInterceptorHandler.obtainMessage(STOP_INTERCEPTING);
		mInterceptorHandler.sendMessage(msg);

	}

	private void runComnads() {

		File file = new File(Constants.SERVER_CSR_FILE_PATH);
		if (file.exists()) {
			file.delete();
		}
		file = new File(Constants.SERVER_CERT_FILE_PATH);
		if (file.exists()) {
			file.delete();
		}

		String killSocatComand = Constants.SOCAT_KILL_COMMAND;
		String clearNatCommand = Constants.IPTABLES_NAT_TABLE_CLEAR_COMMAND;

		String addRuleCommand = Constants.IPTABLES_APP_LEVEL_FILTER_COMMAND
				.replace(Constants.UID, String.valueOf(appInfo.uid)).replace(
						Constants.DESTINATION_IP, hostAddress);

		String generateFakeServerCSRCommand = Constants.OPENSSL_CREATE_SERVER_CSR_COMMAND
				.replace(Constants.DOMAIN_NAME, domainName);

		String generateFakeServerCertificateCommand = Constants.OPENSSL_CREATE_CERTIFICATE_COMMAND;

		CommandCapture commandSet = new CommandCapture(101, killSocatComand,
				clearNatCommand, addRuleCommand, generateFakeServerCSRCommand,
				generateFakeServerCertificateCommand) {
			@Override
			public void commandCompleted(int id, int exitcode) {
				super.commandCompleted(id, exitcode);
				configureSocat();
			}

			@Override
			public void commandTerminated(int id, String reason) {
				super.commandTerminated(id, reason);
			}

		};
		runCommand(commandSet);

	}

	private boolean getIpForDomain() {
		boolean validDomain = true;
		try {
			String domain = domainName;
			InetAddress[] addresses = InetAddress.getAllByName(domain);
			if (null != addresses && addresses.length > 0) {
				hostAddress = addresses[0].getHostAddress();
				boolean isNameAlreadySaved = false;
				SharedPreferences preferences = PreferenceManager
						.getDefaultSharedPreferences(getApplicationContext());
				String domainNames = preferences.getString(
						Constants.PREVIOUS_DOMAIN_NAMES, null);
				if (!TextUtils.isEmpty(domainNames)) {
					String[] names = domainNames.split(",");

					List<String> nameList = Arrays.asList(names);
					if (nameList.contains(domain)) {
						isNameAlreadySaved = true;
					}
				}

				if (!isNameAlreadySaved) {
					if (null == domainNames) {
						domainNames = "google.com";
					}
					domainNames += "," + domain;
					preferences
							.edit()
							.putString(Constants.PREVIOUS_DOMAIN_NAMES,
									domainNames).commit();

				}

			}

		} catch (UnknownHostException e) {
			e.printStackTrace();
			validDomain = false;
			Message msg = callback.obtainMessage(Constants.INVALID_DOMAIN_NAME);
			callback.handleMessage(msg);

		}
		return validDomain;
	}

	private void configureSocat() {

		String command = Constants.SOCAT_TRANSPARENT_PROXY_COMMAND.replace(
				Constants.DOMAIN_NAME, domainName);
		// run socat for 2 mins
		CommandCapture socatommand = new CommandCapture(
				Constants.CONFIGURE_SOCAT_COMMAND_ID,
				Constants.SOCAT_COMMAND_TIMEOUT, command) {
			@Override
			public void commandCompleted(int id, int exitcode) {
				super.commandCompleted(id, exitcode);
				AppLog.logDebug(TAG, "Socat configured");

			}

			@Override
			public void commandTerminated(int id, String reason) {
				super.commandTerminated(id, reason);

			}

		};

		runCommand(socatommand);
		Message message = callback
				.obtainMessage(Constants.STARTED_INTERCEPTING);
		callback.handleMessage(message);

	}

	private void runCommand(Command command) {
		try {
			RootTools.getShell(true).add(command);
		} catch (Exception e) {
			e.printStackTrace();
			AppLog.logDebug(TAG,
					"Exception while running command " + command.getCommand());
			Message message = callback
					.obtainMessage(Constants.ERROR_WHILE_RUNNING);
			callback.handleMessage(message);
		}
	}

	private void stopShellAndSocat() {
		try {
			Shell.closeRootShell();
			CommandCapture commandCapture = new CommandCapture(
					Constants.STOP_HACKING_COMMAND_ID,
					Constants.SOCAT_KILL_COMMAND,
					Constants.IPTABLES_NAT_TABLE_CLEAR_COMMAND) {
				@Override
				public void commandCompleted(int id, int exitcode) {
					super.commandCompleted(id, exitcode);
					AppLog.logDebug(TAG, "Stopped hacking");
					Message message = callback
							.obtainMessage(Constants.STOPPED_INTERCEPTING);
					callback.handleMessage(message);
					interceptorThread.interrupt();

				}

				@Override
				public void commandTerminated(int id, String reason) {
					super.commandTerminated(id, reason);
				}
			};
			runCommand(commandCapture);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
