package org.kll.hospitalmapper;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	Button send;
	EditText name;
	TextView notifyMessage;
	LocationManager locationManager;
	LocationListener locationListener;
	Location currentLocation;

	ProgressDialog waitingfix_dialog, waitingaccuracy_dialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		send = (Button) findViewById(R.id.send);
		name = (EditText) findViewById(R.id.name);
		notifyMessage = (TextView) findViewById(R.id.message);
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		locationListener = new MyLocationListener();
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
				5000, 2, locationListener);

		send.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {

				if (!locationManager
						.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
					buildAlertMessageNoGps();
					return;
				}

				if (name.getText().toString().matches("")) {
					Toast.makeText(getApplicationContext(),
							R.string.name_error_notset, Toast.LENGTH_LONG)
							.show();
					notifyMessage.setText(R.string.name_error_notset);
					return;
				}

				if (currentLocation != null) {
					if (currentLocation.getAccuracy() > 20.0) {
						Toast.makeText(getApplicationContext(),
								R.string.gps_error_nofix, Toast.LENGTH_LONG)
								.show();
						notifyMessage.setText(R.string.gps_error_nofix);
					} else {
						sendSms();

					}
				} else {
					Toast.makeText(getApplicationContext(),
							R.string.gps_error_nofix, Toast.LENGTH_LONG)
							.show();
					notifyMessage.setText(R.string.gps_error_nofix);
					return;
				}

			}
		});

	}

	private void sendSms() {

		SmsManager smsManager = SmsManager.getDefault();
		String message = "Name: " + name.getText().toString() + " \n Location: "
				+ String.valueOf(currentLocation.getLatitude()) + " "
				+ String.valueOf(currentLocation.getLongitude());
		smsManager.sendTextMessage("+9779841419975", null, message, null, null);
		Toast.makeText(getBaseContext(), R.string.sms_send_sucessful, Toast.LENGTH_LONG)
				.show();
		notifyMessage.setText(R.string.sms_send_sucessful);

	}

	private void buildAlertMessageNoGps() {
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.gps_error_off)
				.setCancelable(false)
				.setPositiveButton(R.string.ok,
						new DialogInterface.OnClickListener() {
							public void onClick(final DialogInterface dialog,
									final int id) {
								startActivity(new Intent(
										android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
							}
						})
				.setNegativeButton(R.string.no,
						new DialogInterface.OnClickListener() {
							public void onClick(final DialogInterface dialog,
									final int id) {
								dialog.cancel();
							}
						});
		final AlertDialog alert = builder.create();
		alert.show();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public class MyLocationListener implements LocationListener {

		@Override
		public void onLocationChanged(Location location) {
			if (isBetterLocation(location, currentLocation)) {
				currentLocation = location;
			}

		}

		@Override
		public void onProviderDisabled(String provider) {

		}

		@Override
		public void onProviderEnabled(String provider) {

		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {

		}

	}

	private static final int TWO_MINUTES = 1000 * 60 * 2;

	/**
	 * Determines whether one Location reading is better than the current
	 * Location fix
	 * 
	 * @param location
	 *            The new Location that you want to evaluate
	 * @param currentBestLocation
	 *            The current Location fix, to which you want to compare the new
	 *            one
	 */
	protected boolean isBetterLocation(Location location,
			Location currentBestLocation) {
		if (currentBestLocation == null) {
			// A new location is always better than no location
			return true;
		}

		// Check whether the new location fix is newer or older
		long timeDelta = location.getTime() - currentBestLocation.getTime();
		boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
		boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
		boolean isNewer = timeDelta > 0;

		// If it's been more than two minutes since the current location, use
		// the new location
		// because the user has likely moved
		if (isSignificantlyNewer) {
			return true;
			// If the new location is more than two minutes older, it must be
			// worse
		} else if (isSignificantlyOlder) {
			return false;
		}

		// Check whether the new location fix is more or less accurate
		int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation
				.getAccuracy());
		boolean isLessAccurate = accuracyDelta > 0;
		boolean isMoreAccurate = accuracyDelta < 0;
		boolean isSignificantlyLessAccurate = accuracyDelta > 200;

		// Check if the old and new location are from the same provider
		boolean isFromSameProvider = isSameProvider(location.getProvider(),
				currentBestLocation.getProvider());

		// Determine location quality using a combination of timeliness and
		// accuracy
		if (isMoreAccurate) {
			return true;
		} else if (isNewer && !isLessAccurate) {
			return true;
		} else if (isNewer && !isSignificantlyLessAccurate
				&& isFromSameProvider) {
			return true;
		}
		return false;
	}

	/** Checks whether two providers are the same */
	private boolean isSameProvider(String provider1, String provider2) {
		if (provider1 == null) {
			return provider2 == null;
		}
		return provider1.equals(provider2);
	}
}
