package org.hisp.dhis.tracker.preheat.mappers;

/**
 * @author Luciano Fiandesio
 */
public interface PreheatMapper<T>
{
    T map( T obj );
}
