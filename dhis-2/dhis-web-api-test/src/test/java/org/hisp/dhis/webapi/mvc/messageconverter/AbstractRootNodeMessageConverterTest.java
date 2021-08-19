/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.webapi.mvc.messageconverter;

import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.withSettings;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;
import org.hisp.dhis.common.Compression;
import org.hisp.dhis.node.NodeService;
import org.hisp.dhis.node.types.RootNode;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.mock.http.MockHttpOutputMessage;

import com.google.common.net.HttpHeaders;

/**
 * Unit tests for {@link AbstractRootNodeMessageConverter}.
 *
 * @author Volker Schmidt <volker@dhis2.org>
 */
public class AbstractRootNodeMessageConverterTest
{
    @Mock
    private NodeService nodeService;

    private AbstractRootNodeMessageConverter converter;

    private AbstractRootNodeMessageConverter converterGzip;

    private AbstractRootNodeMessageConverter converterZip;

    private RootNode rootNode = new RootNode( "Test" + System.currentTimeMillis() );

    private MockHttpOutputMessage httpOutputMessage = new MockHttpOutputMessage();

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Before
    public void before()
    {
        converter = Mockito.mock( AbstractRootNodeMessageConverter.class, withSettings()
            .useConstructor( nodeService, "other/xzx", "xzx", Compression.NONE ).defaultAnswer( CALLS_REAL_METHODS ) );
        converterGzip = Mockito.mock( AbstractRootNodeMessageConverter.class, withSettings()
            .useConstructor( nodeService, "other/xzx", "xzx", Compression.GZIP ).defaultAnswer( CALLS_REAL_METHODS ) );
        converterZip = Mockito.mock( AbstractRootNodeMessageConverter.class, withSettings()
            .useConstructor( nodeService, "other/xzx", "xzx", Compression.ZIP ).defaultAnswer( CALLS_REAL_METHODS ) );
    }

    @Test
    public void isAttachmentNull()
    {
        Assert.assertFalse( converter.isAttachment( null ) );
    }

    @Test
    public void isAttachmentInline()
    {
        Assert.assertFalse( converter.isAttachment( "inline; filename=test.txt" ) );
    }

    @Test
    public void isAttachment()
    {
        Assert.assertTrue( converter.isAttachment( "attachment; filename=test.txt" ) );
    }

    @Test
    public void getExtensibleAttachmentFilenameNull()
    {
        Assert.assertNull( converter.getExtensibleAttachmentFilename( null ) );
    }

    @Test
    public void getExtensibleAttachmentFilenameInline()
    {
        Assert.assertNull( converter.getExtensibleAttachmentFilename( "inline; filename=metadata" ) );
    }

    @Test
    public void getExtensibleAttachmentFilename()
    {
        Assert.assertEquals( "metadata", converter.getExtensibleAttachmentFilename( "attachment; filename=metadata" ) );
    }

    @Test
    public void getExtensibleAttachmentFilenameOther()
    {
        Assert.assertNull( converter.getExtensibleAttachmentFilename( "attachment; filename=other" ) );
    }

    @Test
    public void writeInternalWithoutAttachmentUncompressed()
        throws IOException
    {
        Mockito.doAnswer( invocation -> {
            ((OutputStream) invocation.getArgument( 2 )).write( rootNode.getName().getBytes( StandardCharsets.UTF_8 ) );
            return null;
        } ).when( nodeService ).serialize( Mockito.same( rootNode ), Mockito.eq( "other/xzx" ),
            Mockito.any( OutputStream.class ) );
        converter.writeInternal( rootNode, httpOutputMessage );
        Assert.assertNull( httpOutputMessage.getHeaders().get( HttpHeaders.CONTENT_DISPOSITION ) );
        Assert.assertEquals( rootNode.getName(),
            new String( httpOutputMessage.getBodyAsBytes(), StandardCharsets.UTF_8 ) );
    }

