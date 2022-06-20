package org.hisp.dhis.analytics.common;

/**
 * Simple interface that enables validation capabilities to the implementer.
 * 
 * @param <T>
 */
public interface Validator<T>
{
    void validate( T object );
}
