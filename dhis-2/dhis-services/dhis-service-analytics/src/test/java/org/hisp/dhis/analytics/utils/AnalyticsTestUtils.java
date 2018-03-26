package org.hisp.dhis.analytics.utils;

/*
 * Copyright (c) 2004-2018, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import com.csvreader.CsvReader;
import com.google.common.collect.Sets;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.dxf2.datavalueset.DataValueSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author Henning Haakonsen
 */
public class AnalyticsTestUtils
{
    /**
     * Configure org unit hierarchy like so:
     *
     *          A
     *         / \
     *        B   C
     *       / \
     *      D   E
     *
     * @param ouA root
     * @param ouB leftRoot
     * @param ouC rightRoot
     * @param ouD leftB
     * @param ouE rightB
     */
    public static void configureHierarchy( OrganisationUnit ouA, OrganisationUnit ouB,
        OrganisationUnit ouC, OrganisationUnit ouD, OrganisationUnit ouE )
    {
        ouA.getChildren().addAll( Sets.newHashSet( ouB, ouC ) );
        ouB.setParent( ouA );
        ouC.setParent( ouA );

        ouB.getChildren().addAll( Sets.newHashSet( ouD, ouE ) );
        ouD.setParent( ouB );
        ouE.setParent( ouB );
    }

    /**
     * Reads CSV input file.
     *
     * @param inputFile points to file in class path
     * @return list of list of strings
     */
    public static List<String[]> readInputFile( String inputFile )
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

    /**
     * Test if values from keyValue corresponds with values in
     * aggregatedResultMapping. Also test for null values.
     *
     * @param aggregatedResultData aggregated results
     * @param keyValue expected results
     */
    public static void assertResultGrid( Grid aggregatedResultData, Map<String, Double> keyValue )
    {
        assertNotNull( aggregatedResultData );
        
        for ( int i = 0; i < aggregatedResultData.getRows().size(); i++ )
        {
            int numberOfDimensions = aggregatedResultData.getRows().get( 0 ).size() - 1;

            StringBuilder key = new StringBuilder();
            
            for ( int j = 0; j < numberOfDimensions; j++ )
            {
                key.append( aggregatedResultData.getValue( i, j ).toString() );
                
                if ( j != numberOfDimensions - 1 )
                {
                    key.append( "-" );
                }
            }

            Double expected = keyValue.get( key.toString() );
            Double actual = Double.parseDouble( aggregatedResultData.getValue( i, numberOfDimensions ).toString() );

            assertNotNull( "Did not find '" + key + "' in provided results", expected );
            assertNotNull( aggregatedResultData.getRow( i ) );
            assertEquals( "Value for key: '" + key + "' not matching expected value: '" + expected + "'", expected, actual );
        }
    }

    /**
     * Test if values from keyValue corresponds with values in
     * aggregatedDataValueMapping. Also test for null values, and "" as key in
     * aggregatedDataValueMapping
     *
     * @param aggregatedResultMapping aggregated values
     * @param keyValue expected results
     */
    public static void assertResultMapping( Map<String, Object> aggregatedResultMapping,
        Map<String, Double> keyValue )
    {
        assertNotNull( aggregatedResultMapping );
        assertNull( aggregatedResultMapping.get( "testNull" ) );
        assertNull( aggregatedResultMapping.get( "" ) );

        for ( Map.Entry<String, Object> entry : aggregatedResultMapping.entrySet() )
        {
            String key = entry.getKey();
            Double expected = keyValue.get( key );
            Double actual = (Double) entry.getValue();

            assertNotNull( "Did not find '" + key + "' in provided results", expected );
            assertEquals( "Value for key:'" + key + "' not matching expected value: '" + expected + "'", expected, actual );
        }
    }

    /**
     * Test if values from keyValue corresponds with values in
     * aggregatedDataValueSet. Also test for null values.
     *
     * @param aggregatedResultSet aggregated values
     * @param keyValue expected results
     */
    public static void assertResultSet( DataValueSet aggregatedResultSet, Map<String, Double> keyValue )
    {
        for ( org.hisp.dhis.dxf2.datavalue.DataValue dataValue : aggregatedResultSet.getDataValues() )
        {
            String key = dataValue.getDataElement() + "-" + dataValue.getOrgUnit() + "-" + dataValue.getPeriod();

            assertNotNull( keyValue.get( key ) );
            Double actual = Double.parseDouble( dataValue.getValue() );
            Double expected = keyValue.get( key );

            assertEquals( "Value for key: '" + key + "' not matching expected value: '" + expected + "'", expected, actual );
        }
    }
}
