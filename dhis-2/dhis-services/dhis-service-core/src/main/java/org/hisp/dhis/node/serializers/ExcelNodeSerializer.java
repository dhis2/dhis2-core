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

import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFCreationHelper;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFHyperlink;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hisp.dhis.node.AbstractNodeSerializer;
import org.hisp.dhis.node.Node;
import org.hisp.dhis.node.types.CollectionNode;
import org.hisp.dhis.node.types.ComplexNode;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.node.types.SimpleNode;
import org.hisp.dhis.schema.PropertyType;
import org.joda.time.DateTime;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Component
@Scope( value = "prototype", proxyMode = ScopedProxyMode.INTERFACES )
public class ExcelNodeSerializer extends AbstractNodeSerializer
{
    private static final String[] CONTENT_TYPES = { "application/vnd.ms-excel" };

    @Override
    public List<String> contentTypes()
    {
        return Lists.newArrayList( CONTENT_TYPES );
    }

    private XSSFWorkbook workbook;

    private XSSFSheet sheet;

    @Override
    protected void startSerialize( RootNode rootNode, OutputStream outputStream ) throws Exception
    {
        workbook = new XSSFWorkbook();
        sheet = workbook.createSheet( "Sheet1" );

        XSSFFont boldFont = workbook.createFont();
        boldFont.setBold( true );

        XSSFCellStyle boldCellStyle = workbook.createCellStyle();
        boldCellStyle.setFont( boldFont );

        // build schema
        for ( Node child : rootNode.getChildren() )
        {
            if ( child.isCollection() )
            {
                if ( !child.getChildren().isEmpty() )
                {
                    Node node = child.getChildren().get( 0 );

                    XSSFRow row = sheet.createRow( 0 );

                    int cellIdx = 0;

                    for ( Node property : node.getChildren() )
                    {
                        if ( property.isSimple() )
                        {
                            XSSFCell cell = row.createCell( cellIdx++ );
                            cell.setCellValue( property.getName() );
                            cell.setCellStyle( boldCellStyle );
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void endSerialize( RootNode rootNode, OutputStream outputStream ) throws Exception
    {
        int columns = sheet.getRow( 0 ).getPhysicalNumberOfCells();

        for ( int i = 0; i < columns; i++ )
        {
            sheet.autoSizeColumn( i );
        }

        workbook.write( outputStream );
    }

    @Override
    protected void flushStream() throws Exception
    {

    }

    @Override
    protected void startWriteRootNode( RootNode rootNode ) throws Exception
    {
        XSSFCreationHelper creationHelper = workbook.getCreationHelper();

        int rowIdx = 1;

        for ( Node collectionNode : rootNode.getChildren() )
        {
            if ( collectionNode.isCollection() )
            {
                for ( Node complexNode : collectionNode.getChildren() )
                {
                    XSSFRow row = sheet.createRow( rowIdx++ );
                    int cellIdx = 0;

                    for ( Node node : complexNode.getChildren() )
                    {
                        if ( node.isSimple() )
                        {
                            XSSFCell cell = row.createCell( cellIdx++ );
                            cell.setCellValue( getValue( (SimpleNode) node ) );

                            if ( node.haveProperty() && PropertyType.URL.equals( node.getProperty().getPropertyType() ) )
                            {
                                XSSFHyperlink hyperlink = creationHelper.createHyperlink( Hyperlink.LINK_URL );
                                hyperlink.setAddress( getValue( (SimpleNode) node ) );
                                hyperlink.setLabel( getValue( (SimpleNode) node ) );

                                cell.setHyperlink( hyperlink );
                            }
                            else if ( node.haveProperty() && PropertyType.EMAIL.equals( node.getProperty().getPropertyType() ) )
                            {
                                XSSFHyperlink hyperlink = creationHelper.createHyperlink( Hyperlink.LINK_EMAIL );
                                hyperlink.setAddress( getValue( (SimpleNode) node ) );
                                hyperlink.setLabel( getValue( (SimpleNode) node ) );

                                cell.setHyperlink( hyperlink );
                            }
                        }
                    }
                }
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
            DateTime dateTime = new DateTime( simpleNode.getValue() );
            value = DT_FORMATTER.print( dateTime );
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
}
