package org.hisp.dhis.helpers.file;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriterBuilder;
import org.hisp.dhis.actions.IdGenerator;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class CsvFileReader
    implements org.hisp.dhis.helpers.file.FileReader
{
    private List<String[]> csvTable;

    private CSVReader reader;

    public CsvFileReader( File file )
        throws IOException
    {
        reader = new CSVReader( new FileReader( file ) );
        csvTable = reader.readAll();
    }

    @Override
    public org.hisp.dhis.helpers.file.FileReader read( File file )
        throws IOException
    {
        return new CsvFileReader( file );
    }

    @Override
    public org.hisp.dhis.helpers.file.FileReader replacePropertyValuesWithIds( String propertyName )
    {
        int columnIndex = Arrays.asList( csvTable.get( 0 ) ).indexOf( propertyName );

        String lastColumnOriginalValue = "";
        String lastColumnReplacedValue = "";
        for ( String[] row : csvTable
        )
        {

            if ( row[columnIndex].equals( propertyName ) )
            {
                continue;
            }
            if ( row[columnIndex].equals( lastColumnOriginalValue ) )
            {
                row[columnIndex] = lastColumnReplacedValue;
                continue;
            }

            lastColumnOriginalValue = row[columnIndex];
            lastColumnReplacedValue = new IdGenerator().generateUniqueId();

            row[columnIndex] = lastColumnReplacedValue;

        }

        return this;
    }

    @Override
    public org.hisp.dhis.helpers.file.FileReader replace( Function<Object, Object> function )
    {
        return null;
    }

    public String get()
    {
        StringWriter writer = new StringWriter();
        new CSVWriterBuilder( writer ).build().writeAll( csvTable );

        return writer.toString();
    }
}