    @Test
    public void writeInternalWithAttachmentUncompressed()
        throws IOException
    {
        Mockito.doAnswer( invocation -> {
            ((OutputStream) invocation.getArgument( 2 )).write( rootNode.getName().getBytes( StandardCharsets.UTF_8 ) );
            return null;
        } ).when( nodeService ).serialize( Mockito.same( rootNode ), Mockito.eq( "other/xzx" ),
            Mockito.any( OutputStream.class ) );
        httpOutputMessage.getHeaders().set( HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=metadata" );
        converter.writeInternal( rootNode, httpOutputMessage );
        Assert.assertNotNull( httpOutputMessage.getHeaders().get( HttpHeaders.CONTENT_DISPOSITION ) );
        Assert.assertEquals( 1, httpOutputMessage.getHeaders().get( HttpHeaders.CONTENT_DISPOSITION ).size() );
        Assert.assertEquals( "attachment; filename=metadata.xzx",
            httpOutputMessage.getHeaders().get( HttpHeaders.CONTENT_DISPOSITION ).get( 0 ) );
        Assert.assertEquals( rootNode.getName(),
            new String( httpOutputMessage.getBodyAsBytes(), StandardCharsets.UTF_8 ) );
    }

    @Test
    public void writeInternalWithAttachmentGzip()
        throws IOException
    {
        Mockito.doAnswer( invocation -> {
            ((OutputStream) invocation.getArgument( 2 )).write( rootNode.getName().getBytes( StandardCharsets.UTF_8 ) );
            return null;
        } ).when( nodeService ).serialize( Mockito.same( rootNode ), Mockito.eq( "other/xzx" ),
            Mockito.any( OutputStream.class ) );
        httpOutputMessage.getHeaders().set( HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=metadata" );
        converterGzip.writeInternal( rootNode, httpOutputMessage );
        Assert.assertNotNull( httpOutputMessage.getHeaders().get( HttpHeaders.CONTENT_DISPOSITION ) );
        Assert.assertEquals( 1, httpOutputMessage.getHeaders().get( HttpHeaders.CONTENT_DISPOSITION ).size() );
        Assert.assertEquals( "attachment; filename=metadata.xzx.gz",
            httpOutputMessage.getHeaders().get( HttpHeaders.CONTENT_DISPOSITION ).get( 0 ) );
        Assert.assertEquals( rootNode.getName(),
            IOUtils.toString( new GZIPInputStream( new ByteArrayInputStream( httpOutputMessage.getBodyAsBytes() ) ),
                StandardCharsets.UTF_8 ) );
    }

    @Test
    public void writeInternalWithoutAttachmentGzip()
        throws IOException
    {
        Mockito.doAnswer( invocation -> {
            ((OutputStream) invocation.getArgument( 2 )).write( rootNode.getName().getBytes( StandardCharsets.UTF_8 ) );
            return null;
        } ).when( nodeService ).serialize( Mockito.same( rootNode ), Mockito.eq( "other/xzx" ),
            Mockito.any( OutputStream.class ) );
        converterGzip.writeInternal( rootNode, httpOutputMessage );
        Assert.assertNotNull( httpOutputMessage.getHeaders().get( HttpHeaders.CONTENT_DISPOSITION ) );
        Assert.assertEquals( 1, httpOutputMessage.getHeaders().get( HttpHeaders.CONTENT_DISPOSITION ).size() );
        Assert.assertEquals( "attachment; filename=metadata.xzx.gz",
            httpOutputMessage.getHeaders().get( HttpHeaders.CONTENT_DISPOSITION ).get( 0 ) );
        Assert.assertEquals( rootNode.getName(),
            IOUtils.toString( new GZIPInputStream( new ByteArrayInputStream( httpOutputMessage.getBodyAsBytes() ) ),
                StandardCharsets.UTF_8 ) );
    }

    @Test
    public void writeInternalWithoutAttachmentZip()
        throws IOException
    {
        Mockito.doAnswer( invocation -> {
            ((OutputStream) invocation.getArgument( 2 )).write( rootNode.getName().getBytes( StandardCharsets.UTF_8 ) );
            return null;
        } ).when( nodeService ).serialize( Mockito.same( rootNode ), Mockito.eq( "other/xzx" ),
            Mockito.any( OutputStream.class ) );
        converterZip.writeInternal( rootNode, httpOutputMessage );
        Assert.assertNotNull( httpOutputMessage.getHeaders().get( HttpHeaders.CONTENT_DISPOSITION ) );
        Assert.assertEquals( 1, httpOutputMessage.getHeaders().get( HttpHeaders.CONTENT_DISPOSITION ).size() );
        Assert.assertEquals( "attachment; filename=metadata.xzx.zip",
            httpOutputMessage.getHeaders().get( HttpHeaders.CONTENT_DISPOSITION ).get( 0 ) );
        final ZipInputStream zipInputStream = new ZipInputStream(
            new ByteArrayInputStream( httpOutputMessage.getBodyAsBytes() ) );
        Assert.assertNotNull( zipInputStream.getNextEntry() );
        Assert.assertEquals( rootNode.getName(), IOUtils.toString( zipInputStream, StandardCharsets.UTF_8 ) );
    }

    @Test
    public void writeInternalWithAttachmentZip()
        throws IOException
    {
        Mockito.doAnswer( invocation -> {
            ((OutputStream) invocation.getArgument( 2 )).write( rootNode.getName().getBytes( StandardCharsets.UTF_8 ) );
            return null;
        } ).when( nodeService ).serialize( Mockito.same( rootNode ), Mockito.eq( "other/xzx" ),
            Mockito.any( OutputStream.class ) );
        httpOutputMessage.getHeaders().set( HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=metadata" );
        converterZip.writeInternal( rootNode, httpOutputMessage );
        Assert.assertNotNull( httpOutputMessage.getHeaders().get( HttpHeaders.CONTENT_DISPOSITION ) );
        Assert.assertEquals( 1, httpOutputMessage.getHeaders().get( HttpHeaders.CONTENT_DISPOSITION ).size() );
        Assert.assertEquals( "attachment; filename=metadata.xzx.zip",
            httpOutputMessage.getHeaders().get( HttpHeaders.CONTENT_DISPOSITION ).get( 0 ) );
        final ZipInputStream zipInputStream = new ZipInputStream(
            new ByteArrayInputStream( httpOutputMessage.getBodyAsBytes() ) );
        Assert.assertNotNull( zipInputStream.getNextEntry() );
        Assert.assertEquals( rootNode.getName(), IOUtils.toString( zipInputStream, StandardCharsets.UTF_8 ) );
    }
}