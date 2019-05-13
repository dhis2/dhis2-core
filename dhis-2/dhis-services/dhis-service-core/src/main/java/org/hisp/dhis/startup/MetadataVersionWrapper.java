package org.hisp.dhis.startup;/*
 * Copyright (c) 2004-2018, University of Oslo
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
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.keyjsonvalue.KeyJsonValue;
import org.hisp.dhis.keyjsonvalue.KeyJsonValueService;
import org.hisp.dhis.metadata.version.MetadataVersion;
import org.hisp.dhis.metadata.version.MetadataVersionService;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.system.startup.AbstractStartupRoutine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Objects;

/**
 * @author David Katuscak
 */
public class MetadataVersionWrapper extends AbstractStartupRoutine
{
    private static final Logger log = LoggerFactory.getLogger( MetadataVersionWrapper.class );

    private static final String METADATASTORE_NAMESPACE = "METADATASTORE";
    private static final String WRAPPED_METADATA_PREFIX = "{\"metadata\":";

    private final KeyJsonValueService keyJsonValueService;
    private final MetadataVersionService metadataVersionService;
    private final RenderService renderService;

    public MetadataVersionWrapper( KeyJsonValueService keyJsonValueService,
        MetadataVersionService metadataVersionService, RenderService renderService )
    {
        this.keyJsonValueService = keyJsonValueService;
        this.metadataVersionService = metadataVersionService;
        this.renderService = renderService;
    }

    @Override
    public void execute() throws Exception
    {
        List<MetadataVersion> metadataVersions = metadataVersionService.getAllVersions();

        for ( MetadataVersion metadataVersion : metadataVersions )
        {
            KeyJsonValue keyJsonValue = keyJsonValueService.getKeyJsonValue( METADATASTORE_NAMESPACE,
                metadataVersion.getName() );

            String value = keyJsonValue.getValue();
            if ( value != null && !value.substring( 0, WRAPPED_METADATA_PREFIX.length() ).equals( WRAPPED_METADATA_PREFIX ) )
            {
                MetadataWrapper metadataWrapper = new MetadataWrapper( value );

                //MetadataWrapper is used to avoid Metadata keys reordering by jsonb (jsonb does not preserve keys order)
                keyJsonValue.setValue( renderService.toJsonAsString( metadataWrapper ) );
                keyJsonValueService.updateKeyJsonValue( keyJsonValue );

                metadataVersion.setHashCode( getHashCode( keyJsonValue.getValue() ) );

                metadataVersionService.updateVersion( metadataVersion );
            }
        }
    }

    private static String getHashCode( String value ) throws NoSuchAlgorithmException
    {
        byte[] bytesOfMessage = value.getBytes( StandardCharsets.UTF_8 );
        MessageDigest md = MessageDigest.getInstance( "MD5" );
        byte[] digest = md.digest( bytesOfMessage );

        StringBuilder hexString = new StringBuilder();
        for ( byte aDigest : digest )
        {
            String hex = Integer.toHexString( 0xFF & aDigest );
            if ( hex.length() == 1 )
                hexString.append( '0' );
            hexString.append( hex );
        }
        return hexString.toString();
    }

    @JacksonXmlRootElement( localName = "metadataPayload", namespace = DxfNamespaces.DXF_2_0 )
    private class MetadataWrapper
    {
        private String metadata;

        public MetadataWrapper( )
        {
        }

        MetadataWrapper( String metadata )
        {
            this.metadata = metadata;
        }

        @JsonProperty( "metadata" )
        @JacksonXmlProperty( localName = "metadata", namespace = DxfNamespaces.DXF_2_0 )
        public String getMetadata()
        {
            return metadata;
        }

        public void setMetadata( String metadata )
        {
            this.metadata = metadata;
        }

        @Override
        public boolean equals( Object o )
        {
            if ( this == o )
            {
                return true;
            }

            if ( o == null || getClass() != o.getClass() )
            {
                return false;
            }

            MetadataWrapper temp = (MetadataWrapper) o;

            return Objects.equals( temp.getMetadata(), this.getMetadata() );
        }

        @Override
        public int hashCode()
        {
            return metadata != null ? metadata.hashCode() : 0;
        }

        @Override
        public java.lang.String toString()
        {
            return "MetadataWrapper{" +
                "metadata=" + metadata +
                '}';
        }
    }
}
