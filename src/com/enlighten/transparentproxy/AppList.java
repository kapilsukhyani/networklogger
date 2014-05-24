package com.enlighten.transparentproxy;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.enlighten.transparentproxy.app.AppLog;
import com.enlighten.transparentproxy.constants.Constants;
import com.enlighten.transparentproxy.utils.Utility;
import com.stericson.RootTools.RootTools;

public class AppList extends ListActivity implements OnItemClickListener {
	private static final String TAG = "AppList";
	private AppInfoLoader loader;
	private List<ApplicationInfo> mInstalledApps = new ArrayList<ApplicationInfo>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.app_list);
		AppLog.logDebug(TAG, "Applist oncreate");
		checkRoot();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_search:
			openSearch();
			return true;
		case R.id.action_settings:
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void openSearch() {
		onSearchRequested();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);

		if (intent.getAction().equals(Intent.ACTION_SEARCH)) {
			performSearch(intent.getStringExtra(SearchManager.QUERY));
		}
	}

	private void performSearch(String query) {
		Toast.makeText(this, "search starting", Toast.LENGTH_SHORT).show();
		ListAdapter adapter = getListView().getAdapter();
		if (null != adapter) {
			((AppInfoListAdapter) adapter).filterItems(query);
		}

	}

	private class AppInfoLoader extends
			AsyncTask<Void, Void, List<ApplicationInfo>> {

		@Override
		protected List<ApplicationInfo> doInBackground(Void... params) {

			return loadApps();
		}

		private List<ApplicationInfo> loadApps() {
			List<ApplicationInfo> apps = null;
			apps = getPackageManager().getInstalledApplications(
					PackageManager.GET_META_DATA);
			return apps;
		}

		@Override
		protected void onPostExecute(List<ApplicationInfo> result) {
			super.onPostExecute(result);
			mInstalledApps.addAll(result);

			getListView().setAdapter(new AppInfoListAdapter(result));
		}
	}

	private class AppInfoListAdapter extends BaseAdapter {
		private List<ApplicationInfo> apps = null;

		public AppInfoListAdapter(List<ApplicationInfo> apps) {
			if (apps == null) {
				throw new IllegalArgumentException();
			}
			this.apps = apps;
		}

		@Override
		public int getCount() {

			return apps.size();
		}

		@Override
		public Object getItem(int arg0) {
			return apps.get(arg0);
		}

		@Override
		public long getItemId(int position) {
			return apps.get(position).uid;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (null == convertView) {
				convertView = getLayoutInflater().inflate(
						android.R.layout.simple_list_item_1, null);
			}

			((TextView) convertView).setText(apps.get(position).packageName);
			convertView.setTag(apps.get(position));

			return convertView;
		}

		public void filterItems(String query) {
			for (ApplicationInfo info : apps) {
				if (info.packageName.contains(query)) {

					int itemindex = apps.indexOf(info);
					getListView().smoothScrollToPosition(itemindex);
					break;
				}
			}
		}

	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {

		ApplicationInfo appInfo = (ApplicationInfo) arg1.getTag();
		if (appInfo.packageName.equals("com.enlighten.transparentproxy")) {
			Toast.makeText(this,
					"Network activity cannot be logged for Logger",
					Toast.LENGTH_SHORT).show();
		} else {
			Toast.makeText(this, "name " + appInfo.uid, Toast.LENGTH_SHORT)
					.show();

			Intent configureLoggerIntent = new Intent(AppList.this,
					ConfigureLogger.class);
			configureLoggerIntent.putExtra(Constants.APP_INFO, appInfo);
			startActivity(configureLoggerIntent);

		}
	}

	private void checkRoot() {
		if (!RootTools.isRootAvailable()) {
			Dialog d = new AlertDialog.Builder(this)
					.setMessage(R.string.root_not_available)
					.setCancelable(false)
					.setPositiveButton("OK", new OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
							AppList.this.finish();

						}
					}).create();
			d.show();
		} else if (!RootTools.isAccessGiven()) {

			Dialog d = new AlertDialog.Builder(this)
					.setMessage(R.string.root_denied).setCancelable(false)
					.setPositiveButton("OK", new OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
							AppList.this.finish();

						}
					}).create();
			d.show();
		} else {

			init();
		}
	}

	private void init() {
		initView();
		initFiles();

	}

	private void initFiles() {
		(new AsyncTask<Void, Void, Void>() {
			Dialog progressDialog;

			@Override
			protected void onPreExecute() {
				super.onPreExecute();
				progressDialog = ProgressDialog.show(AppList.this,
						"Initializing", "Pease wait a second");
			}

			@Override
			protected Void doInBackground(Void... params) {
				if (!PreferenceManager
						.getDefaultSharedPreferences(AppList.this).getBoolean(
								Constants.APPLICAITON_INITIALIZED, false)) {

					File file = getFilesDir();
					if (!file.exists()) {
						file.mkdir();
					}

					File logDir = new File(Constants.TRAFFIC_LOG_PATH);
					if (!logDir.exists()) {
						logDir.mkdir();
					}

					File opensslDir = new File(
							Constants.OPENSSL_WORKING_DIRECTORY_PATH);

					if (!opensslDir.exists()) {
						opensslDir.mkdir();

					}

					File rFile = new File(Constants.CA_CERT_FILE_PATH);
					Utility.copyStreamToFile(
							getResources().openRawResource(R.raw.ca), rFile);

					rFile = new File(Constants.CA_KEY_FILE_PATH);
					Utility.copyStreamToFile(
							getResources().openRawResource(R.raw.cauth), rFile);

					rFile = new File(Constants.SERVER_KEY_FILE_PATH);
					Utility.copyStreamToFile(
							getResources().openRawResource(R.raw.server), rFile);

					rFile = new File(Constants.SERIAL_FILE_PATH);
					Utility.copyStreamToFile(
							getResources().openRawResource(R.raw.serial), rFile);

					SharedPreferences preferences = PreferenceManager
							.getDefaultSharedPreferences(AppList.this);

					preferences
							.edit()
							.putBoolean(Constants.APPLICAITON_INITIALIZED, true)
							.commit();

				}

				if (!checkRequiredBinaries()) {
					Toast.makeText(
							AppList.this,
							"Not able to install prerequisites, cannot run app",
							Toast.LENGTH_LONG).show();
					finish();
				}

				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				super.onPostExecute(result);
				progressDialog.dismiss();
			}
		}).execute((Void) null);

	}

	private void initView() {
		getActionBar().setTitle(R.string.available_apps);
		loader = new AppInfoLoader();
		loader.execute((Void) null);
		getListView().setOnItemClickListener(this);
	}

	private boolean checkRequiredBinaries() {
		// assuming that the permission on that file is proper if it is found
		// via findBinay, although there is a method
		// checkUtil to check a binary and its permission together but it
		// returns false if permissions are not proper and
		// gives the impression that binary itself is not present
		if (RootTools.findBinary(Constants.OPENSSL)) {
			AppLog.logDebug(TAG, "Found openssl");
		} else {
			AppLog.logDebug(TAG, "openssl not found, installing binary");
			// install the binary as it is not available
			// install the binary as it is not available
			if (!RootTools.hasBinary(this, Constants.OPENSSL)) {

				if (RootTools.installBinary(this, R.raw.openssl, "openssl")) {
					Constants.updateOpensslCommandToInstalledBinaryPath();
				} else {
					return false;
				}

			} else {
				Constants.updateOpensslCommandToInstalledBinaryPath();
			}
		}

		if (RootTools.findBinary(Constants.SOCAT)) {
			AppLog.logDebug(TAG, "Found socat");
		} else {
			AppLog.logDebug(TAG, "socat not found, installing binary");
			// install the binary as it is not available
			if (!RootTools.hasBinary(this, Constants.SOCAT)) {
				if (RootTools.installBinary(this, R.raw.socat, "socat")) {
					Constants.updateSocatCommandToInstalledBinaryPath();

				} else {
					return false;
				}

			} else {
				Constants.updateSocatCommandToInstalledBinaryPath();
			}

		}

		if (RootTools.findBinary(Constants.BUSYBOX)) {
			AppLog.logDebug(TAG, "Found busybox");
		} else {
			AppLog.logDebug(TAG, "busybox not found, installing binary");
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					showBusyboxInstallationDialog();
				}
			});

		}

		return true;

	}

	private void showBusyboxInstallationDialog() {

		new Builder(AppList.this)
				.setTitle("Busybox is required to run this app.")
				.setMessage("Do you want to install it?")
				.setCancelable(false)
				.setPositiveButton("Install busybox",
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								Toast.makeText(
										AppList.this,
										"Remember to run the app after downloading the app to install busybox",
										Toast.LENGTH_LONG).show();
								dialog.dismiss();
								// install the binary as it is not available
								RootTools.offerBusyBox(AppList.this);
								finish();
							}
						})
				.setNegativeButton("No", new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						Toast.makeText(
								AppList.this,
								"Closing app as prerequisites are not available",
								Toast.LENGTH_LONG).show();
						AppList.this.finish();
					}
				}).create().show();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
	}

}
