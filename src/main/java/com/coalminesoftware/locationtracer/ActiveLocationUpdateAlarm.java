package com.coalminesoftware.locationtracer;

import android.content.Context;
import android.location.LocationManager;
import android.os.Looper;

import com.coalminesoftware.locationtracer.alarm.IrregularRecurringAlarm;
import com.coalminesoftware.locationtracer.caching.LocationStore;
import com.coalminesoftware.locationtracer.listener.DefaultLocationListener;

public class ActiveLocationUpdateAlarm extends IrregularRecurringAlarm {
	private long locationUpdateInterval;
	private LocationStore<?> locationStore;

	public ActiveLocationUpdateAlarm(Context context, boolean wakeForAlarm, long locationUpdateInterval, LocationStore<?> locationStore) {
		super(context, wakeForAlarm);

		this.locationUpdateInterval = locationUpdateInterval;
		this.locationStore = locationStore;
	}

	@Override
	protected long determineNextAlarmDelay(long alarmElapsedRealtime) {
		Long timeElapsed = determineRealtimeElapsedSinceLastLocation(alarmElapsedRealtime);
		return timeElapsed == null || hasAlarmExpired(timeElapsed)?
				locationUpdateInterval :
				determineRemainingTime(timeElapsed);
	}

	@Override
	public void handleAlarm(long alarmRealtime) {
		Long timeElapsed = determineRealtimeElapsedSinceLastLocation(alarmRealtime);
		if(timeElapsed == null || hasAlarmExpired(timeElapsed)) {
			// Since a passive listener is already registered, a no-op location listener is used to avoid offering duplicate updates to the cache.
			getLocationManager().requestSingleUpdate(
					LocationManager.NETWORK_PROVIDER, // TODO Switch this back to GPS.
					DefaultLocationListener.INSTANCE,
					Looper.myLooper());
		}
	}

	private Long determineRealtimeElapsedSinceLastLocation(long alarmElapsedRealtime) {
		Long lastLocationObservationTimestamp = locationStore.getLastLocationOfferElapsedRealtime();
		return lastLocationObservationTimestamp == null?
				null :
				alarmElapsedRealtime - lastLocationObservationTimestamp;
	}

	private long determineRemainingTime(long timeElapsed) {
		return locationUpdateInterval - timeElapsed;
	}

	private boolean hasAlarmExpired(long timeElapsed) {
		return timeElapsed < locationUpdateInterval;
	}

	private LocationManager getLocationManager() {
		return (LocationManager)getContext().getSystemService(Context.LOCATION_SERVICE);
	}
}


