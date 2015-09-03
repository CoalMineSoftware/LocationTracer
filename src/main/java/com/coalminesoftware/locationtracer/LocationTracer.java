package com.coalminesoftware.locationtracer;

import java.util.List;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Looper;

import com.coalminesoftware.locationtracer.alarm.IrregularRecurringAlarm;
import com.coalminesoftware.locationtracer.alarm.RecurringAlarm;
import com.coalminesoftware.locationtracer.caching.LocationStore;
import com.coalminesoftware.locationtracer.listener.CachingLocationListener;
import com.coalminesoftware.locationtracer.listener.DefaultLocationListener;
import com.coalminesoftware.locationtracer.provider.LocationProviderDeterminationStrategy;
import com.coalminesoftware.locationtracer.provider.SimpleLocationProviderDeterminationStrategy;
import com.coalminesoftware.locationtracer.reporting.LocationReporter;
import com.coalminesoftware.locationtracer.transformation.LocationTransformer;
import com.coalminesoftware.locationtracer.transformation.PassthroughLocationTransformer;

public class LocationTracer<StorageLocation> {
	private Context context;

	private LocationStore<StorageLocation> locationStore;
	private LocationReporter<StorageLocation> locationReporter;

	private LocationListener locationListener;
	private ListeningSession locationListeningSession;

	private LocationProviderDeterminationStrategy activeListeningLocationProviderStrategy = new SimpleLocationProviderDeterminationStrategy(LocationManager.GPS_PROVIDER);
	private LocationProviderDeterminationStrategy passiveListeningLocationProviderStrategy = new SimpleLocationProviderDeterminationStrategy(LocationManager.GPS_PROVIDER);

	private ReportingSession reportingSession;

	private float minimumLocationUpdateDistance = 0f; // TODO The minimum distance and time used for location updates should be configurable but with reasonable defaults.

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

		String providerName = activeListeningLocationProviderStrategy.determineLocationProvider(getLocationManager());
		getLocationManager().requestLocationUpdates(providerName,
				locationUpdateIntervalDuration,
				minimumLocationUpdateDistance,
				locationListener);

		locationListeningSession = new ListeningSession();
	}

	public synchronized void startListeningPassively(Long activeLocationRequestInterval, boolean wakeForActiveLocationRequests) {
		verifyListeningNotInProgress();

		getLocationManager().requestLocationUpdates(LocationManager.PASSIVE_PROVIDER,
				0,
				0,
				locationListener);

		IrregularRecurringAlarm alarm = activeLocationRequestInterval == null?
				null :
				startActiveLocationUpdateAlarm(activeLocationRequestInterval, wakeForActiveLocationRequests);

		locationListeningSession = new ListeningSession(alarm);
	}

	private void verifyListeningNotInProgress() {
		if(locationListeningSession != null) {
			throw new IllegalStateException("Cannot start listening when listening is already in progress.");
		}
	}

	private IrregularRecurringAlarm startActiveLocationUpdateAlarm(long activeLocationRequestInterval, boolean wakeForActiveLocationRequests) {
		IrregularRecurringAlarm alarm = new ActiveLocationUpdateAlarm(wakeForActiveLocationRequests, activeLocationRequestInterval);

		alarm.startRecurringAlarm();

		return alarm;
	}

	public synchronized void stopListening() {
		verifyListeningInProgress();

		getLocationManager().removeUpdates(locationListener);

		if(locationListeningSession.getActiveLocationUpdateAlarm() != null) {
			locationListeningSession.getActiveLocationUpdateAlarm().stopRecurringAlarm();
		}

		locationListeningSession = null;
	}

	private void verifyListeningInProgress() {
		if(locationListeningSession == null) {
			throw new IllegalStateException("Cannot stop listening when listening is not in progress.");
		}
	}

	public void startReporting(long reportIntervalDuration, boolean wakeForReport) {
		if(reportingSession != null) {
			throw new IllegalStateException("Cannot start reporting when reporting is already in progress.");
		}

		RecurringAlarm reportingAlarm = new RecurringAlarm(context, reportIntervalDuration, wakeForReport) {
			@Override
			public void handleAlarm(long alarmElapsedRealtime) {
				reportStoredLocations();
			}
		};
		reportingAlarm.startRecurringAlarm();

		reportingSession = new ReportingSession(reportingAlarm);
	}

	public void stopReporting(boolean reportUnreportedLocations) {
		reportingSession.getReportingAlarm().stopRecurringAlarm();
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

	private LocationManager getLocationManager() {
		return (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
	}

	private static class ReportingSession {
		private RecurringAlarm reportingAlarm;

		public ReportingSession(RecurringAlarm reportingAlarm) {
			this.reportingAlarm = reportingAlarm;
		}

		public RecurringAlarm getReportingAlarm() {
			return reportingAlarm;
		}
	}

	private static class ListeningSession {
		private IrregularRecurringAlarm activeLocationUpdateAlarm;

		public ListeningSession() { }

		public ListeningSession(IrregularRecurringAlarm activeLocationUpdateAlarm) {
			this.activeLocationUpdateAlarm = activeLocationUpdateAlarm;
		}

		public IrregularRecurringAlarm getActiveLocationUpdateAlarm() {
			return activeLocationUpdateAlarm;
		}
	}

	public class ActiveLocationUpdateAlarm extends IrregularRecurringAlarm {
		private long locationUpdateIntervalDuration;

		public ActiveLocationUpdateAlarm(boolean wakeForAlarm, long locationUpdateIntervalDuration) {
			super(context, wakeForAlarm);

			this.locationUpdateIntervalDuration = locationUpdateIntervalDuration;
		}

		@Override
		protected long determineNextAlarmDelay(long alarmTime) {
			Long timeElapsed = determineTimeElapsedSinceLastLocationAcceptance(alarmTime);
			return timeElapsed == null || hasAlarmExpired(timeElapsed)?
					locationUpdateIntervalDuration :
					determineRemainingTime(timeElapsed);
		}

		@Override
		public void handleAlarm(long alarmTime) {
			Long timeElapsed = determineTimeElapsedSinceLastLocationAcceptance(alarmTime);
			if(timeElapsed == null || hasAlarmExpired(timeElapsed)) {
				// Since a passive listener should already be listening for the location update that this request hopes
				// to cause, a no-op location listener is used to avoid offering duplicate updates to the cache.
				getLocationManager().requestSingleUpdate(
						passiveListeningLocationProviderStrategy.determineLocationProvider(getLocationManager()),
						DefaultLocationListener.INSTANCE,
						Looper.myLooper());
			}
		}

		private Long determineTimeElapsedSinceLastLocationAcceptance(long alarmTime) {
			Long lastLocationAcceptanceTime = locationStore.getLastLocationAcceptanceTime();
			return lastLocationAcceptanceTime == null?
					null :
					alarmTime - lastLocationAcceptanceTime;
		}

		private long determineRemainingTime(long timeElapsed) {
			return locationUpdateIntervalDuration - timeElapsed;
		}

		private boolean hasAlarmExpired(long timeElapsed) {
			return timeElapsed >= locationUpdateIntervalDuration;
		}
	}
}


