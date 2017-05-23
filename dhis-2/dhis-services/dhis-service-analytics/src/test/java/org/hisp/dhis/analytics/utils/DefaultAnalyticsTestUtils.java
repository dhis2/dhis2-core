package org.hisp.dhis.analytics.utils;

import com.csvreader.CsvReader;
import com.google.common.collect.Sets;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;

import static org.junit.Assert.assertNotNull;

/**
 * Created by henninghakonsen on 23/05/2017.
 * Project: dhis-2.
 */
public class DefaultAnalyticsTestUtils
    implements AnalyticsTestUtils
{
    public void configureHierarchy( OrganisationUnit ouA, OrganisationUnit ouB,
        OrganisationUnit ouC, OrganisationUnit ouD, OrganisationUnit ouE )
    {
        ouA.getChildren().addAll( Sets.newHashSet( ouB, ouC ) );
        ouB.setParent( ouA );
        ouC.setParent( ouA );

        ouB.getChildren().addAll( Sets.newHashSet( ouD, ouE ) );
        ouD.setParent( ouB );
        ouE.setParent( ouB );
    }

    public ArrayList<String[]> readInputFile( String inputFile )
        throws IOException
    {
        InputStream input = new ClassPathResource( inputFile ).getInputStream();
        assertNotNull( "Reading '" + inputFile + "' failed", input );

        CsvReader reader = new CsvReader( input, Charset.forName( "UTF-8" ) );

        reader.readRecord(); // Ignore first row
        ArrayList<String[]> lines = new ArrayList<>();
        while ( reader.readRecord() )
        {
            String[] values = reader.getValues();
            lines.add( values );
        }

        return lines;
    }
}
