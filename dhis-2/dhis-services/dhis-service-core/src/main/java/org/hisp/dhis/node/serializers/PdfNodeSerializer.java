package org.hisp.dhis.node.serializers;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import com.google.common.collect.Lists;
import com.lowagie.text.Document;
import com.lowagie.text.pdf.PdfPTable;
import org.hisp.dhis.node.AbstractNodeSerializer;
import org.hisp.dhis.node.Node;
import org.hisp.dhis.node.types.CollectionNode;
import org.hisp.dhis.node.types.ComplexNode;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.node.types.SimpleNode;
import org.hisp.dhis.system.util.DateUtils;
import org.hisp.dhis.system.util.PDFUtils;
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
public class PdfNodeSerializer extends AbstractNodeSerializer
{
    private static final String[] CONTENT_TYPES = { "application/pdf" };

    private Document document;

    @Override
    public List<String> contentTypes()
    {
        return Lists.newArrayList( CONTENT_TYPES );
    }

    @Override
    protected void startSerialize( RootNode rootNode, OutputStream outputStream ) throws Exception
    {
        document = PDFUtils.openDocument( outputStream );
    }

    @Override
    protected void endSerialize( RootNode rootNode, OutputStream outputStream ) throws Exception
    {
        if ( document.isOpen() )
        {
            document.close();
        }
    }

    @Override
    protected void flushStream() throws Exception
    {

    }

    @Override
    protected void startWriteRootNode( RootNode rootNode ) throws Exception
    {
        for ( Node child : rootNode.getChildren() )
        {
            if ( child.isCollection() )
            {
                PdfPTable table = PDFUtils.getPdfPTable( true, 0.25f, 0.75f );
                boolean haveContent = false;

                for ( Node node : child.getChildren() )
                {
                    for ( Node property : node.getChildren() )
                    {
                        if ( property.isSimple() )
                        {
                            table.addCell( PDFUtils.getItalicCell( property.getName() ) );
                            table.addCell( PDFUtils.getTextCell( getValue( (SimpleNode) property ) ) );
                            haveContent = true;
                        }
                    }

                    if ( haveContent )
                    {
                        table.addCell( PDFUtils.getEmptyCell( 2, 15 ) );
                        haveContent = false;
                    }
                }

                document.add( table );
            }
        }
    }

    public String getValue( SimpleNode simpleNode )
    {
        if ( simpleNode.getValue() == null )
        {
            return "";
        }

        String value = String.format( "%s", simpleNode.getValue() );

        if ( Date.class.isAssignableFrom( simpleNode.getValue().getClass() ) )
        {
            value = DateUtils.getIso8601NoTz( (Date) simpleNode.getValue() );
        }

        return value;
    }

    @Override
    protected void endWriteRootNode( RootNode rootNode ) throws Exception
    {

    }

    @Override
    protected void startWriteSimpleNode( SimpleNode simpleNode ) throws Exception
    {

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
