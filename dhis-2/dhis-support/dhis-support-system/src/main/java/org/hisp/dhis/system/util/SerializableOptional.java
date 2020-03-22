package org.hisp.dhis.system.util;

import java.io.Serializable;

/**
 * Optional for {@link Serializable} values. Accepts nulls.
 *
 * @author Lars Helge Overland
 */
public class SerializableOptional
{
    private final Serializable value;

    private SerializableOptional()
    {
        this.value = null;
    }

    private SerializableOptional( Serializable value )
    {
        this.value = value;
    }

    public static SerializableOptional of( Serializable value )
    {
        return new SerializableOptional( value );
    }

    public static SerializableOptional empty()
    {
        return new SerializableOptional();
    }

    public boolean isPresent()
    {
        return value != null;
    }

    public Serializable get()
    {
        return value;
    }
}
