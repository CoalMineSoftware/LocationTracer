package com.coalminesoftware.locationtracer.provider;

import android.location.LocationManager;
import android.location.LocationProvider;

/**
 * Defines a strategy for determining the name of the {@link LocationProvider} that locations will be requested from.
 */
public interface LocationProviderDeterminationStrategy {
	/**
	 * @param locationManager The system's LocationManager.
	 * @return The name of the LocationProvider to be used.
	 */
	String determineLocationProvider(LocationManager locationManager);
}
