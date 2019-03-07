package org.hisp.dhis.helpers.file;

import java.io.File;
import java.io.IOException;
import java.util.function.Function;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public interface FileReader
{
    FileReader read( File file )
        throws IOException;

    FileReader replacePropertyValuesWithIds( String propertyValues );

    FileReader replace( Function<Object, Object> function );

    Object get();

    default <T> T get( Class<T> type )
    {
        T t = type.cast( get() );
        return t;
    }

}
