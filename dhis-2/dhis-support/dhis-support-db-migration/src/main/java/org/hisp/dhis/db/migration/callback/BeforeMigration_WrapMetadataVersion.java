package org.hisp.dhis.db.migration.callback;
/*
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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import javafx.util.Pair;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.metadata.version.MetadataVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author David Katuscak
 */
public class BeforeMigration_WrapMetadataVersion implements Callback
{
    private static final Logger log = LoggerFactory.getLogger( BeforeMigration_WrapMetadataVersion.class );

    private static final String WRAPPED_METADATA_PREFIX = "{\"metadata\":";

    private static final String VALUE_COLUMN_EXISTS_SQL = "SELECT count(0) FROM information_schema.columns " +
        "WHERE table_schema = 'public' AND table_name = 'keyjsonvalue' AND column_name = 'value';";

    private static final String GET_ALL_METADATA_VERSIONS_SQL = "SELECT uid, name, hashcode FROM metadataversion";
    private static final String UPDATE_METADATA_VERSION_SQL = "UPDATE metadataversion SET hashcode = ? WHERE uid = ?";
    private static final String GET_KEYJSONVALUE_SQL = "SELECT value, uid FROM keyjsonvalue WHERE namespace='METADATASTORE' AND namespacekey = ?";
    private static final String UPDATE_KEYJSONVALUE_SQL = "UPDATE keyjsonvalue SET value = ? WHERE uid = ?";

    private static final ObjectMapper mapper = new ObjectMapper();

    static
    {
        mapper.enableDefaultTyping();
        mapper.setSerializationInclusion( JsonInclude.Include.NON_NULL );
    }

    @Override public boolean supports( final Event event, final Context context )
    {
        return event == Event.BEFORE_MIGRATE;
    }

    @Override public boolean canHandleInTransaction( final Event event, final Context context )
    {
        return true;
    }

    @Override public void handle( final Event event, final Context context )
    {
        //TODO: check that hashCode is calculated in the same way also in the metadata import/sync
        try
        {
            if ( necessaryToRunMigration( context ) )
            {
                log.info( "Migration necessary" );
                List<MetadataVersion> metadataVersions = getMetadataVersions( context );

                log.info( metadataVersions.size() + " metadataVersions found" );

                for ( MetadataVersion metadataVersion : metadataVersions )
                {
                    Pair<String, String> keyJsonValueUidAndValue = getKeyJsonValue( context, metadataVersion );

                    if ( keyJsonValueUidAndValue != null )
                    {
                        String value = keyJsonValueUidAndValue.getValue();
                        if ( value != null && !value.substring( 0, WRAPPED_METADATA_PREFIX.length() ).equals( WRAPPED_METADATA_PREFIX ) )
                        {
                            MetadataWrapper metadataWrapper = new MetadataWrapper( value );

                            //MetadataWrapper is used to avoid Metadata keys reordering by jsonb (jsonb does not preserve keys order)
                            String newWrappedValue = toJsonAsString( metadataWrapper );
                            String newHashCode = getHashCode( newWrappedValue );

                            Pair<String, String> updatedKeyJsonValueUidAndValue = new Pair<>( keyJsonValueUidAndValue.getKey(), newWrappedValue );

                            metadataVersion.setHashCode( newHashCode );

                            updateKeyJsonValue( context, updatedKeyJsonValueUidAndValue );
                            updateMetadataVersion( context, metadataVersion );
                        }
                        else
                        {
                            log.info( "Metadata payload is already wrapped for metadataVersion: " + metadataVersion.getName() );
                        }
                    }
                    else
                    {
                        log.info( "No keyjsonvalue entry (metadata payload) found for metadataVersion: " +
                            metadataVersion.getName() );
                    }
                }
            }
            else
            {
                log.info( "Migration NOT necessary" );
            }
        }
        catch ( SQLException | NoSuchAlgorithmException | JsonProcessingException e )
        {
            log.error( "MetadataVersionWrapperCallback failed.", e );
        }
    }

    private boolean necessaryToRunMigration( final Context context ) throws SQLException
    {
        try ( Statement stm = context.getConnection().createStatement();
        ResultSet rs = stm.executeQuery( VALUE_COLUMN_EXISTS_SQL ))
        {
            rs.next();
            return rs.getInt( "count" ) == 1;
        }
    }

    private List<MetadataVersion> getMetadataVersions( final Context context ) throws SQLException
    {
        List<MetadataVersion> metadataVersions = new ArrayList<>();

        try ( Statement stm = context.getConnection().createStatement(); ResultSet rs = stm
            .executeQuery( GET_ALL_METADATA_VERSIONS_SQL ) )
        {
            while ( rs.next() )
            {
                MetadataVersion metadataVersion = new MetadataVersion();
                metadataVersion.setUid( rs.getString( "uid" ) );
                metadataVersion.setName( rs.getString( "name" ) );
                metadataVersion.setHashCode( rs.getString( "hashcode" ) );

                metadataVersions.add( metadataVersion );
            }
        }

        return metadataVersions;
    }

    private void updateMetadataVersion( final Context context, MetadataVersion metadataVersion ) throws SQLException
    {
        try ( PreparedStatement stm = context.getConnection().prepareStatement( UPDATE_METADATA_VERSION_SQL ) )
        {
            stm.setString( 1, metadataVersion.getHashCode() );
            stm.setString( 2, metadataVersion.getUid() );

            stm.execute();
        }
    }

    private Pair<String, String> getKeyJsonValue( final Context context, MetadataVersion metadataVersion )
        throws SQLException
    {
        List<String> temp = new ArrayList<>();

        try ( PreparedStatement stm = context.getConnection().prepareStatement( GET_KEYJSONVALUE_SQL ) )
        {
            stm.setString( 1, metadataVersion.getName() );

            try ( ResultSet rs = stm.executeQuery() )
            {
                if ( rs.next() )
                {
                    temp.add( rs.getString( "uid" ) );
                    temp.add( rs.getString( "value" ) );
                }
            }
        }

        if ( !temp.isEmpty() )
        {
            return new Pair<>( temp.get( 0 ), temp.get( 1 ) );
        }

        return null;
    }

    private void updateKeyJsonValue( final Context context, Pair<String, String> updatedKeyJsonValueUidAndValue )
        throws SQLException
    {
        try ( PreparedStatement stm = context.getConnection().prepareStatement( UPDATE_KEYJSONVALUE_SQL ) )
        {
            stm.setString( 1, updatedKeyJsonValueUidAndValue.getValue() );
            stm.setString( 2, updatedKeyJsonValueUidAndValue.getKey() );

            stm.execute();
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

        public MetadataWrapper()
        {
        }

        MetadataWrapper( String metadata )
        {
            this.metadata = metadata;
        }

        @JsonProperty( "metadata" ) @JacksonXmlProperty( localName = "metadata", namespace = DxfNamespaces.DXF_2_0 )
        public String getMetadata()
        {
            return metadata;
        }

        public void setMetadata( String metadata )
        {
            this.metadata = metadata;
        }

        @Override public boolean equals( Object o )
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

        @Override public int hashCode()
        {
            return metadata != null ? metadata.hashCode() : 0;
        }

        @Override public java.lang.String toString()
        {
            return "MetadataWrapper{" + "metadata=" + metadata + '}';
        }
    }

    private String toJsonAsString( Object value ) throws JsonProcessingException
    {
            return mapper.writeValueAsString( value );
    }
}
