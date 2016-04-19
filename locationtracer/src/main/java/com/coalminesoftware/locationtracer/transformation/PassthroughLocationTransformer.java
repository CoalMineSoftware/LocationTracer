package com.coalminesoftware.locationtracer.transformation;

import android.location.Location;

/** A {@link LocationTransformer} that performs no transformation on the {@link Location}s it is given. */
public class PassthroughLocationTransformer implements LocationTransformer<Location> {
	public static final PassthroughLocationTransformer INSTANCE = new PassthroughLocationTransformer();

	private PassthroughLocationTransformer() { }

	@Override
	public Location transformLocation(Location location) {
		return location;
	}
}
