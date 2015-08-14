package com.coalminesoftware.locationtracer;

import com.coalminesoftware.locationtracer.caching.LocationStore;
import com.coalminesoftware.locationtracer.transformation.LocationTransformer;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

class CachingLocationListener<StorageLocation> implements LocationListener {
	private LocationTransformer<StorageLocation> locationTransformer;
	private LocationStore<StorageLocation> locationStore;

	public CachingLocationListener(LocationTransformer<StorageLocation> locationTransformer, LocationStore<StorageLocation> locationStore) {
		this.locationTransformer = locationTransformer;
		this.locationStore = locationStore;
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) { }

	@Override
	public void onProviderEnabled(String provider) { }

	@Override
	public void onProviderDisabled(String provider) { }

	@Override
	public void onLocationChanged(Location location) {
		locationStore.offerLocation(locationTransformer.transformLocation(location));
	}
}


