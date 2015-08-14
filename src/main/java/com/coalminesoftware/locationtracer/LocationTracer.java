package com.coalminesoftware.locationtracer;

import java.util.List;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.SystemClock;

import com.coalminesoftware.locationtracer.caching.LocationStore;
import com.coalminesoftware.locationtracer.reporting.LocationReporter;
import com.coalminesoftware.locationtracer.transformation.LocationTransformer;
import com.coalminesoftware.locationtracer.transformation.PassthroughLocationTransformer;

public class LocationTracer<StorageLocation> {
	private static final String	REPORT_INTERVAL_DURATION_EXTRA_KEY = "reportIntervalDuration";
	private static final String	WAKE_FOR_REPORT_EXTRA_KEY = "wakeForReport";

	private Context context;

	private LocationListener locationListener;

	private LocationStore<StorageLocation> locationStore;
	private LocationReporter<StorageLocation> locationReporter;

	private PendingIntent reportingAlarmPendingIntent;
	private LocationReportingBroadcastReceiver reportingBroadcastReceiver;

	private LocationTracer(Context context, LocationTransformer<StorageLocation> locationTransformer,
			LocationStore<StorageLocation> locationStore, LocationReporter<StorageLocation> locationReporter) {
		this.context = context;
		this.locationStore = locationStore;
		this.locationReporter = locationReporter;

		locationListener = new CachingLocationListener<StorageLocation>(locationTransformer, locationStore);
		reportingBroadcastReceiver = new LocationReportingBroadcastReceiver();
	}

	public static LocationTracer<Location> newInstance(Context context, LocationStore<Location> locationStore,
			LocationReporter<Location> locationReporter) {
		return newInstance(context, locationStore, PassthroughLocationTransformer.INSTANCE, locationReporter);
	}

	public static <StorageLocation> LocationTracer<StorageLocation> newInstance(Context context,
			LocationStore<StorageLocation> locationStore, LocationTransformer<StorageLocation> locationTransformer,
			LocationReporter<StorageLocation> locationReporter) {
		return new LocationTracer<StorageLocation>(context, locationTransformer, locationStore, locationReporter);
	}

	public synchronized void startListeningActively(long locationUpdateIntervalDuration) {
		// TODO Check whether the tracer is already started

		String providerName = determineBestActiveLocationProviderName(getLocationManager());
		getLocationManager().requestLocationUpdates(providerName, locationUpdateIntervalDuration, 0, locationListener);
	}

	public synchronized void startListeningPassively(Integer locationRequestInterval, LocationStore<StorageLocation> locationCache) {
		throw new UnsupportedOperationException("Not yet implemented.");
	}

	public synchronized void stopListening() {
		getLocationManager().removeUpdates(locationListener);
	}

	public void startReporting(long reportIntervalDuration, boolean wakeForReport) {
		if(reportingAlarmPendingIntent != null) {
			throw new RuntimeException("Cannot start reporting when reporting is already in progress.");
		}

		registerReportingAlarmReceiver();
		scheduleLocationReport(reportIntervalDuration, wakeForReport);
	}

	public void stopReporting(boolean reportUnreportedLocations) {
		cancelReportingAlarm();
		unregisterReportingAlarmReceiver();

		if(reportUnreportedLocations) {
			reportStoredLocations();
		}
	}

	private void reportStoredLocations() {
		if(locationStore.getLocationCount() > 0) {
			List<StorageLocation> locations = locationStore.getLocations();
			List<StorageLocation> reportedLocations = locationReporter.reportLocations(locations);
			locationStore.removeLocations(reportedLocations);
		}
	}

	private void registerReportingAlarmReceiver() {
		IntentFilter filter = new IntentFilter(LocationReportingBroadcastReceiver.REPORT_LOCATIONS_INTENT_ACTION);
		context.registerReceiver(reportingBroadcastReceiver, filter);
	}

	private void unregisterReportingAlarmReceiver() {
		context.unregisterReceiver(reportingBroadcastReceiver);
	}

	private synchronized void scheduleLocationReport(long reportIntervalDuration, boolean wakeForReport) {
		int alarmType = wakeForReport? AlarmManager.ELAPSED_REALTIME_WAKEUP : AlarmManager.ELAPSED_REALTIME;
		getAlarmManager().set(alarmType,
				SystemClock.elapsedRealtime() + reportIntervalDuration,
				getOrCreateReportingAlarmPendingIntent(reportIntervalDuration, wakeForReport));
	}

	private void cancelReportingAlarm() {
		getAlarmManager().cancel(reportingAlarmPendingIntent);
	}

	private PendingIntent getOrCreateReportingAlarmPendingIntent(long reportIntervalDuration, boolean wakeForReport) {
		if(reportingAlarmPendingIntent == null) {
			Intent intent = new Intent(LocationReportingBroadcastReceiver.REPORT_LOCATIONS_INTENT_ACTION);
			intent.putExtra(REPORT_INTERVAL_DURATION_EXTRA_KEY, reportIntervalDuration);
			intent.putExtra(WAKE_FOR_REPORT_EXTRA_KEY, wakeForReport);

			reportingAlarmPendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
		}
		
		return reportingAlarmPendingIntent;
	}

	private String determineBestActiveLocationProviderName(LocationManager locationManager) {
		return LocationManager.GPS_PROVIDER;
	}

	private LocationManager getLocationManager() {
		return (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
	}

	private AlarmManager getAlarmManager() {
		return (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
	}

	private class LocationReportingBroadcastReceiver extends BroadcastReceiver {
		protected static final String	REPORT_LOCATIONS_INTENT_ACTION = "com.coalminesoftware.locationtracer.REPORT_LOCATIONS";

		@Override
		public void onReceive(Context context, Intent intent) {
			long reportIntervalDuration = intent.getExtras().getLong(REPORT_INTERVAL_DURATION_EXTRA_KEY);
			boolean wakeForReport = intent.getExtras().getBoolean(WAKE_FOR_REPORT_EXTRA_KEY);

			reportStoredLocations();
			scheduleLocationReport(reportIntervalDuration, wakeForReport);
		}
	}
}


