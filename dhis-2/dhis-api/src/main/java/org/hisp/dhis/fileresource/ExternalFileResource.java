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

import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.MergeMode;

import java.util.Date;

/**
 * @author Stian Sandvold
 */
public class ExternalFileResource
    extends BaseIdentifiableObject
{

    /**
     * FileResource containing the file we are exposing
     */
    private FileResource fileResource;

    /**
     * The accessToken required to identify ExternalFileResources trough the api
     */
    private String accessToken;

    /**
     * Date when the resource expires. Null means it never expires
     */
    private Date expires;

    public Date getExpires()
    {
        return expires;
    }

    public void setExpires( Date expires )
    {
        this.expires = expires;
    }

    public String getAccessToken()
    {
        return accessToken;
    }

    public void setAccessToken( String accessToken )
    {
        this.accessToken = accessToken;
    }

    public FileResource getFileResource()
    {
        return fileResource;
    }

    public void setFileResource( FileResource fileResource )
    {
        this.fileResource = fileResource;
    }

    @Override
    public void mergeWith( IdentifiableObject other, MergeMode mergeMode )
    {

        super.mergeWith( other, mergeMode );

        if ( other.getClass().isInstance( this ) )
        {

            ExternalFileResource externalFileResource = (ExternalFileResource) other;

            if ( mergeMode.isReplace() )
            {
                fileResource = externalFileResource.getFileResource();
                accessToken = externalFileResource.getAccessToken();
                expires = externalFileResource.getExpires();
            }

            if ( mergeMode.isMerge() )
            {
                fileResource = (externalFileResource.getFileResource() != null ?
                    externalFileResource.getFileResource() :
                    null);
                accessToken = (externalFileResource.getAccessToken() != null ?
                    externalFileResource.getAccessToken() :
                    null);
                expires = (externalFileResource.getExpires() != null ? externalFileResource.getExpires() : null);
            }
        }
    }
}
