package org.hisp.dhis.helpers.file;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.hisp.dhis.actions.IdGenerator;
import org.hisp.dhis.utils.DataGenerator;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class FileReaderUtils
{
    private Logger logger = Logger.getLogger( FileReaderUtils.class.getName() );

    public FileReader read( File file )
        throws IOException
    {
        if ( file.getName().endsWith( ".csv" ) )
        {
            return new CsvFileReader( file );
        }

        if ( file.getName().endsWith( ".json" ) )
        {
            return new JsonFileReader( file );
        }

        logger.warning( "Tried to read file " + file.getName() + ", but there is no reader implemented for this file type. " );

        return null;
    }
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
        return read( file ).replace( e -> {
            String json = e.toString();

            json = json.replaceAll( "%id%", new IdGenerator().generateUniqueId() );
            json = json.replaceAll( "%string%", DataGenerator.randomString() );

            return new JsonParser().parse( json );
        } ).get( JsonObject.class );
    }
}
