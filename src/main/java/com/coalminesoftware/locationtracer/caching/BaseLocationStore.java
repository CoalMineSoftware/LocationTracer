package com.coalminesoftware.locationtracer.caching;

import android.os.SystemClock;

public abstract class BaseLocationStore<StorageLocation> implements LocationStore<StorageLocation> {
	private Long lastAcceptedLocationTime;

	protected void updateLastAcceptedLocationTime() {
		lastAcceptedLocationTime = SystemClock.elapsedRealtime();
	}

	@Override
	public Long getLastLocationAcceptanceTime() {
		return lastAcceptedLocationTime;
	}
}
