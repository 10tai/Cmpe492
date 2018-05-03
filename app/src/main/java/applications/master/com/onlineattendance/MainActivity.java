/**
 * Copyright 2015 Daniel Martí
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package applications.master.com.onlineattendance;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.FileReader;
import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.List;

//import cc.mvdan.accesspoint.WifiApControl;
//import cc.mvdan.accesspoint.WifiApControl.Client;
//import cc.mvdan.accesspoint.WifiApControl.ReachableClientListener;

public class MainActivity extends Activity implements ActivityCompat.OnRequestPermissionsResultCallback,
		PermissionUtils.PermissionResultCallback{

	private WifiManager wifiManager;
	private WifiApControl apControl;
	private ClientArrayAdapter adapter;
	private PermissionUtils permissionUtils;
	ArrayList<String> permissions;

	private Button refresh;

	private static final int REQUEST_WRITE_SETTINGS = 1;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

//		refresh=(Button) findViewById(R.id.refresh_btn);
//		CheckForPermissions();

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(this)) {
			new AlertDialog.Builder(this)
				.setMessage("Allow reading/writing the system settings? Necessary to set up access points.")
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
						intent.setData(Uri.parse("package:" + getPackageName()));
//						intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

						startActivityForResult(intent, REQUEST_WRITE_SETTINGS);
					}
				}).show();
			return;
		}
		startControl();

//		refresh.setOnClickListener(new View.OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				int size= getConnectedDevicesMac().size();
//				for(int i=0;i<size;i++)
//				{
//					Toast.makeText(MainActivity.this,"connected users = "+getConnectedDevicesMac().get(i),Toast.LENGTH_LONG).show();
//
//				}
//
//			}
//		});
	} // end of oncreate method here


	private void CheckForPermissions(){
		permissions=new ArrayList<>();
		permissionUtils=new PermissionUtils(MainActivity.this);
	    permissions.add(Manifest.permission.WRITE_SETTINGS);
		permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
		permissions.add(Manifest.permission.ACCESS_NETWORK_STATE);
		permissions.add(Manifest.permission.ACCESS_WIFI_STATE);
		permissions.add(Manifest.permission.READ_PHONE_STATE);

		permissionUtils.check_permission(permissions,"Need permission for Application",1);
	}



	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQUEST_WRITE_SETTINGS:
			startControl();
			break;
		}
	}

	private void startControl() {
		adapter = new ClientArrayAdapter(this, new ArrayList<WifiApControl.Client>());
		ListView listView = (ListView) findViewById(R.id.clientlist);
		listView.setAdapter(adapter);

		wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

		apControl = WifiApControl.getInstance(this);

		new Thread() {
			@Override
			public void run() {
				try {
					while (!isInterrupted()) {
						Thread.sleep(1000);
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								refresh();
							}
						});
					}
				} catch (InterruptedException e) {
					// ignored
				}
			}
		}.start();
	}

	@Override
	public void onResume() {
		super.onResume();
		refresh();
	}

	private static String stateString(int state) {
		switch (state) {
		case WifiApControl.STATE_FAILED:
			return "FAILED";
		case WifiApControl.STATE_DISABLED:
			return "DISABLED";
		case WifiApControl.STATE_DISABLING:
			return "DISABLING";
		case WifiApControl.STATE_ENABLED:
			return "ENABLED";
		case WifiApControl.STATE_ENABLING:
			return "ENABLING";
		default:
			return "UNKNOWN!";
		}
	}

	private void updateText() {
		StringBuilder sb = new StringBuilder();

		if (!WifiApControl.isSupported()) {
			sb.append("Warning: Wifi AP mode not supported!\n");
			sb.append("You should get unknown or zero values below.\n");
			sb.append("If you don't, isSupported() is probably buggy!\n");
		}

		if (apControl == null) {
			sb.append("Something went wrong while trying to get AP control!\n");
			sb.append("Make sure to grant the app the WRITE_SETTINGS permission.");
			TextView tv = (TextView) findViewById(R.id.apinfo);
			tv.setText(sb.toString());
			return;
		}

		int state = apControl.getState();
		sb.append("State: ").append(stateString(state)).append('\n');

		boolean enabled = apControl.isEnabled();
		sb.append("Enabled: ").append(enabled ? "YES" : "NO").append('\n');

		WifiConfiguration config = apControl.getConfiguration();
		sb.append("WifiConfiguration:");
		if (config == null) {
			sb.append(" null\n");
		} else {
			sb.append("\n");
			sb.append("   Hotspot Name: \"").append(config.SSID).append("\"\n");
			sb.append("   Hotspot Password: \"").append(config.preSharedKey).append("\"\n");
		}

		Inet4Address addr4 = apControl.getInet4Address();

		TextView tv = (TextView) findViewById(R.id.apinfo);
		tv.setText(sb.toString());
	}

	@Override
	public void PermissionGranted(int request_code) {
		if(request_code==1)
		{
			startControl();
		}

	}

	@Override
	public void PartialPermissionGranted(int request_code, ArrayList<String> granted_permissions) {

	}

	@Override
	public void PermissionDenied(int request_code) {

	}

	@Override
	public void NeverAskAgain(int request_code) {

	}

	private class ClientArrayAdapter extends ArrayAdapter<WifiApControl.Client> {

		private boolean[] reachable;

		public ClientArrayAdapter(Context context, List<WifiApControl.Client> clients) {
			super(context, 0, clients);
			this.reachable = new boolean[clients.size()];
		}

//		@TargetApi(Build.VERSION_CODES.HONEYCOMB)
		private void compatAddAll(List<WifiApControl.Client> clients) {
//			if (Build.VERSION.SDK_INT < 11) {
				for (WifiApControl.Client client : clients) {
					add(client);
				}
//				return;
//			}
			addAll(clients);
		}

		public void setClients(List<WifiApControl.Client> clients) {
			clear();
			if (clients != null) {
				compatAddAll(clients);
				reachable = new boolean[clients.size()];
			}
			notifyDataSetChanged();
		}

		public void setReachable(WifiApControl.Client client) {
			int position = getPosition(client);
			if (position < 0) {
				return;
			}
			reachable[position] = true;
			notifyDataSetChanged();
		}

		@Override
		public View getView(int position, View view, ViewGroup parent) {
			ViewHolder holder;

			if (view == null) {
				LayoutInflater inflater = LayoutInflater.from(getContext());
				view = inflater.inflate(R.layout.clientlistitem, parent, false);

				holder = new ViewHolder();
				holder.desc = (TextView) view.findViewById(R.id.client_desc);
				holder.reach = (TextView) view.findViewById(R.id.client_reach);

				view.setTag(holder);
			} else {
				holder = (ViewHolder) view.getTag();
			}

			WifiApControl.Client client = getItem(position);
			holder.desc.setText(client.ipAddr + " :" + client.hwAddr);
			return view;
		}

		private class ViewHolder {
			TextView desc;
			TextView reach;
		}
	}

	private void listClients(int timeout) {
		if (apControl == null) {
			return;
		}
		List<WifiApControl.Client> clients = apControl.getReachableClients(timeout,
				new WifiApControl.ReachableClientListener() {
			public void onReachableClient(final WifiApControl.Client client) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						adapter.setReachable(client);
					}
				});
			}
			public void onComplete() { }
		});
		adapter.setClients(clients);
	}

	private void refresh() {
//		Runtime runtime = Runtime.getRuntime();
//		Process proc = null;
//		try {
//			proc = runtime.exec("ping -c 1   ip address here" );
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		try {
//			proc.waitFor();
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
		updateText();
		listClients(300);
	}

	public void refresh(View view) {
		refresh();
	}

	private void enable() {
		wifiManager.setWifiEnabled(false);
		apControl.enable();
		refresh();
	}

	private void enableButtonAfter(final Button button, final long time) {
		new Thread() {
			@Override
			public void run() {
				try {
					Thread.sleep(time);
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							button.setEnabled(true);
						}
					});
				} catch (InterruptedException e) { }
			}
		}.start();
	}

	public void enable(View view) {
		Button button = (Button) view;
		button.setEnabled(false);
		enable();
		enableButtonAfter(button, 1000);
	}

	private void disable() {
		apControl.disable();
		wifiManager.setWifiEnabled(true);
		refresh();
	}

	public void disable(View view) {
		Button button = (Button) view;
		button.setEnabled(false);
		disable();
		enableButtonAfter(button, 1000);
	}



	public static ArrayList<String> getConnectedDevicesMac()
	{
		ArrayList<String> res = new ArrayList<String>();
		//NetManager.updateArpFile();

		BufferedReader br;
		try
		{
			br = new BufferedReader(new FileReader("/proc/net/arp"));
			String line;
			line = br.readLine();
			while ((line = br.readLine()) != null)
			{
				String[] sp = line.split(" +");
				if (sp[3].matches("..:..:..:..:..:.."))
					res.add(sp[3]);
			}

			br.close();
		}
		catch (Exception e)
		{}

		return res;
	} // end of gteconnecteddevicesmac method here





}
