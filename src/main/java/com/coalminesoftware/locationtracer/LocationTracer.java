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
	private static final String REPORT_INTERVAL_DURATION_EXTRA_KEY = "reportIntervalDuration";
	private static final String WAKE_FOR_REPORT_EXTRA_KEY = "wakeForReport";
	private static int REPORTING_RECEIVER_INSTANCE_COUNT = 0;

	private Context context;

	private LocationStore<StorageLocation> locationStore;
	private LocationReporter<StorageLocation> locationReporter;

	private LocationListener locationListener;
	private boolean listening;

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
		verifyListeningNotInProgress();

		String providerName = determineBestActiveLocationProviderName(getLocationManager());
		getLocationManager().requestLocationUpdates(providerName, locationUpdateIntervalDuration, 0, locationListener);

		listening = true;
	}

	public synchronized void startListeningPassively() {
		startListeningPassively(null);
	}

	public synchronized void startListeningPassively(Integer activeLocationRequestInterval) {
		verifyListeningNotInProgress();

		getLocationManager().requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 0, 0, locationListener);

		if(activeLocationRequestInterval != null) {			
			registerActiveLocationUpdate(activeLocationRequestInterval);
		}

		listening = true;
	}

	private void registerActiveLocationUpdate(Integer activeLocationRequestInterval) {
		// TODO Do it.
	}

	private void verifyListeningNotInProgress() {
		if(listening) {
			throw new IllegalStateException("Cannot start listening when listening is already in progress.");
		}
	}

	public synchronized void stopListening() {
		getLocationManager().removeUpdates(locationListener);

		// TODO If active updates were requested when starting passive listening, cancel them here.

		listening = false;
	}

	public void startReporting(long reportIntervalDuration, boolean wakeForReport) {
		if(reportingSession != null) {
			throw new IllegalStateException("Cannot start reporting when reporting is already in progress.");
		}

		LocationReportingBroadcastReceiver reportingAlarmBroadcastReceiver = registerReportingAlarmReceiver();
		PendingIntent reportingAlarmPendingIntent = scheduleLocationReport(
				reportingAlarmBroadcastReceiver.getGeneratedReportLocationsIntentAction(),
				reportIntervalDuration,
				wakeForReport);

		reportingSession = new ReportingSession(reportingAlarmPendingIntent, reportingAlarmBroadcastReceiver);
	}

	public void stopReporting(boolean reportUnreportedLocations) {
		cancelReportingAlarm();
		unregisterReportingAlarmReceiver();

		reportingSession = null;

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

	private LocationReportingBroadcastReceiver registerReportingAlarmReceiver() {
		LocationReportingBroadcastReceiver broadcastReceiver = new LocationReportingBroadcastReceiver();

		IntentFilter filter = new IntentFilter(broadcastReceiver.getGeneratedReportLocationsIntentAction());
		context.registerReceiver(broadcastReceiver, filter);

		return broadcastReceiver;
	}

	private void unregisterReportingAlarmReceiver() {
		context.unregisterReceiver(reportingSession.getReportingBroadcastReceiver());
	}

	private synchronized PendingIntent scheduleLocationReport(String receiverAction, long reportIntervalDuration, boolean wakeForReport) {
		int alarmType = wakeForReport? AlarmManager.ELAPSED_REALTIME_WAKEUP : AlarmManager.ELAPSED_REALTIME;

		PendingIntent reportingAlarmPendingIntent = buildReportingAlarmPendingIntent(receiverAction, reportIntervalDuration, wakeForReport);

		getAlarmManager().set(alarmType,
				SystemClock.elapsedRealtime() + reportIntervalDuration,
				reportingAlarmPendingIntent);

		return reportingAlarmPendingIntent;
	}

	private void cancelReportingAlarm() {
		getAlarmManager().cancel(reportingSession.getReportingAlarmPendingIntent());
	}

	private PendingIntent buildReportingAlarmPendingIntent(String receiverAction, long reportIntervalDuration, boolean wakeForReport) {
		Intent intent = new Intent(receiverAction);
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
		private static final String BASE_REPORT_LOCATIONS_ACTION = "com.coalminesoftware.locationtracer.REPORT_LOCATIONS";

		private String reportLocationsIntentAction = generateLocationReportingAction();

		@Override
		public void onReceive(Context context, Intent intent) {
			long reportIntervalDuration = intent.getExtras().getLong(REPORT_INTERVAL_DURATION_EXTRA_KEY);
			boolean wakeForReport = intent.getExtras().getBoolean(WAKE_FOR_REPORT_EXTRA_KEY);

			reportStoredLocations();
			scheduleLocationReport(intent.getAction(), reportIntervalDuration, wakeForReport);
		}

		public String getGeneratedReportLocationsIntentAction() {
			return reportLocationsIntentAction;
		}

		private String generateLocationReportingAction() {
			return context.getPackageName() + "/" +
					BASE_REPORT_LOCATIONS_ACTION + "/" +
					REPORTING_RECEIVER_INSTANCE_COUNT++;
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


