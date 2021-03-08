package org.hisp.dhis.webapi.json;

import java.net.URL;

/**
 * A {@link JsonURL} is a {@link JsonString} with a URL format.
 *
 * The {@link #url()} utility method allows to access the JSON string node as
 * {@link URL}.
 *
 * @author Jan Bernitt
 */
public interface JsonURL extends JsonString
{

    default URL url()
    {
        return converted( () -> new URL( string() ) );
    }
}
