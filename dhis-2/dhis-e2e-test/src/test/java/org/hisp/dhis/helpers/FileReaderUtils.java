package org.hisp.dhis.helpers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.hisp.dhis.actions.IdGenerator;
import org.hisp.dhis.utils.DataGenerator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class FileReaderUtils
{
    /**
     * Reads json file and generates data where needed.
     * Json file should indicate what data should be generated.
     * Format: %string%, %id%
     *
     * @param file
     * @return
     * @throws IOException
     */
    public JsonObject readJsonAndGenerateData( File file )
        throws IOException
    {
        byte[] content = Files.readAllBytes( Paths.get( file.getPath() ) );

        String json = new String( content );

        JsonObject object = new JsonParser().parse( json ).getAsJsonObject();

        JsonObject newOb = new JsonObject();
        for ( String key :
            object.keySet() )
        {
            JsonElement element = object.get( key );
            if ( element.isJsonArray() )
            {
                JsonArray array = new JsonArray();
                for ( JsonElement e :
                    element.getAsJsonArray() )
                {
                    array.add( replaceAll( e ) );
                }
                newOb.add( key, array );
            }

            else
            {
                newOb.add( key, replaceAll( element ) );
            }
        }

        return newOb;
    }

    private JsonElement replaceAll( JsonElement element )
    {
        String json = element.toString();

        json = json.replaceAll( "%id%", new IdGenerator().generateUniqueId() );
        json = json.replaceAll( "%string%", DataGenerator.randomString() );

        return new JsonParser().parse( json );
    }
}
