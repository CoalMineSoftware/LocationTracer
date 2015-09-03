package com.coalminesoftware.locationtracer.caching;

import android.os.SystemClock;

/**
 * Convenience implementation of {@link LocationStore} that implements {@link #getLastLocationAcceptanceTime()} by
 * returning the a stored time that subclasses update by calling {@link #updateLastAcceptedLocationTime()} when their
 * {@link #offerLocation(Object)} implementation accepts a location.
 *
 * @param <StorageLocation>
 */
public abstract class BaseLocationStore<StorageLocation> implements LocationStore<StorageLocation> {
	private Long lastAcceptedLocationTime;

	/**
	 * Updates the time returned by {@link #getLastLocationAcceptanceTime()} to the current value of
	 * {@link SystemClock#elapsedRealtime()}.
	 */
	protected void updateLastAcceptedLocationTime() {
		lastAcceptedLocationTime = SystemClock.elapsedRealtime();
	}

	@Override
	public Long getLastLocationAcceptanceTime() {
		return lastAcceptedLocationTime;
	}
}
