package com.coalminesoftware.locationtracer.storage;

import java.util.Collection;
import java.util.List;

import android.os.SystemClock;

/**
 * Used to store observed locations prior to being reported at a semi-regular interval.
 *
 * @param <StorageLocation>
 */
public interface LocationStore<StorageLocation> {
	void offerLocation(StorageLocation location);
	int getLocationCount();
	List<StorageLocation> getLocations();
	void removeLocations(Collection<StorageLocation> locations);

	/**
	 * @return The "elapsed realtime" (see {@link SystemClock#elapsedRealtime()}) when the last location was accepted by
	 * the store, or null if no locations have been accepted.  The value returned by this method should not be affected
	 * by calls to {@link #removeLocations(Collection)} - it should continue to return the time that the last location
	 * was accepted, regardless of whether that location has been removed from the store.
	 */
	Long getLastLocationAcceptanceTime();
}
