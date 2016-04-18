package com.coalminesoftware.locationtracer;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.List;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Looper;
import android.util.Log;

import com.coalminesoftware.locationtracer.alarm.IrregularRecurringAlarm;
import com.coalminesoftware.locationtracer.alarm.RecurringAlarm;
import com.coalminesoftware.locationtracer.listener.CachingLocationListener;
import com.coalminesoftware.locationtracer.listener.DefaultLocationListener;
import com.coalminesoftware.locationtracer.provider.LocationProviderDeterminationStrategy;
import com.coalminesoftware.locationtracer.provider.SimpleLocationProviderDeterminationStrategy;
import com.coalminesoftware.locationtracer.reporting.LocationReporter;
import com.coalminesoftware.locationtracer.reporting.LocationReporter.ReportCompletionHandler;
import com.coalminesoftware.locationtracer.storage.LocationStore;
import com.coalminesoftware.locationtracer.transformation.LocationTransformer;
import com.coalminesoftware.locationtracer.transformation.PassthroughLocationTransformer;

public class LocationTracer<StorageLocation> {
	private static final String LOGGING_TAG = "LocationTracer";
	private static final long DEFAULT_MINIMUM_LOCATION_UPDATE_INTERVAL_DURATION = 1000;
	private static final float DEFAULT_MINIMUM_LOCATION_UPDATE_DISTANCE = 0.0f;

	private Context context;

	private LocationStore<StorageLocation> locationStore;
	private LocationReporter<StorageLocation> locationReporter;

	private LocationListener locationListener;

	private ListeningSession locationListeningSession;
	private ReportingSession reportingSession;

	private LocationProviderDeterminationStrategy activeListeningLocationProviderDeterminationStrategy = new SimpleLocationProviderDeterminationStrategy(LocationManager.GPS_PROVIDER);
	private LocationProviderDeterminationStrategy passiveListeningLocationProviderDeterminationStrategy = new SimpleLocationProviderDeterminationStrategy(LocationManager.GPS_PROVIDER);

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

	/**
	 * Starts actively requesting location updates with a minimum update interval of one second and no minimum distance.
	 * 
	 * @see #startListeningActively(long, float)
	 */
	public void startListeningActively() {
		startListeningActively(
				DEFAULT_MINIMUM_LOCATION_UPDATE_INTERVAL_DURATION,
				DEFAULT_MINIMUM_LOCATION_UPDATE_DISTANCE);
	}

	/**
	 * Starts actively requesting location updates with the specified minimum update interval and distance.
	 * @param minimumLocationUpdateIntervalDuration The minimum number of milliseconds requested between location updates.
	 * @param minimumLocationUpdateDistance The minimum displacement, in meters, that needs to exist between a location and the preceding location.
	 */
	public synchronized void startListeningActively(long minimumLocationUpdateIntervalDuration,
			float minimumLocationUpdateDistance) {
		verifyListeningNotInProgress();

		String providerName = activeListeningLocationProviderDeterminationStrategy
				.determineLocationProvider(getLocationManager());

		getLocationManager().requestLocationUpdates(
				providerName,
				minimumLocationUpdateIntervalDuration,
				minimumLocationUpdateDistance,
				locationListener);

		locationListeningSession = new ListeningSession();
	}

	/**
	 * Starts passively listening for location updates that happen at the request of other code.
	 * @param wakeForActiveLocationRequests
	 */
	public void startListeningPassively(boolean wakeForActiveLocationRequests) {
		startListeningPassively(
				DEFAULT_MINIMUM_LOCATION_UPDATE_INTERVAL_DURATION,
				DEFAULT_MINIMUM_LOCATION_UPDATE_DISTANCE,
				null,
				false);
	}

