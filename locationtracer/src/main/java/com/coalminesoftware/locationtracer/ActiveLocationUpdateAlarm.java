package com.coalminesoftware.locationtracer;

import android.content.Context;
import android.location.LocationManager;
import android.os.Looper;

import com.coalminesoftware.locationtracer.alarm.IrregularRecurringAlarm;
import com.coalminesoftware.locationtracer.listener.DefaultLocationListener;
import com.coalminesoftware.locationtracer.provider.LocationProviderDeterminationStrategy;
import com.coalminesoftware.locationtracer.storage.LocationStore;

public class ActiveLocationUpdateAlarm extends IrregularRecurringAlarm {
	private long locationUpdateIntervalDuration;
	private LocationStore<?> locationStore;
	private LocationProviderDeterminationStrategy locationProviderDeterminationStrategy;

	public ActiveLocationUpdateAlarm(Context context, LocationStore<?> locationStore, boolean wakeForAlarm, long locationUpdateIntervalDuration, 
			LocationProviderDeterminationStrategy locationProviderDeterminationStrategy) {
		super(context, wakeForAlarm);

		this.locationUpdateIntervalDuration = locationUpdateIntervalDuration;
		this.locationProviderDeterminationStrategy = locationProviderDeterminationStrategy;
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
					locationProviderDeterminationStrategy.determineLocationProvider(getLocationManager()),
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

	private LocationManager getLocationManager() {
		return (LocationManager)getContext().getSystemService(Context.LOCATION_SERVICE);
	}
}
