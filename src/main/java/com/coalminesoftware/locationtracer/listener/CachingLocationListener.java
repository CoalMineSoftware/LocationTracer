package com.coalminesoftware.locationtracer.listener;

import android.location.Location;
import android.os.SystemClock;

import com.coalminesoftware.locationtracer.caching.LocationStore;
import com.coalminesoftware.locationtracer.transformation.LocationTransformer;

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


