package org.hisp.dhis.document;

/*
 * Copyright (c) 2004-2017, University of Oslo
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
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.MetadataObject;
import org.hisp.dhis.fileresource.FileResource;

/**
 * @author Lars Helge Overland
 */
@JacksonXmlRootElement( localName = "document", namespace = DxfNamespaces.DXF_2_0 )
public class Document
    extends BaseIdentifiableObject implements MetadataObject
{
    /**
     * Can be either a valid URL, or the path (filename) of a file.
     * If the external property is true, this should be an URL.
     * If the external property is false, this should be the filename
     */
    private String url;

    /**
     * A reference to the file associated with the Document. If document represents
     * an URL or a file uploaded before this property was added, this will be null.
     */
    private FileResource fileResource;

    /**
     * Determines if this document refers to a file (!external) or URL (external).
     */
    private boolean external;

    /**
     * The content type of the file referred to by the document, or null if document
     * refers to an URL
     */
    private String contentType;

    /**
     * Flags whether the file should be displayed in-browser or downloaded.
     * true should trigger a download of the file when accessing the document data
     */
    private Boolean attachment = false;

    public Document()
    {
    }

    public Document( String name, String url, boolean external, String contentType )
    {
        this.name = name;
        this.url = url;
        this.external = external;
        this.contentType = contentType;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    // @Property( PropertyType.URL )
    public String getUrl()
    {
        return url;
    }

    public void setUrl( String url )
    {
        this.url = url;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isExternal()
    {
        return external;
    }

    public void setExternal( boolean external )
    {
        this.external = external;
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
    public Boolean getAttachment()
    {
        return attachment;
    }

    public void setAttachment( Boolean attachment )
    {
        this.attachment = attachment;
    }

    // Should not be exposed in the api
    public FileResource getFileResource()
    {
        return fileResource;
    }

    public void setFileResource( FileResource fileResource )
    {
        this.fileResource = fileResource;
    }
}
