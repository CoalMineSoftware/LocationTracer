package com.coalminesoftware.locationtracer.transformation;

import android.location.Location;

import com.coalminesoftware.locationtracer.caching.LocationStore;

/**
 * Transforms observed {@link Location}s into a given type prior to being offered to a {@link LocationStore}.
 * <p>
 * For example, suppose that an application is used to help a company's delivery drivers manage their deliveries -
 * displaying deliveries that need to be made and reporting each driver's progress to some central location/server.  In
 * addition to the information present in a Location, that application might also need to report which delivery the
 * driver is currently making when a location is observed.  In that scenario, a DeliveryLocation class could be written
 * that extends Location and adds an <code>activeDelivery</code> field.  A transformer would then be needed to create a
 * DeliveryLocation from an observed Location and populate the new field.
 * <p>
 * A transformer could also be written to minimize an application's memory usage.  If an application is only concerned
 * with a subset of the fields present in a Location (perhaps just latitude, longitude and time,) locations could be
 * stored as instances of a new class that has no relation to Android's Location class.  Its corresponding transmformer
 * might looks like this:
 * 
 * <pre><code>public class MinimalLocationTransformer implements LocationTransformer&lt;MinimalLocation&gt; {
 *     public StorageLocation transformLocation(Location location) {
 *         return new MinimalLocation(location.getTime(), location.getLatitude(), location.getLongitude());
 *     }
 * }</code></pre>
 *
 * For users wishing to store Locations as-is, see {@link PassthroughLocationTransformer}.
 *
 * @param <StorageLocation>
 */
public interface LocationTransformer<StorageLocation> {
	StorageLocation transformLocation(Location location);
}
