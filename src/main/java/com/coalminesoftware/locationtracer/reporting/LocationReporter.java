package com.coalminesoftware.locationtracer.reporting;

import java.util.List;

public interface LocationReporter<StorageLocation> {
	/**
	 * Attempt to report the given locations.
	 * 
	 * @param locations Locations to report.
	 * @return The locations that were successfully reported.
	 */
	List<StorageLocation> reportLocations(List<StorageLocation> locations);
}
