package com.coalminesoftware.locationtracer;

import android.content.Context;
import android.location.LocationManager;
import android.os.Looper;

import com.coalminesoftware.locationtracer.alarm.IrregularRecurringAlarm;
import com.coalminesoftware.locationtracer.caching.LocationStore;
import com.coalminesoftware.locationtracer.listener.DefaultLocationListener;
import com.coalminesoftware.locationtracer.provider.LocationProviderDeterminationStrategy;

public class ActiveLocationUpdateAlarm extends IrregularRecurringAlarm {
	private long locationUpdateIntervalDuration;
	private LocationStore<?> locationStore;
	private LocationProviderDeterminationStrategy locationProviderDeterminationStrategy;

	public ActiveLocationUpdateAlarm(Context context,
			boolean wakeForAlarm,
			long locationUpdateIntervalDuration,
			LocationStore<?> locationStore,
			LocationProviderDeterminationStrategy locationProviderDeterminationStrategy) {
		super(context, wakeForAlarm);

		this.locationUpdateIntervalDuration = locationUpdateIntervalDuration;
		this.locationStore = locationStore;
		this.locationProviderDeterminationStrategy = locationProviderDeterminationStrategy;
	}

	@Override
	protected long determineNextAlarmDelay(long alarmTime) {
		Long timeElapsed = determineTimeElapsedSinceLastLocationUpdate(alarmTime);
		return timeElapsed == null || hasAlarmExpired(timeElapsed)?
				locationUpdateIntervalDuration :
				determineRemainingTime(timeElapsed);
	}

	@Override
	public void handleAlarm(long alarmTime) {
		Long timeElapsed = determineTimeElapsedSinceLastLocationUpdate(alarmTime);
		if(timeElapsed == null || hasAlarmExpired(timeElapsed)) {
			// Since a passive listener is already listening for the location update that this request hopes to cause,
			// a no-op location listener is used to avoid offering duplicate updates to the cache.
			getLocationManager().requestSingleUpdate(
					locationProviderDeterminationStrategy.determineLocationProvider(getLocationManager()),
					DefaultLocationListener.INSTANCE,
					Looper.myLooper());
		}
	}

	private Long determineTimeElapsedSinceLastLocationUpdate(long alarmElapsedRealtime) {
		Long lastLocationObservationTimestamp = locationStore.getLastLocationOfferElapsedRealtime();
		return lastLocationObservationTimestamp == null?
				null :
				alarmElapsedRealtime - lastLocationObservationTimestamp;
	}

	private long determineRemainingTime(long timeElapsed) {
		return locationUpdateIntervalDuration - timeElapsed;
	}

	private boolean hasAlarmExpired(long timeElapsed) {
		return timeElapsed >= locationUpdateIntervalDuration;
	}

	private LocationManager getLocationManager() {
		return (LocationManager)getContext().getSystemService(Context.LOCATION_SERVICE);
	}
}


