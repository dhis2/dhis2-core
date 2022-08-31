/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.node.serializer;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.node.serializers.CsvNodeSerializer;
import org.hisp.dhis.node.types.CollectionNode;
import org.hisp.dhis.node.types.ComplexNode;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.node.types.SimpleNode;
import org.junit.After;
import org.junit.Test;

import com.fasterxml.jackson.dataformat.csv.CsvWriteException;

@Slf4j
public class CsvNodeSerializerTest
{

    private CsvNodeSerializer csvNodeSerializer = new CsvNodeSerializer();

    @Test
    public void CsvFileIsWrittenWhenOnlySimpleNodesAreProvided()
        throws Exception
    {
        csvNodeSerializer.serialize( createCollectionWithSimpleNodes(), new FileOutputStream( "output.csv" ) );

        List<List<String>> values = readTestFile();

        assertEquals( 2, values.size() );
        assertEquals( 3, values.get( 0 ).size() );
        assertEquals( 3, values.get( 1 ).size() );
        assertEquals( "\"First simple node\"", values.get( 0 ).get( 0 ) );
        assertEquals( "\"Second value simple child\"", values.get( 1 ).get( 1 ) );
        assertEquals( "\"Third simple node\"", values.get( 0 ).get( 2 ) );
    }

    @Test
    public void CsvFileIsWrittenWhenAttributesAreProvided()
        throws Exception
    {
        csvNodeSerializer.serialize( createComplexCollection( "attributes", true ),
            new FileOutputStream( "output.csv" ) );

        List<List<String>> values = readTestFile();

        assertEquals( 2, values.size() );
        assertEquals( 6, values.get( 0 ).size() );
        assertEquals( 6, values.get( 1 ).size() );
        assertEquals( "\"First simple node\"", values.get( 0 ).get( 0 ) );
        assertEquals( "\"Second attr\"", values.get( 0 ).get( 3 ) );
        assertEquals( "\"Fourth attr\"", values.get( 0 ).get( 5 ) );
        assertEquals( "\"First attr value\"", values.get( 1 ).get( 2 ) );
        assertEquals( "\"Third attr value\"", values.get( 1 ).get( 4 ) );
    }

    @Test
    public void CsvFileIsWrittenWhenSimpleNodesAreProvidedButAttributeComplexNodeIsNot()
        throws Exception
    {
        csvNodeSerializer.serialize( createComplexCollection( "enrollments", true ),
            new FileOutputStream( "output.csv" ) );

        List<List<String>> values = readTestFile();

        assertEquals( 2, values.size() );
        assertEquals( 2, values.get( 0 ).size() );
        assertEquals( 2, values.get( 1 ).size() );
        assertEquals( "\"First simple node\"", values.get( 0 ).get( 0 ) );
        assertEquals( "\"Second value simple child\"", values.get( 1 ).get( 1 ) );
    }

    @Test
    public void CsvFileIsNotWrittenWhenNoSimpleNodesNorAttributeComplexNodeAreProvided()
        throws Exception
    {
        Exception exception = assertThrows( CsvWriteException.class, () -> csvNodeSerializer
            .serialize( createComplexCollection( "enrollments", false ), new FileOutputStream( "output.csv" ) ) );

        String expectedMessage = "Schema specified that header line is to be written; but contains no column names";
        String actualMessage = exception.getMessage();

        assertTrue( actualMessage.contains( expectedMessage ) );
    }

    @After
    public void cleanUp()
    {
        File f = new File( "output.csv" );
        if ( !f.delete() )
        {
            log.warn( "Not possible to delete file {}", f.getName() );
        }
    }

    private RootNode createCollectionWithSimpleNodes()
    {
        CollectionNode collectionRootNode = new CollectionNode( "Complex node" );
        CollectionNode firstCollectionNode = new CollectionNode( "First collection child" );
        CollectionNode secondCollectionNode = new CollectionNode( "Second collection child" );

        SimpleNode firstSimpleNode = new SimpleNode( "First simple node", "First value simple child" );
        SimpleNode secondSimpleNode = new SimpleNode( "Second simple node", "Second value simple child" );
        SimpleNode thirdSimpleNode = new SimpleNode( "Third simple node", "Third value simple child" );

        secondCollectionNode.addChild( firstSimpleNode );
        secondCollectionNode.addChild( secondSimpleNode );
        secondCollectionNode.addChild( thirdSimpleNode );
        firstCollectionNode.addChild( secondCollectionNode );
        collectionRootNode.addChild( firstCollectionNode );

        return new RootNode( collectionRootNode );
    }

    private RootNode createComplexCollection( String complexNodeName, boolean includeSimpleNodes )
    {
        CollectionNode collectionRootNode = new CollectionNode( "Complex node" );
        CollectionNode firstCollectionNode = new CollectionNode( "First collection child" );
        CollectionNode secondCollectionNode = new CollectionNode( "Second collection child" );

        ComplexNode attributesComplexNode = new ComplexNode( complexNodeName );
        ComplexNode attributeNodeChild = new ComplexNode( "attribute child" );
        attributeNodeChild.addChild( new SimpleNode( "First attr", "First attr value" ) );
        attributeNodeChild.addChild( new SimpleNode( "Second attr", "Second attr value" ) );
        attributeNodeChild.addChild( new SimpleNode( "Third attr", "Third attr value" ) );
        attributeNodeChild.addChild( new SimpleNode( "Fourth attr", "Fourth attr value" ) );

        attributesComplexNode.addChild( attributeNodeChild );

        if ( includeSimpleNodes )
        {
            SimpleNode firstSimpleNode = new SimpleNode( "First simple node", "First value simple child" );
            SimpleNode secondSimpleNode = new SimpleNode( "Second simple node", "Second value simple child" );
            secondCollectionNode.addChild( firstSimpleNode );
            secondCollectionNode.addChild( secondSimpleNode );
        }

        secondCollectionNode.addChild( attributesComplexNode );
        firstCollectionNode.addChild( secondCollectionNode );
        collectionRootNode.addChild( firstCollectionNode );

        return new RootNode( collectionRootNode );
    }

    private List<List<String>> readTestFile()
        throws IOException
    {
        List<List<String>> values = new ArrayList<>();
        try ( BufferedReader br = new BufferedReader( new FileReader( "output.csv" ) ) )
        {
            String line;
            while ( (line = br.readLine()) != null )
            {
                values.add( Arrays.asList( line.split( "," ) ) );
            }
        }

        return values;
    }
}