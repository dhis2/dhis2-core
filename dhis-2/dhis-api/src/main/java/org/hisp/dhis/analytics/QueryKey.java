package org.hisp.dhis.analytics;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;

public class QueryKey
{
    private static final char SEPARATOR = '-';

    List<String> keyComponents = new ArrayList<>();

    public QueryKey()
    {
    }

    /**
     * Adds a component to this key. Null values are included.
     *
     * @param keyComponent the key component.
     */
    public QueryKey add( Object keyComponent )
    {
        this.keyComponents.add( String.valueOf( keyComponent ) );
        return this;
    }

    /**
     * Adds a component to this key. Null values are omitted.
     *
     * @param keyComponent the key component.
     */
    public QueryKey addIgnoreNull( Object keyComponent )
    {
        if ( keyComponent != null )
        {
            this.keyComponents.add( String.valueOf( keyComponent ) );
        }

        return this;
    }

    /**
     * Returns a plain text key based on the components of this key. Use
     * {@link QueryKey#build()} to obtain a shorter and more usable key.
     */
    public String asPlainKey()
    {
        return StringUtils.join( keyComponents, SEPARATOR );
    }

    /**
     * Returns a 40-character unique key. The key is a SHA-1 hash of
     * the components of this key.
     */
    public String build()
    {
        String key = StringUtils.join( keyComponents, SEPARATOR );
        return DigestUtils.shaHex( key );
    }

    /**
     * Equal to {@link QueryKey#build()}.
     */
    @Override
    public String toString()
    {
        return build();
    }
}
