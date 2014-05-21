package com.enlighten.transparentproxy;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.os.Binder;
import android.os.Environment;
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
	private static final int SAVE_SOCAT_OUTPUT = 103;
	private InterceptorHandler mInterceptorHandler;
	private Handler callback;
	CommandCapture socatommand;

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
			} else if (msg.what == SAVE_SOCAT_OUTPUT) {
				saveSocatOutput();
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
		return new InterceptorLocalBinder();
	}

	public class InterceptorLocalBinder extends Binder {

		public Interceptor getService() {
			return Interceptor.this;
		}

	}

	public void startIntercepting(Handler callback, String domainName,
			ApplicationInfo appInfo) {

		if (!TextUtils.isEmpty(domainName) && null != appInfo
				&& null != callback) {
			this.domainName = domainName;
			this.appInfo = appInfo;
			this.callback = callback;
			interceptorThread = new HandlerThread("interceptor");
			interceptorThread.start();
			mInterceptorHandler = new InterceptorHandler(
					interceptorThread.getLooper());

			mInterceptorHandler.sendEmptyMessage(START_INTERCEPTING);
		}

	}

	public void stopIntercepting() {

		mInterceptorHandler.sendEmptyMessage(STOP_INTERCEPTING);

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
			callback.sendEmptyMessage(Constants.INVALID_DOMAIN_NAME);

		}
		return validDomain;
	}

	private void configureSocat() {

		String command = Constants.SOCAT_TRANSPARENT_PROXY_COMMAND.replace(
				Constants.DOMAIN_NAME, domainName);
		// run socat for 3 mins
		socatommand = new CommandCapture(Constants.CONFIGURE_SOCAT_COMMAND_ID,
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

			@Override
			public void commandOutput(int id, String line) {
				super.commandOutput(id, line);
				Message message = callback
						.obtainMessage(Constants.OUTPUT_UPDATED);
				message.obj = socatommand.toString();
				callback.sendMessage(message);

			}

		};

		runCommand(socatommand);
		callback.sendEmptyMessage(Constants.STARTED_INTERCEPTING);

	}

	private void runCommand(Command command) {
		try {
			RootTools.getShell(true).add(command);
		} catch (Exception e) {
			e.printStackTrace();
			AppLog.logDebug(TAG,
					"Exception while running command " + command.getCommand());
			callback.sendEmptyMessage(Constants.ERROR_WHILE_RUNNING);
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
					callback.sendEmptyMessage(Constants.STOPPED_INTERCEPTING);

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

	public void saveInterceptedData() {
		mInterceptorHandler.sendEmptyMessage(SAVE_SOCAT_OUTPUT);
	}

	private void saveSocatOutput() {
		BufferedWriter writer = null;
		if (null != socatommand) {
			File parentDir = Environment
					.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
			if (parentDir.exists()) {
				try {
					String fileName = parentDir.getAbsolutePath() + "/"
							+ Constants.INTERCEPTED_DATA_FILE_NAME + "_"
							+ (new Date()).getTime();
					File file = new File(fileName);
					if (file.exists()) {
						file.delete();
					}
					file.createNewFile();
					writer = new BufferedWriter(new OutputStreamWriter(
							new FileOutputStream(file)));

					writer.write(socatommand.toString());
					Message msg = callback.obtainMessage(Constants.DATA_SAVED);
					msg.obj = file.getAbsolutePath();
					callback.sendMessage(msg);
					return;
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					if (null != writer) {
						try {
							writer.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}

			}
		}

		callback.sendEmptyMessage(Constants.DATA_NOT_SAVED);
	}

}
