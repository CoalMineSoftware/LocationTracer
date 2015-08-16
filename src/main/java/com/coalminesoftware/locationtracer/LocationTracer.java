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

	private LocationStore<StorageLocation> locationStore;
	private LocationReporter<StorageLocation> locationReporter;

	private LocationListener locationListener;

	private ReportingSession reportingSession;

	private LocationTracer(Context context, LocationTransformer<StorageLocation> locationTransformer,
			LocationStore<StorageLocation> locationStore, LocationReporter<StorageLocation> locationReporter) {
		this.context = context.getApplicationContext();
		this.locationStore = locationStore;
		this.locationReporter = locationReporter;

		locationListener = new CachingLocationListener<StorageLocation>(locationTransformer, locationStore);
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
		if(reportingSession != null) {
			throw new IllegalStateException("Cannot start reporting when reporting is already in progress.");
		}

		BroadcastReceiver reportingAlarmBroadcastReceiver = registerReportingAlarmReceiver();
		PendingIntent reportingAlarmPendingIntent = scheduleLocationReport(reportIntervalDuration, wakeForReport);

		reportingSession = new ReportingSession(reportingAlarmPendingIntent, reportingAlarmBroadcastReceiver);
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

	private BroadcastReceiver registerReportingAlarmReceiver() {
		BroadcastReceiver broadcastReceiver = new LocationReportingBroadcastReceiver();

		IntentFilter filter = new IntentFilter(LocationReportingBroadcastReceiver.REPORT_LOCATIONS_INTENT_ACTION);
		context.registerReceiver(broadcastReceiver, filter);

		return broadcastReceiver;
	}

	private void unregisterReportingAlarmReceiver() {
		context.unregisterReceiver(reportingSession.getReportingBroadcastReceiver());
	}

	private synchronized PendingIntent scheduleLocationReport(long reportIntervalDuration, boolean wakeForReport) {
		int alarmType = wakeForReport? AlarmManager.ELAPSED_REALTIME_WAKEUP : AlarmManager.ELAPSED_REALTIME;

		PendingIntent reportingAlarmPendingIntent = buildReportingAlarmPendingIntent(reportIntervalDuration, wakeForReport);

		getAlarmManager().set(alarmType,
				SystemClock.elapsedRealtime() + reportIntervalDuration,
				reportingAlarmPendingIntent);

		return reportingAlarmPendingIntent;
	}

	private void cancelReportingAlarm() {
		getAlarmManager().cancel(reportingSession.getReportingAlarmPendingIntent());
		reportingSession = null;
	}

	private PendingIntent buildReportingAlarmPendingIntent(long reportIntervalDuration, boolean wakeForReport) {
		Intent intent = new Intent(LocationReportingBroadcastReceiver.REPORT_LOCATIONS_INTENT_ACTION);
		intent.putExtra(REPORT_INTERVAL_DURATION_EXTRA_KEY, reportIntervalDuration);
		intent.putExtra(WAKE_FOR_REPORT_EXTRA_KEY, wakeForReport);

		return PendingIntent.getBroadcast(context, 0, intent, 0);
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

	private static class ReportingSession {
		private PendingIntent reportingAlarmPendingIntent;
		private BroadcastReceiver reportingBroadcastReceiver;

		public ReportingSession(PendingIntent reportingAlarmPendingIntent, BroadcastReceiver reportingBroadcastReceiver) {
			this.reportingAlarmPendingIntent = reportingAlarmPendingIntent;
			this.reportingBroadcastReceiver = reportingBroadcastReceiver;
		}

		public PendingIntent getReportingAlarmPendingIntent() {
			return reportingAlarmPendingIntent;
		}

		public BroadcastReceiver getReportingBroadcastReceiver() {
			return reportingBroadcastReceiver;
		}
	}
}


