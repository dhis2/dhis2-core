package org.hisp.dhis.webapi.controller;
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

import com.google.common.hash.Hashing;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;
import org.hisp.dhis.externalfileresource.ExternalFileResource;
import org.hisp.dhis.externalfileresource.ExternalFileResourceService;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceDomain;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.schema.descriptors.ExternalFileResourceSchemaDescriptor;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * @author Stian Sandvold
 */
@Controller
@RequestMapping( ExternalFileResourceSchemaDescriptor.API_ENDPOINT )
@ApiVersion( { ApiVersion.Version.DEFAULT, ApiVersion.Version.ALL } )
public class ExternalFileResourceController
{

    @Autowired
    private ExternalFileResourceService externalFileResourceService;

    @Autowired
    private FileResourceService fileResourceService;

    @RequestMapping( value = "/{accessToken}", method = RequestMethod.GET )
    public ExternalFileResource getExternalFileResource( @PathVariable String accessToken,
        HttpServletResponse response )
    {
        testExternalFileResource();

        System.out.println( "AccessToken: " + accessToken );

        ExternalFileResource externalFileResource = externalFileResourceService
            .getExternalFileResourceByAccesstoken( accessToken );

        return externalFileResource;

    }

    public void testExternalFileResource(
    )
    {

        File f = new File( "delete_me5.txt" );

        try
        {
            FileWriter fileWriter = new FileWriter( f );
            fileWriter.write( "delete_me5!" );
            fileWriter.close();
        }
        catch ( IOException e )
        {
            e.printStackTrace();
        }

        FileResource fr = new FileResource();


        ByteSource bytes = Files.asByteSource(f);
        String contentMd5 = null;

        try
        {
            contentMd5 = bytes.hash( Hashing.md5() ).toString();
        }
        catch ( IOException e )
        {
            e.printStackTrace();
        }

        fr.setName( "delete_me5.txt" );
        fr.setContentLength( f.length() );
        fr.setContentMd5( contentMd5 );
        fr.setContentType( MimeTypeUtils.TEXT_PLAIN.toString() );
        fr.setDomain( FileResourceDomain.EXTERNAL );
        fr.setStorageKey( "delete_me5" );

        fileResourceService.saveFileResource( fr, f );

        ExternalFileResource externalFileResource = new ExternalFileResource();

        externalFileResource.setAccessToken( "TEST" );
        externalFileResource.setExpires( null );
        externalFileResource.setFileResource( fr );
        externalFileResource.setName( "test" );

        externalFileResourceService.saveExternalFileResource( externalFileResource );

    }

}
