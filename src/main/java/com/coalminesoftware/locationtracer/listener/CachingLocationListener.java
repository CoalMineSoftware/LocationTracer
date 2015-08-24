package com.coalminesoftware.locationtracer.listener;

import android.location.Location;

import com.coalminesoftware.locationtracer.caching.LocationStore;
import com.coalminesoftware.locationtracer.transformation.LocationTransformer;

public class CachingLocationListener<StorageLocation> extends DefaultLocationListener {
	private LocationTransformer<StorageLocation> locationTransformer;
	private LocationStore<StorageLocation> locationStore;

	public CachingLocationListener(LocationTransformer<StorageLocation> locationTransformer, LocationStore<StorageLocation> locationStore) {
		this.locationTransformer = locationTransformer;
		this.locationStore = locationStore;
	}

	@Override
	public void onLocationChanged(Location location) {
		locationStore.offerLocation(locationTransformer.transformLocation(location));
	}
}


