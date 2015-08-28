package com.coalminesoftware.locationtracer.provider;

import android.location.LocationManager;

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
