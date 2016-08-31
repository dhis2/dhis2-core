package org.hisp.dhis.node.serializers;

/*
 * Copyright (c) 2004-2015, University of Oslo
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

import com.fasterxml.jackson.dataformat.csv.CsvFactory;
import com.fasterxml.jackson.dataformat.csv.CsvGenerator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.google.common.collect.Lists;
import org.hisp.dhis.node.AbstractNodeSerializer;
import org.hisp.dhis.node.Node;
import org.hisp.dhis.node.types.CollectionNode;
import org.hisp.dhis.node.types.ComplexNode;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.node.types.SimpleNode;
import org.joda.time.DateTime;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Component
@Scope( value = "prototype", proxyMode = ScopedProxyMode.INTERFACES )
public class CsvNodeSerializer extends AbstractNodeSerializer
{
    private static final String[] CONTENT_TYPES = { "application/csv", "text/csv" };

    private static final CsvMapper CSV_MAPPER = new CsvMapper();

    private static final CsvFactory CSV_FACTORY = CSV_MAPPER.getFactory();

    private CsvGenerator csvGenerator;

    @Override
    public List<String> contentTypes()
    {
        return Lists.newArrayList( CONTENT_TYPES );
    }

    @Override
    protected void startSerialize( RootNode rootNode, OutputStream outputStream ) throws Exception
    {
        csvGenerator = CSV_FACTORY.createGenerator( outputStream );

        CsvSchema.Builder schemaBuilder = CsvSchema.builder()
            .setUseHeader( true );

        // build schema
        for ( Node child : rootNode.getChildren() )
        {
            if ( child.isCollection() )
            {
                if ( !child.getChildren().isEmpty() )
                {
                    Node node = child.getChildren().get( 0 );

                    for ( Node property : node.getChildren() )
                    {
                        if ( property.isSimple() )
                        {
                            schemaBuilder.addColumn( property.getName() );
                        }
                    }
                }
            }
        }

        csvGenerator.setSchema( schemaBuilder.build() );
    }

    @Override
    protected void flushStream() throws Exception
    {
        csvGenerator.flush();
    }

    @Override
    protected void startWriteRootNode( RootNode rootNode ) throws Exception
    {
        for ( Node child : rootNode.getChildren() )
        {
            if ( child.isCollection() )
            {
                for ( Node node : child.getChildren() )
                {
                    csvGenerator.writeStartObject();

                    for ( Node property : node.getChildren() )
                    {
                        if ( property.isSimple() )
                        {
                            writeSimpleNode( (SimpleNode) property );
                        }
                    }

                    csvGenerator.writeEndObject();
                }
            }
        }
    }

    @Override
    protected void endWriteRootNode( RootNode rootNode ) throws Exception
    {

    }

    @Override
    protected void startWriteSimpleNode( SimpleNode simpleNode ) throws Exception
    {
        String value = String.format( "%s", simpleNode.getValue() );

        if ( Date.class.isAssignableFrom( simpleNode.getValue().getClass() ) )
        {
            DateTime dateTime = new DateTime( simpleNode.getValue() );
            value = DT_FORMATTER.print( dateTime );
        }

        csvGenerator.writeObjectField( simpleNode.getName(), value );
    }

    @Override
    protected void endWriteSimpleNode( SimpleNode simpleNode ) throws Exception
    {

    }

    @Override
    protected void startWriteComplexNode( ComplexNode complexNode ) throws Exception
    {

    }

    @Override
    protected void endWriteComplexNode( ComplexNode complexNode ) throws Exception
    {

    }

    @Override
    protected void startWriteCollectionNode( CollectionNode collectionNode ) throws Exception
    {

    }

    @Override
    protected void endWriteCollectionNode( CollectionNode collectionNode ) throws Exception
    {

    }

    @Override
    protected void dispatcher( Node node ) throws Exception
    {

    }
}
