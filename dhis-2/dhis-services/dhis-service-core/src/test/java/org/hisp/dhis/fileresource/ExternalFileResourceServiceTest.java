package org.hisp.dhis.fileresource;
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
import org.hisp.dhis.DhisSpringTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.Assert.*;

/**
 * @author Stian Sandvold
 */
public class ExternalFileResourceServiceTest
    extends DhisSpringTest
{

    @Autowired
    ExternalFileResourceStore externalFileResourceStore;

    @Autowired
    ExternalFileResourceService externalFileResourceService;

    @Autowired
    FileResourceService fileResourceService;

    FileResource fileResourceA;

    ExternalFileResource externalFileResourceA;

    @Override
    public void setUpTest()
        throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write( 123 );
        baos.close();

        byte[] bytes = baos.toByteArray();
        String md5 = ByteSource.wrap( bytes ).hash( Hashing.md5() ).toString();
        fileResourceA = new FileResource( "TEST NAME", "TEXT", baos.size(), md5, FileResourceDomain.EXTERNAL );
        fileResourceService.saveFileResource( fileResourceA, bytes );

        externalFileResourceA = new ExternalFileResource();
        externalFileResourceA.setFileResource( fileResourceA );
        externalFileResourceA.setName( "TEST" );
        externalFileResourceA.setExpires( null );
        externalFileResourceA.setAccessToken( ExternalFileResourceTokenGenerator.generate() );

        externalFileResourceStore.save( externalFileResourceA );
    }

    @Test
    public void testGetExternalFileResourceByAccessToken()
    {
        assertEquals(
            externalFileResourceA,
            externalFileResourceService.getExternalFileResourceByAccessToken( externalFileResourceA.getAccessToken() )
        );
    }
}
