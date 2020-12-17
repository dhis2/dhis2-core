package org.hisp.dhis.system.util;

import java.io.IOException;
import java.io.OutputStream;

import org.hisp.dhis.commons.config.JacksonObjectMapperConfig;

import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

public class JacksonCsvUtils
{
    /**
     * Writes the given response to the given output stream as CSV
     * using {@link CsvMapper}. The schema is inferred from the
     * given type using {@CsvSchema}. A header line is included.
     *
     * @param value the value to write.
     * @param out the {@link OutputStream} to write to.
     * @throws IOException if the write operation fails.
     */
    public static void toCsv( Object value, Class<?> type, OutputStream out )
        throws IOException
    {
        CsvMapper csvMapper = JacksonObjectMapperConfig.csvMapper;
        CsvSchema schema = csvMapper.schemaFor( type ).withHeader();
        ObjectWriter writer = csvMapper.writer( schema );
        writer.writeValue( out, value );
    }
}
