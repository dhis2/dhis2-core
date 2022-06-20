package org.hisp.dhis.analytics.common;

/**
 * Interface responsible for processing and/or transforming the internals of
 * objects of the given type.
 * 
 * @param <T>
 */
public interface Processor<T>
{
    T process( T object );
}
