package com.coalminesoftware.locationtracer.test;

import java.util.Collection;
import java.util.List;

import android.app.Activity;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.coalminesoftware.locationtracer.LocationTracer;
import com.coalminesoftware.locationtracer.reporting.LocationReporter;
import com.coalminesoftware.locationtracer.storage.InMemoryLocationStore;
import com.coalminesoftware.locationtracer.storage.LocationStore;

public class MainActivity extends Activity {
	private static final String TAG = "MainActivity";
	private LocationTracer<Location> tracer;
	private LocationStore<Location> store;
	private LocationReporter<Location> reporter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		store = new InMemoryLocationStore<Location>(10) {
			@Override
			public void offerLocation(Location location) {
				super.offerLocation(location);
				Log.i(TAG, "Offered "+location);
			}

			@Override
			public List<Location> getLocations() {
				List<Location> locations = super.getLocations();
				Log.i(TAG, "Getting "+locations.size()+" locations.");
				return locations;
			}

			@Override
			public void removeLocations(Collection<Location> locations) {
				Log.i(TAG, "Removing "+locations.size()+" locations");
				super.removeLocations(locations);
			}

			@Override
			protected void onLocationPurged(Location removedLocation) {
				Log.i(TAG, "Excess location purged: " + removedLocation);
			}
		};

		reporter = new LocationReporter<Location>() {
			@Override
			public void reportLocations(List<Location> locations, ReportCompletionHandler<Location> reportCompletionNotifier) {
				Log.i(TAG, "Reporting " + locations.size() + " locations.");

				reportCompletionNotifier.onLocationReportComplete(locations);
			}
		};

		tracer = LocationTracer.newInstance(this, store, reporter);
	}

	@Override
	protected void onResume() {
		super.onResume();
		tracer.startReporting(15000, false);
//		tracer.startListeningActively();
//		tracer.startListeningActively(2000, 0.1f);
//		tracer.startListeningPassively(false);
		tracer.startListeningPassively(2000, 0.0f, 5000, false);
	}

	@Override
	protected void onPause() {
//		tracer.stopListening();
		tracer.stopReporting(true);
		super.onPause();
	}
}
