package org.hisp.dhis.helpers.file;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.hisp.dhis.actions.IdGenerator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.function.Function;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class JsonFileReader
    implements FileReader
{
    private JsonObject obj;

    public JsonFileReader( File file )
        throws IOException
    {
        byte[] content = Files.readAllBytes( Paths.get( file.getPath() ) );

        String json = new String( content );

        obj = new JsonParser().parse( json ).getAsJsonObject();
    }

    public JsonFileReader read( File file )
        throws IOException
    {
        return new JsonFileReader( file );
    }

    @Override
    public FileReader replacePropertyValuesWithIds( String propertyName )
    {
        replace( p -> {
            JsonObject object = ((JsonElement) p).getAsJsonObject();
            {
                if ( object.has( propertyName ) )
                {
                    object.addProperty( propertyName, new IdGenerator().generateUniqueId() );
                }
            }

            return object;
        } );

        return this;
    }

    public JsonObject get()
    {
        return obj;
    }

    @Override
    public JsonFileReader replace( Function<Object, Object> function )
    {
        JsonObject newObj = new JsonObject();
        for ( String key :
            obj.keySet() )
        {
            JsonElement element = obj.get( key );
            if ( element.isJsonArray() )
            {
                JsonArray array = new JsonArray();
                for ( JsonElement e :
                    element.getAsJsonArray() )
                {
                    array.add( (JsonElement) function.apply( e ) );
                }
                newObj.add( key, array );
            }

            else
            {
                newObj.add( key, (JsonElement) function.apply( element ) );
            }
        }

        obj = newObj;
        return this;
    }
}