	/**
	 * Starts passively listening for location updates that happen at the request of other code. If a location update is
	 * not received within a certain amount of time, a location is actively requested.
	 * 
	 * @param minimumLocationUpdateIntervalDuration The minimum number of milliseconds requested between location updates. 
	 * @param minimumLocationUpdateDistance The minimum displacement, in meters, that needs to exist between a location and the preceding location.
	 * @param activeLocationRequestInterval How long to wait since the last location update before actively requesting another. 
	 * @param wakeForActiveLocationRequests Whether to wake the device when requesting active location updates.
	 */
	public void startListeningPassively(
			long minimumLocationUpdateIntervalDuration,
			float minimumLocationUpdateDistance,
			long activeLocationRequestInterval,
			boolean wakeForActiveLocationRequests) {
		startListeningPassively(
				minimumLocationUpdateIntervalDuration,
				minimumLocationUpdateDistance,
				(Long)activeLocationRequestInterval,
				wakeForActiveLocationRequests);
	}

	private synchronized void startListeningPassively(
			long minimumLocationUpdateIntervalDuration,
			float minimumLocationUpdateDistance,
			Long activeLocationRequestInterval,
			boolean wakeForActiveLocationRequests) {
		verifyListeningNotInProgress();

		getLocationManager().requestLocationUpdates(
				LocationManager.PASSIVE_PROVIDER,
				minimumLocationUpdateIntervalDuration,
				minimumLocationUpdateDistance,
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

	public synchronized void startReporting(long reportIntervalDuration, boolean wakeForReport) {
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

	public synchronized void stopReporting(boolean reportUnreportedLocations) {
		reportingSession.getReportingAlarm().stopRecurringAlarm();
		reportingSession = null;

		if(reportUnreportedLocations) {
			reportStoredLocations();
		}
	}

	private void reportStoredLocations() {
		if(locationStore.getLocationCount() > 0) {
			List<StorageLocation> locations = locationStore.getLocations();
			locationReporter.reportLocations(locations, new LocationRemovingReportCompletionNotifier<StorageLocation>(locationStore));
		}
	}

	private LocationManager getLocationManager() {
		return (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
	}

	/**
	 * @param activeListeningLocationProviderDeterminationStrategy The strategy used to determine which
	 *         {@link LocationProvider} to request locations from when listening actively.
	 */
	public void setActiveListeningLocationProviderDeterminationStrategy(
			LocationProviderDeterminationStrategy activeListeningLocationProviderDeterminationStrategy) {
		this.activeListeningLocationProviderDeterminationStrategy = activeListeningLocationProviderDeterminationStrategy;
	}

	/**
	 * @param passiveListeningLocationProviderDeterminationStrategy The strategy used to determine which
	 *         {@link LocationProvider} to request locations from if/when requesting active location updates during
	 *         passive listening.
	 */
	public void setPassiveListeningLocationProviderDeterminationStrategy(
			LocationProviderDeterminationStrategy passiveListeningLocationProviderDeterminationStrategy) {
		this.passiveListeningLocationProviderDeterminationStrategy = passiveListeningLocationProviderDeterminationStrategy;
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
						passiveListeningLocationProviderDeterminationStrategy.determineLocationProvider(getLocationManager()),
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

	/**
	 * {@link ReportCompletionHandler} implementation that removes locations from the store once reported.
	 */
	private static class LocationRemovingReportCompletionNotifier<StorageLocation> implements ReportCompletionHandler<StorageLocation> {
		private final WeakReference<LocationStore<StorageLocation>> storeReference;
		
		public LocationRemovingReportCompletionNotifier(LocationStore<StorageLocation> locationStore) {
			storeReference = new WeakReference<LocationStore<StorageLocation>>(locationStore);
		}

		@Override
		public void onLocationReportComplete(Collection<StorageLocation> locations) {
			LocationStore<StorageLocation> store = storeReference.get();
			if(store == null) {
				Log.w(LOGGING_TAG, "Location store no longer exists. Ignoring reporting completion.");
			} else {
				store.removeLocations(locations);
			}
		}			
	};
}


