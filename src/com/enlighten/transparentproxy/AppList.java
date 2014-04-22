package com.enlighten.transparentproxy;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.NotificationManager;
import android.app.SearchManager;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
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
import com.enlighten.transparentproxy.app.ProxyApplication;
import com.enlighten.transparentproxy.constants.Constants;
import com.enlighten.transparentproxy.service.HTTPService;
import com.enlighten.transparentproxy.webserver.WebServer;
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
			initView();
			initCommand();

		}
	}

	private void initCommand() {
		if (((ProxyApplication) getApplication())
				.initIPTable(IpTableInitHandler)) {
//			startService(new Intent(AppList.this, HTTPService.class));
			startSrver();
		}
	}

	private void initView() {
		getActionBar().setTitle(R.string.available_apps);
		loader = new AppInfoLoader();
		loader.execute((Void) null);
		getListView().setOnItemClickListener(this);
	}

	public Handler IpTableInitHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			Bundle bundle = msg.getData();
			if (msg.what == Constants.IPTABLE_INITIATED_MESSAGE_ID) {

				if (bundle.getBoolean(Constants.IPTABLE_INITIATED)) {
					Toast.makeText(getApplicationContext(),
							"Network filter got set", Toast.LENGTH_LONG).show();

					((ProxyApplication) getApplication())
							.initAppLevelFilter(IpTableInitHandler);

				} else {
					String reason = bundle.getString(Constants.FAILURE_REASON);
					Toast.makeText(getApplicationContext(),
							reason + ", exiting", Toast.LENGTH_LONG).show();
					AppList.this.finish();
				}
			} else if (msg.what == Constants.SYSTEM_LEVEL_FILTER_ENABLED_MESSAGE_ID) {
				if (bundle.getBoolean(Constants.FILTER_ENABLED)) {
					Log.d(TAG, "Starting server");
//					startService(new Intent(AppList.this, HTTPService.class));
					startSrver();

				}

			}

		};
	};

	private WebServer server;

	private void startSrver() {
		if (server == null) {
			server = new WebServer(
					getApplicationContext(),
					(NotificationManager) getSystemService(NOTIFICATION_SERVICE));
		}

		server.startThread();
	}

}
