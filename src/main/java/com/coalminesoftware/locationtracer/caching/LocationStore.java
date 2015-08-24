package com.coalminesoftware.locationtracer.caching;

import java.util.Collection;
import java.util.List;

/**
 * Used to store observed locations in between uploads.
 *
 * @param <StorageLocation>
 */
public interface LocationStore<StorageLocation> {
	void offerLocation(StorageLocation location);
	int getLocationCount();
	List<StorageLocation> getLocations();
	void removeLocations(Collection<StorageLocation> locations);
	Long getLastLocationOfferElapsedRealtime();
}
