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
import android.os.Bundle;
import android.os.SystemClock;

import com.coalminesoftware.locationtracer.cache.LocationStore;
import com.coalminesoftware.locationtracer.reporting.LocationReporter;
import com.coalminesoftware.locationtracer.transformation.LocationTransformer;
import com.coalminesoftware.locationtracer.transformation.PassthroughLocationTransformer;

public class LocationTracer<StorageLocation> {
	private static final String	REPORT_LOCATIONS_INTENT_ACTION	= "com.coalminesoftware.locationtracer.REPORT_LOCATIONS";

	private Context context;

	private LocationListener locationListener = new CachingLocationListener();
	private LocationTransformer<StorageLocation> locationTransformer;
	private LocationStore<StorageLocation> locationStore;
	private LocationReporter<StorageLocation> locationReporter;

	private PendingIntent reportingAlarmPendingIntent;
	private LocationReportingBroadcastReceiver locationReportingBroadcastReceiver;
	private long reportIntervalDuration;
	private boolean wakeForReport;

	private LocationTracer(Context context, LocationTransformer<StorageLocation> locationTransformer,
			LocationStore<StorageLocation> locationStore, LocationReporter<StorageLocation> locationReporter) {
		this.context = context;
		this.locationTransformer = locationTransformer;
		this.locationStore = locationStore;
		this.locationReporter = locationReporter;

		locationReportingBroadcastReceiver = new LocationReportingBroadcastReceiver();
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

	public synchronized void startListeningActively(long locationUpdateIntervalDuration, long reportIntervalDuration, boolean wakeForReport) {

		// TODO Check whether the tracer is already started

		String providerName = determineBestActiveLocationProviderName(getLocationManager());
		getLocationManager().requestLocationUpdates(providerName, locationUpdateIntervalDuration, 0, locationListener);

		registerLocationReportingAlarmReceiver();
		this.reportIntervalDuration = reportIntervalDuration;
		this.wakeForReport = wakeForReport;
		scheduleLocationReport();
	}

	public synchronized void startListeningPassively(Integer locationRequestInterval, LocationStore<StorageLocation> locationCache) {
		throw new UnsupportedOperationException("Not yet implemented.");
	}

	public synchronized void stopListening(boolean reportUnreportedLocations) {
		getLocationManager().removeUpdates(locationListener);

		cancelReportingAlarm();
		unregisterLocationReportingAlarmReceiver();

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

	private void registerLocationReportingAlarmReceiver() {
		IntentFilter filter = new IntentFilter(REPORT_LOCATIONS_INTENT_ACTION);
		context.registerReceiver(locationReportingBroadcastReceiver, filter);
	}

	private void unregisterLocationReportingAlarmReceiver() {
		context.unregisterReceiver(locationReportingBroadcastReceiver);
	}

	private void scheduleLocationReport() {
		int alarmType = wakeForReport? AlarmManager.ELAPSED_REALTIME_WAKEUP : AlarmManager.ELAPSED_REALTIME;
		getAlarmManager().set(alarmType,
				SystemClock.elapsedRealtime() + reportIntervalDuration,
				getOrCreateReportingAlarmPendingIntent());
	}

	private void cancelReportingAlarm() {
		getAlarmManager().cancel(getOrCreateReportingAlarmPendingIntent());
	}

	private PendingIntent getOrCreateReportingAlarmPendingIntent() {
		if(reportingAlarmPendingIntent == null) {
			Intent intent = new Intent(REPORT_LOCATIONS_INTENT_ACTION);
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

	private class CachingLocationListener implements LocationListener {
		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) { }

		@Override
		public void onProviderEnabled(String provider) { }

		@Override
		public void onProviderDisabled(String provider) { }

		@Override
		public void onLocationChanged(Location location) {
			locationStore.offerLocation(locationTransformer.transformLocation(location));
		}
	};

	private class LocationReportingBroadcastReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			scheduleLocationReport(); // TODO Use the value passed in
			reportStoredLocations();
		}
	}
}


