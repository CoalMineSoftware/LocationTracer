# LocationTracer

This library takes care of the wiring needed to record and upload location traces in an Android application.

It takes care of registering for location updates - both active and passive, with an option to request an active location update when a passive update isn't observed for a given amount of time.

The process of uploading a trace is delegated to a callback that an API client would implement.

Storing locations in between uploads is also delegated to a callback.  A simple in-memory implementation is provided but API users may choose to implement more sophisticated LocationStores that save locations to persistent memory.
