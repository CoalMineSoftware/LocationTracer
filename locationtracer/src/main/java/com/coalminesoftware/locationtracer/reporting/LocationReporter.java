package com.coalminesoftware.locationtracer.reporting;

import java.util.Collection;
import java.util.List;

public interface LocationReporter<StorageLocation> {
	/**
	 * Attempt to report the given locations. Implementers must call
	 * {@link ReportCompletionHandler#onLocationReportComplete(Collection)} on the provided
	 * {@link ReportCompletionHandler} once the given locations have been reported.
	 * 
	 * @param locations Locations to report.
	 */
	void reportLocations(List<StorageLocation> locations, ReportCompletionHandler<StorageLocation> reportCompletionNotifier);

	/**
	 * Used by {@link LocationReporter} implementers to notify the library that the locations
	 * provided to {@link LocationReporter#reportLocations(List, ReportCompletionHandler)} were
	 * successfully reported.
	 */
	interface ReportCompletionHandler<StorageLocation> {
		void onLocationReportComplete(Collection<StorageLocation> reportedLocations);
	}
}
