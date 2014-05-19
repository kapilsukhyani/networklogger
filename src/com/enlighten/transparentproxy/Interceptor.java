package com.enlighten.transparentproxy;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.enlighten.transparentproxy.app.AppLog;
import com.enlighten.transparentproxy.constants.Constants;
import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.execution.CommandCapture;

public class Interceptor extends IntentService {

	private String domainName;
	private String hostAddress;
	private ApplicationInfo appInfo;
	private static final String TAG = "INTERCEPTOR_SERVICE";

	public Interceptor(String name) {
		super(name);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		appInfo = (ApplicationInfo) intent
				.getParcelableExtra(Constants.APP_INFO);
		domainName = intent.getStringExtra(Constants.DOMAIN_NAME_PARAM);
	}

	@Override
	public IBinder onBind(Intent intent) {
		

		return new InterceptorLocalBinder();
	}

	public class InterceptorLocalBinder extends Binder {

		public IntentService getService() {
			return Interceptor.this;
		}

	}

	public void startIntercepting() {

	}

	public void stopIntercepting() {

	}

	private void startHacking() {

		if (getIpForDomain()) {
			killPreviouslyRunnigSocat();
		}

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

		}
		return validDomain;
	}

	private void killPreviouslyRunnigSocat() {

		CommandCapture killSocatCommand = new CommandCapture(
				Constants.KILL_SOCAT_COMMAND_ID, Constants.SOCAT_KILL_COMMAND) {
			@Override
			public void commandCompleted(int id, int exitcode) {
				super.commandCompleted(id, exitcode);
				AppLog.logDebug(TAG, "Killed previously running socat commands");
				clearIPtablesNatTable();
			}

			@Override
			protected void commandFinished() {
				super.commandFinished();
			}

			@Override
			public void commandOutput(int id, String line) {
				super.commandOutput(id, line);
			}

			@Override
			public void commandTerminated(int id, String reason) {
				super.commandTerminated(id, reason);
			}
		};

		runCommand(killSocatCommand);

	}

	private void clearIPtablesNatTable() {
		CommandCapture iptablesNatClearCommand = new CommandCapture(
				Constants.IPTABLES_NAT_CLEAR_COMMAND_ID,
				Constants.IPTABLES_NAT_TABLE_CLEAR_COMMAND) {
			@Override
			public void commandCompleted(int id, int exitcode) {
				super.commandCompleted(id, exitcode);
				AppLog.logDebug(TAG, "IPtables nat cleared");
				addIPTablesNatRule();
			}

			@Override
			protected void commandFinished() {
				super.commandFinished();
			}

			@Override
			public void commandOutput(int id, String line) {
				super.commandOutput(id, line);
			}

			@Override
			public void commandTerminated(int id, String reason) {
				super.commandTerminated(id, reason);
			}
		};

		runCommand(iptablesNatClearCommand);
	}

	private void addIPTablesNatRule() {
		String command = Constants.IPTABLES_APP_LEVEL_FILTER_COMMAND.replace(
				Constants.UID, String.valueOf(appInfo.uid)).replace(
				Constants.DESTINATION_IP, hostAddress);

		CommandCapture appLevelFilterCommand = new CommandCapture(
				Constants.IPTABLES_APP_LEVEL_FILTER_COMMAND_ID, command) {
			@Override
			public void commandCompleted(int id, int exitcode) {
				super.commandCompleted(id, exitcode);
				AppLog.logDebug(TAG, "App level filter created in nat");
				createCertificateRequest();

			}

			@Override
			protected void commandFinished() {
				super.commandFinished();
			}

			@Override
			public void commandOutput(int id, String line) {
				super.commandOutput(id, line);
			}

			@Override
			public void commandTerminated(int id, String reason) {
				super.commandTerminated(id, reason);
			}
		};

		runCommand(appLevelFilterCommand);

	}

	private void createCertificateRequest() {

		File file = new File(Constants.SERVER_CSR_FILE_PATH);
		if (file.exists()) {
			file.delete();
		}

		String command = Constants.OPENSSL_CREATE_SERVER_CSR_COMMAND.replace(
				Constants.DOMAIN_NAME, domainName);
		CommandCapture generateCsrCommand = new CommandCapture(
				Constants.GENERATE_FAKE_SERVER_CSR_COMAND_ID, command) {
			@Override
			public void commandCompleted(int id, int exitcode) {
				super.commandCompleted(id, exitcode);
				AppLog.logDebug(TAG, "Fakse server CSR generated");
				createCertificate();

			}

			@Override
			protected void commandFinished() {
				super.commandFinished();
			}

			@Override
			public void commandOutput(int id, String line) {
				super.commandOutput(id, line);
			}

			@Override
			public void commandTerminated(int id, String reason) {
				super.commandTerminated(id, reason);
			}

		};

		runCommand(generateCsrCommand);

	}

	private void createCertificate() {
		File file = new File(Constants.SERVER_CERT_FILE_PATH);
		if (file.exists()) {
			file.delete();
		}

		String command = Constants.OPENSSL_CREATE_CERTIFICATE_COMMAND;
		CommandCapture createCertCommand = new CommandCapture(
				Constants.GENERATE_FAKE_SERVER_CRT_COMMAND_ID, command) {
			@Override
			public void commandCompleted(int id, int exitcode) {
				super.commandCompleted(id, exitcode);
				AppLog.logDebug(TAG, "Fake server created");
				configureSocat();
			}

			@Override
			protected void commandFinished() {
				super.commandFinished();
			}

			@Override
			public void commandOutput(int id, String line) {
				super.commandOutput(id, line);
			}

			@Override
			public void commandTerminated(int id, String reason) {
				super.commandTerminated(id, reason);
			}
		};
		runCommand(createCertCommand);

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
			protected void commandFinished() {
				super.commandFinished();
			}

			@Override
			public void commandOutput(int id, final String line) {
				super.commandOutput(id, line);
				AppLog.logDebug(TAG, "hacked::: " + line);

			}

			@Override
			public void commandTerminated(int id, String reason) {
				super.commandTerminated(id, reason);

			}

		};

		runCommand(socatommand);

	}

	private void runCommand(CommandCapture command) {
		try {
			RootTools.getShell(true).add(command);
		} catch (Exception e) {
			e.printStackTrace();
			AppLog.logDebug(TAG,
					"Exception while running command " + command.getCommand());
		}

	}

}
