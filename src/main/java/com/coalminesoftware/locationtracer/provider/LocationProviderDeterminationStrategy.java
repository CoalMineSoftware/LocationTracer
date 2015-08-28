package com.coalminesoftware.locationtracer.provider;

import android.location.LocationManager;

public interface LocationProviderDeterminationStrategy {
	String determineLocationProvider(LocationManager locationManager);
}
