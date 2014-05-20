package com.enlighten.transparentproxy;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.enlighten.transparentproxy.Interceptor.InterceptorLocalBinder;
import com.enlighten.transparentproxy.app.AppLog;
import com.enlighten.transparentproxy.constants.Constants;
import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.execution.CommandCapture;
import com.stericson.RootTools.execution.Shell;

public class ConfigureLogger extends Activity implements OnClickListener {
	private Button hackToggleButton;
	private AutoCompleteTextView domainName;
	private static final String TAG = "ConfigureLogger";
	private String hostAddress;
	private ApplicationInfo appInfo;
	private Dialog setupProgressDialog;
	private TextView console;
	private boolean isHacking = false;
	private Interceptor interceptor;
	private Handler mCallbackHandler;

	public class CallbackHandler implements Callback {

		@Override
		public boolean handleMessage(Message msg) {
			if (msg.what == Constants.STARTED_INTERCEPTING) {
				setupProgressDialog.dismiss();
				toggleHackButton("Stop Hacking", true);

			} else if (msg.what == Constants.STOPPED_INTERCEPTING) {

			} else if (msg.what == Constants.INVALID_DOMAIN_NAME) {

				setupProgressDialog.dismiss();
				domainName.setError("Invalid domain name dude");
			}
			return false;
		}

	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_configure);
		mCallbackHandler = new Handler(new CallbackHandler());
		appInfo = (ApplicationInfo) getIntent().getExtras().get(
				Constants.APP_INFO);

		if (null != appInfo) {

			getActionBar().setTitle(appInfo.packageName);
			domainName = (AutoCompleteTextView) findViewById(R.id.domain_name);
			String savedNames = PreferenceManager.getDefaultSharedPreferences(
					this).getString(Constants.PREVIOUS_DOMAIN_NAMES, null);
			if (!TextUtils.isEmpty(savedNames)) {
				String[] names = savedNames.split(",");
				domainName.setAdapter(new ArrayAdapter<String>(this,
						android.R.layout.simple_dropdown_item_1line, names));
			}

			hackToggleButton = (Button) findViewById(R.id.start_hack);
			hackToggleButton.setOnClickListener(this);

			console = (TextView) findViewById(R.id.console);
		} else {
			Toast.makeText(this, "No application selected to be debugged",
					Toast.LENGTH_LONG).show();
			finish();
		}

	}

	@Override
	public void onClick(View v) {

		switch (v.getId()) {
		case R.id.start_hack:

			if (!isHacking) {
				if (TextUtils.isEmpty(domainName.getText().toString())) {
					domainName.setError("Please enter host to be hacked");
				} else {
					startHacking();
				}
			} else {
				stopHacking();
			}

			break;

		default:
			break;
		}

	}

	private void startHacking() {
		setupProgressDialog = ProgressDialog.show(ConfigureLogger.this,
				"Configuring", "Initializing setup");
		Intent intent = new Intent(this, Interceptor.class);

		if (null == interceptor) {
			boolean bound = getApplicationContext().bindService(intent,
					new ServiceConnection() {

						@Override
						public void onServiceDisconnected(ComponentName name) {
							interceptor = null;
						}

						@Override
						public void onServiceConnected(ComponentName name,
								IBinder service) {
							interceptor = ((InterceptorLocalBinder) service)
									.getService();
							interceptor.startIntercepting(mCallbackHandler,
									domainName.getText().toString().trim(),
									appInfo);
						}
					}, BIND_AUTO_CREATE);

			if (!bound) {
				setupProgressDialog.dismiss();
				Toast.makeText(getApplicationContext(),
						"Not able to bind to the service", Toast.LENGTH_LONG)
						.show();

			}
		} else {
			interceptor.startIntercepting(mCallbackHandler, domainName
					.getText().toString().trim(), appInfo);
		}

		/*
		 * (new AsyncTask<Void, Void, Void>() {
		 * 
		 * @Override protected void onPreExecute() { super.onPreExecute();
		 * setupProgressDialog = ProgressDialog.show(ConfigureLogger.this,
		 * "Configuring", "Initializing setup"); }
		 * 
		 * @Override protected Void doInBackground(Void... params) {
		 * 
		 * if (getIpForDomain()) { killPreviouslyRunnigSocat(); } return null; }
		 * 
		 * @Override protected void onPostExecute(Void result) {
		 * super.onPostExecute(result); } }).execute((Void) null);
		 */

	}

	private boolean getIpForDomain() {
		boolean validDomain = true;
		try {
			String domain = domainName.getText().toString().trim();
			InetAddress[] addresses = InetAddress.getAllByName(domain);
			if (null != addresses && addresses.length > 0) {
				hostAddress = addresses[0].getHostAddress();
				boolean isNameAlreadySaved = false;
				SharedPreferences preferences = PreferenceManager
						.getDefaultSharedPreferences(ConfigureLogger.this);
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
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					domainName.setError("Host entered is invalid dude");
				}
			});

			stopProgressDialog();

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
				Constants.DOMAIN_NAME, domainName.getText().toString().trim());
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
				Constants.DOMAIN_NAME, domainName.getText().toString().trim());
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
				runOnUiThread(new Runnable() {

					@Override
					public void run() {
						console.setText(console.getText() + " \n" + line);

					}
				});

			}

			@Override
			public void commandTerminated(int id, String reason) {
				super.commandTerminated(id, reason);
				showToastOnUiThread("socat process terminated");

			}

		};

		runCommand(socatommand);

		stopProgressDialog();
		toggleHackButton("Stop Hacking", true);

	}

	private void runCommand(CommandCapture command) {
		try {
			RootTools.getShell(true).add(command);
		} catch (Exception e) {
			e.printStackTrace();
			AppLog.logDebug(TAG,
					"Exception while running command " + command.getCommand());
			stopProgressDialog();
		}

	}

	private void stopProgressDialog() {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				setupProgressDialog.dismiss();
			}
		});
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	private void showToastOnUiThread(final String toast) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				Toast.makeText(ConfigureLogger.this, toast, Toast.LENGTH_LONG)
						.show();

			}
		});
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		stopHacking();
	}

	private void stopHacking() {

		/*
		 * setupProgressDialog = ProgressDialog.show(ConfigureLogger.this,
		 * "Stopping", "Stopping porcesses");
		 * 
		 * try { Shell.closeRootShell(); CommandCapture commandCapture = new
		 * CommandCapture( Constants.STOP_HACKING_COMMAND_ID,
		 * Constants.SOCAT_KILL_COMMAND,
		 * Constants.IPTABLES_NAT_TABLE_CLEAR_COMMAND) {
		 * 
		 * @Override public void commandCompleted(int id, int exitcode) {
		 * super.commandCompleted(id, exitcode); AppLog.logDebug(TAG,
		 * "Stopped hacking"); stopProgressDialog();
		 * toggleHackButton("Start Hacking", false); }
		 * 
		 * @Override public void commandTerminated(int id, String reason) {
		 * super.commandTerminated(id, reason); stopProgressDialog();
		 * showToastOnUiThread
		 * ("Not able to stop hacking, something went wrong"); } };
		 * runCommand(commandCapture);
		 * 
		 * } catch (IOException e) { e.printStackTrace(); }
		 */

	}

	private void toggleHackButton(final String buttonText, final boolean status) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				hackToggleButton.setText(buttonText);
				isHacking = status;

			}
		});
	}

}
