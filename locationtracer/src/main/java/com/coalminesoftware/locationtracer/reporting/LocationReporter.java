package com.coalminesoftware.locationtracer.reporting;

import java.util.Collection;
import java.util.List;

public interface LocationReporter<StorageLocation> {
	/**
	 * Attempt to report the given locations.
	 * 
	 * @param locations Locations to report.
	 * @return The locations that were successfully reported.
	 */
	void reportLocations(List<StorageLocation> locations, ReportCompletionHandler<StorageLocation> reportCompletionNotifier);

	/**
	 * Used by {@link LocationReporter} implementers to notify the library that the locations
	 * provided to {@link LocationReporter#reportLocations(List, ReportCompletionHandler)} were
	 * successfully reported.
	 */
	public interface ReportCompletionHandler<StorageLocation> {
		void onLocationReportComplete(Collection<StorageLocation> reportedLocations);
	}
}
