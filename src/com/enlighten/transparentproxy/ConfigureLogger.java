package com.enlighten.transparentproxy;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
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
import com.enlighten.transparentproxy.constants.Constants;

public class ConfigureLogger extends Activity implements OnClickListener {
	private Button hackToggleButton;
	private AutoCompleteTextView domainName;
	private static final String TAG = "ConfigureLogger";
	private ApplicationInfo appInfo;
	private Dialog progressDialog;
	private TextView console;
	private boolean isHacking = false;
	private Interceptor interceptor;
	private Handler mCallbackHandler;
	private ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			interceptor = null;
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			interceptor = ((InterceptorLocalBinder) service).getService();
			interceptor.startIntercepting(mCallbackHandler, domainName
					.getText().toString().trim(), appInfo);
		}
	};

	public class CallbackHandler implements Callback {

		@Override
		public boolean handleMessage(Message msg) {
			if (msg.what == Constants.STARTED_INTERCEPTING) {
				progressDialog.dismiss();
				toggleHackButton("Stop Hacking", true);

			} else if (msg.what == Constants.STOPPED_INTERCEPTING) {
				progressDialog.dismiss();
				toggleHackButton("Start Hacking", false);
				showSaveInterceptedDataDialg(console.getText());
				console.setText("Intercepting: ");
				Toast.makeText(ConfigureLogger.this, "Stopped Intercepting",
						Toast.LENGTH_LONG).show();

			} else if (msg.what == Constants.INVALID_DOMAIN_NAME) {

				progressDialog.dismiss();
				domainName.setError("Invalid domain name dude");
			}

			else if (msg.what == Constants.OUTPUT_UPDATED) {
				String updatedOutput = (String) msg.obj;
				console.append(updatedOutput);
			}

			else if (msg.what == Constants.DATA_SAVED) {
				progressDialog.dismiss();
				String filePath = (String) msg.obj;
				Toast.makeText(getApplicationContext(),
						"Intercepted data saved in " + filePath + " file",
						Toast.LENGTH_LONG).show();
			} else if (msg.what == Constants.DATA_NOT_SAVED) {
				progressDialog.dismiss();
				Toast.makeText(getApplicationContext(),
						"Not able to save data", Toast.LENGTH_LONG).show();
			}

			return true;
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
				stopHacking(false);
			}

			break;

		default:
			break;
		}

	}

	private void startHacking() {
		progressDialog = ProgressDialog.show(ConfigureLogger.this,
				"Configuring", "Initializing setup");

		if (null == interceptor) {
			Intent intent = new Intent(this, Interceptor.class);
			if (!getApplicationContext().bindService(intent,
					mServiceConnection, BIND_AUTO_CREATE)) {
				progressDialog.dismiss();
				Toast.makeText(getApplicationContext(),
						"Not able to bind to the service", Toast.LENGTH_LONG)
						.show();

			}
		} else {
			interceptor.startIntercepting(mCallbackHandler, domainName
					.getText().toString().trim(), appInfo);
		}

	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (isHacking) {
			stopHacking(true);
		}

	}

	@Override
	public void onBackPressed() {
		showStopHackingAlert();
	}

	private void showStopHackingAlert() {
		if (isHacking) {
			Builder builder = new Builder(this);
			builder.setTitle("App will stop intercepting data");
			builder.setMessage("You sure you wanna do it? ");
			builder.setPositiveButton("Do it",
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
							ConfigureLogger.this.finish();
						}
					});
			builder.setNegativeButton("Abort",
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					});
			builder.create().show();
		} else {
			finish();
		}

	}

	private void stopHacking(boolean isFinishing) {

		if (!isFinishing) {
			progressDialog = ProgressDialog.show(ConfigureLogger.this,
					"Stopping", "Stopping porcesses");
		}
		if (null != interceptor) {
			interceptor.stopIntercepting();
		}

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

	private void showSaveInterceptedDataDialg(final CharSequence text) {

		Builder dialogBuilder = new Builder(this);
		dialogBuilder.setTitle("Do you want to save intercepted data?");
		dialogBuilder.setPositiveButton("YES",
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						interceptor.saveInterceptedData(text);
						dialog.dismiss();
						progressDialog = ProgressDialog.show(
								ConfigureLogger.this, "Please wait",
								"Saving your data");
					}
				});

		dialogBuilder.setNegativeButton("NO",
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {

						dialog.dismiss();
					}
				});
		dialogBuilder.create().show();
	}

}
