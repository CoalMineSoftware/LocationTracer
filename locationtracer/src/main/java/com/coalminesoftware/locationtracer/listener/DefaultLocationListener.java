package com.coalminesoftware.locationtracer.listener;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

/** Location listener that takes no action when observing a location. */
public class DefaultLocationListener implements LocationListener {
	public static final DefaultLocationListener INSTANCE = new DefaultLocationListener();

	@Override
	public void onLocationChanged(Location location) { }

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) { }

	@Override
	public void onProviderEnabled(String provider) { }

	@Override
	public void onProviderDisabled(String provider) { }
}
