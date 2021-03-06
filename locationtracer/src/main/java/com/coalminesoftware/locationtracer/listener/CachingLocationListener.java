package com.coalminesoftware.locationtracer.listener;

import android.location.Location;
import android.os.SystemClock;

import com.coalminesoftware.locationtracer.storage.LocationStore;
import com.coalminesoftware.locationtracer.transformation.LocationTransformer;

/**
 * Listener that transforms observed {@link Location} updates using the provided {@link LocationTransformer} and stores
 * the resulting model object in the given {@link LocationStore}.
 *
 * @param <StorageLocation> The type that Locations will be transformed into and stored as.
 */
public class CachingLocationListener<StorageLocation> extends DefaultLocationListener {
	private LocationTransformer<StorageLocation> locationTransformer;
	private LocationStore<StorageLocation> locationStore;
	private Long lastLocationObservationTime;

	public CachingLocationListener(LocationTransformer<StorageLocation> locationTransformer, LocationStore<StorageLocation> locationStore) {
		this.locationTransformer = locationTransformer;
		this.locationStore = locationStore;
	}

	@Override
	public void onLocationChanged(Location location) {
		lastLocationObservationTime = SystemClock.elapsedRealtime();
		locationStore.offerLocation(locationTransformer.transformLocation(location));
	}

	public Long getLastLocationObservationTime() {
		return lastLocationObservationTime;
	}
}


