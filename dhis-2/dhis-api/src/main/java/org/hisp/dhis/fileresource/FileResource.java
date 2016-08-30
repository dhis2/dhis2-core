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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;

import java.util.UUID;

/**
 * @author Halvdan Hoem Grelland
 */
public class FileResource
    extends BaseIdentifiableObject
{
    /**
     * MIME type.
     */
    private String contentType;

    /**
     * Byte size of content, non negative.
     */
    private long contentLength;

    /**
     * MD5 digest of content.
     */
    private String contentMd5;

    /**
     * Key used for content storage at external location.
     */
    private String storageKey;

    /**
     * Flag indicating whether the resource is assigned (e.g. to a DataValue) or
     * not. Unassigned FileResources are generally safe to delete when reaching
     * a certain age (unassigned objects might be in staging).
     */
    private boolean assigned = false;

    /**
     * The domain which this FileResource belongs to.
     */
    private FileResourceDomain domain;

    /**
     * Current storage status of content.
     */
    private FileResourceStorageStatus storageStatus = FileResourceStorageStatus.NONE;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public FileResource()
    {
    }

    public FileResource( String name, String contentType, long contentLength, String contentMd5, FileResourceDomain domain )
    {
        this.name = name;
        this.contentType = contentType;
        this.contentLength = contentLength;
        this.contentMd5 = contentMd5;
        this.domain = domain;
        this.storageKey = generateStorageKey();
    }

    // -------------------------------------------------------------------------
    // Overrides
    // -------------------------------------------------------------------------

    @Override
    public boolean haveUniqueNames()
    {
        return false;
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getContentType()
    {
        return contentType;
    }

    public void setContentType( String contentType )
    {
        this.contentType = contentType;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public long getContentLength()
    {
        return contentLength;
    }

    public void setContentLength( long contentLength )
    {
        this.contentLength = contentLength;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getContentMd5()
    {
        return contentMd5;
    }

    public void setContentMd5( String contentMd5 )
    {
        this.contentMd5 = contentMd5;
    }

    public String getStorageKey()
    {
        return storageKey;
    }

    public void setStorageKey( String storageKey )
    {
        this.storageKey = storageKey;
    }

    public boolean isAssigned()
    {
        return assigned;
    }

    public void setAssigned( boolean assigned )
    {
        this.assigned = assigned;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public FileResourceStorageStatus getStorageStatus()
    {
        return storageStatus;
    }

    public void setStorageStatus( FileResourceStorageStatus storageStatus )
    {
        this.storageStatus = storageStatus;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public FileResourceDomain getDomain()
    {
        return domain;
    }

    public void setDomain( FileResourceDomain domain )
    {
        this.domain = domain;
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private String generateStorageKey()
    {
        return domain.getContainerName() + "/" + UUID.randomUUID().toString();
    }
}
