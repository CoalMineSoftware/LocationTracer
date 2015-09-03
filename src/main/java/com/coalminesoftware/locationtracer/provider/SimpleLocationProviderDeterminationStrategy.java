package com.coalminesoftware.locationtracer.provider;

import android.location.LocationManager;

/**
 * A {@link LocationProviderDeterminationStrategy} implementation that simply returns the name provided during an
 * instance's construction.
 */
public class SimpleLocationProviderDeterminationStrategy implements LocationProviderDeterminationStrategy {
	private String providerName;

	public SimpleLocationProviderDeterminationStrategy(String providerName) {
		this.providerName = providerName;
	}

	@Override
	public String determineLocationProvider(LocationManager locationManager) {
		return providerName;
	}
}
