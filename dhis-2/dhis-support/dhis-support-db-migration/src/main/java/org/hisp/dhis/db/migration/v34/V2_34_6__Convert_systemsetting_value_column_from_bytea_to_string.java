/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.db.migration.v34;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.hisp.dhis.sms.config.ContentType;
import org.hisp.dhis.sms.config.GenericGatewayParameter;
import org.hisp.dhis.sms.config.GenericHttpGatewayConfig;
import org.hisp.dhis.sms.config.GenericHttpGetGatewayConfig;
import org.hisp.dhis.sms.config.SmsConfiguration;
import org.hisp.dhis.sms.config.SmsGatewayConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.SerializationUtils;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author David Katuscak (katuscak.d@gmail.com)
 * @author Ameen Mohamed (ameen@dhis2.org)
 */
@SuppressWarnings( "deprecation" )
public class V2_34_6__Convert_systemsetting_value_column_from_bytea_to_string extends BaseJavaMigration
{
    private static final Logger log = LoggerFactory
        .getLogger( V2_34_6__Convert_systemsetting_value_column_from_bytea_to_string.class );

    private static final String CHECK_SYSTEM_SETTING_VALUE_TYPE_SQL = "SELECT data_type FROM information_schema.columns "
        +
        "WHERE table_name = 'systemsetting' AND column_name = 'value';";

    private static final String SMS_CONFIGURATION_SETTING_NAME = "keySmsSetting";

    // Copied from SimplisticHttpGetGateway.java
    public static final String KEY_TEXT = "text";

    public static final String KEY_RECIPIENT = "recipients";

    @Override
    public void migrate( final Context context )
        throws Exception
    {
        try
        {
            // 1. Check whether migration is needed at all. Maybe it was already
            // applied.
            boolean continueWithMigration = false;
            try ( Statement stmt = context.getConnection().createStatement();
                ResultSet rs = stmt.executeQuery( CHECK_SYSTEM_SETTING_VALUE_TYPE_SQL ); )
            {
                if ( rs.next() && rs.getString( "data_type" ).equals( "bytea" ) )
                {
                    continueWithMigration = true;
                }
            }

            if ( continueWithMigration )
            {
                // 2. Fetch the data to convert if available
                Set<SystemSetting> systemSettingsToConvert = fetchExistingSystemSettings( context );

                // 3. Create a new column of type varchar in systemsetting table
                try ( Statement stmt = context.getConnection().createStatement() )
                {
                    stmt.executeUpdate( "ALTER TABLE systemsetting ADD COLUMN IF NOT EXISTS value_text TEXT" );
                }

                // 4. Fill the new column with transformed data
                fillNewColWithTransformedData( context, systemSettingsToConvert );

                // 5. Delete old byte array column for value in systemsetting
                // table
                try ( Statement stmt = context.getConnection().createStatement() )
                {
                    stmt.executeUpdate( "ALTER TABLE systemsetting DROP COLUMN value" );
                }

                // 6. Rename new value_text column to the name of the recently
                // deleted column
                try ( Statement stmt = context.getConnection().createStatement() )
                {
                    stmt.executeUpdate( "ALTER TABLE systemsetting RENAME COLUMN value_text TO value" );
                }
            }
        }
        catch ( Exception e )
        {
            log.error( "Exception occurred: " + e, e );
            throw e;
        }
    }

    private Set<SystemSetting> fetchExistingSystemSettings( final Context context )
        throws SQLException
    {
        Set<SystemSetting> systemSettingsToConvert = new HashSet<>();

        try ( Statement stmt = context.getConnection().createStatement();
            ResultSet rs = stmt.executeQuery( "select * from systemsetting" ); )
        {
            while ( rs.next() )
            {
                String name = rs.getString( "name" );

                SystemSetting systemSetting = new SystemSetting();
                systemSetting.setId( rs.getLong( "systemsettingid" ) );
                systemSetting.setName( name );

                if ( SMS_CONFIGURATION_SETTING_NAME.equals( name ) )
                {
                    SmsConfiguration smsConfiguration = (SmsConfiguration) SerializationUtils
                        .deserialize( rs.getBytes( "value" ) );
                    updateSmsConfiguration( smsConfiguration );
                    systemSetting.setValue( smsConfiguration );
                }
                else
                {
                    systemSetting.setValue( (Serializable) SerializationUtils.deserialize( rs.getBytes( "value" ) ) );
                }

                systemSettingsToConvert.add( systemSetting );
            }
        }
        return systemSettingsToConvert;
    }

    private void fillNewColWithTransformedData( final Context context, Set<SystemSetting> systemSettingsToConvert )
        throws JsonProcessingException
    {
        ObjectMapper objectMapper = new ObjectMapper();

        ObjectMapper specialObjectMapper = new ObjectMapper();
        specialObjectMapper.setVisibility( specialObjectMapper.getSerializationConfig()
            .getDefaultVisibilityChecker()
            .withFieldVisibility( Visibility.ANY )
            .withGetterVisibility( Visibility.NONE )
            .withSetterVisibility( Visibility.NONE )
            .withCreatorVisibility( Visibility.NONE ) );

        try ( PreparedStatement ps = context.getConnection()
            .prepareStatement( "UPDATE systemsetting SET value_text = ? WHERE systemsettingid = ?" ) )
        {
            for ( SystemSetting systemSetting : systemSettingsToConvert )
            {
                if ( systemSetting.getName().equals( SMS_CONFIGURATION_SETTING_NAME ) )
                {
                    ps.setString( 1, specialObjectMapper.writeValueAsString( systemSetting.getValue() ) );
                }
                else
                {
                    ps.setString( 1, objectMapper.writeValueAsString( systemSetting.getValue() ) );
                }
                ps.setLong( 2, systemSetting.getId() );

                ps.execute();
            }
        }
        catch ( SQLException e )
        {
            log.error( "Flyway java migration error:", e );
            throw new FlywayException( e );
        }
    }

    private void updateSmsConfiguration( SmsConfiguration smsConfiguration )
    {
        if ( smsConfiguration == null )
        {
            return;
        }

        List<SmsGatewayConfig> existingGatewayConfigs = smsConfiguration.getGateways();

        List<SmsGatewayConfig> updatedGatewayConfigs = new ArrayList<>();

        for ( SmsGatewayConfig gatewayConfig : existingGatewayConfigs )
        {
            if ( gatewayConfig instanceof GenericHttpGetGatewayConfig )
            {
                GenericHttpGatewayConfig newGatewayConfig = convertToNewSmsGenericConfig(
                    (GenericHttpGetGatewayConfig) gatewayConfig );
                updatedGatewayConfigs.add( newGatewayConfig );
            }
            else
            {
                updatedGatewayConfigs.add( gatewayConfig );
            }
        }

        smsConfiguration.setGateways( updatedGatewayConfigs );
    }

    private GenericHttpGatewayConfig convertToNewSmsGenericConfig( GenericHttpGetGatewayConfig gatewayConfig )
    {
        GenericHttpGatewayConfig newGatewayConfig = new GenericHttpGatewayConfig();
        newGatewayConfig.setContentType( ContentType.FORM_URL_ENCODED );
        newGatewayConfig.setDefaultGateway( gatewayConfig.isDefaultGateway() );
        newGatewayConfig.setName( gatewayConfig.getName() );
        newGatewayConfig.setParameters( gatewayConfig.getParameters() );
        newGatewayConfig.setPassword( gatewayConfig.getPassword() );
        newGatewayConfig.setUid( gatewayConfig.getUid() );
        newGatewayConfig.setUrlTemplate(
            gatewayConfig.getUrlTemplate() + "?" + createConfigurationTemplateFromConfig( gatewayConfig ) );
        newGatewayConfig.setUseGet( true );
        newGatewayConfig.setSendUrlParameters( true );
        newGatewayConfig.setUsername( gatewayConfig.getUsername() );

        return newGatewayConfig;
    }

    private String createConfigurationTemplateFromConfig( GenericHttpGetGatewayConfig gatewayConfig )
    {
        StringBuilder configTemplateBuilder = new StringBuilder();
        if ( !StringUtils.isEmpty( gatewayConfig.getMessageParameter() ) )
        {
            configTemplateBuilder.append( gatewayConfig.getMessageParameter() ).append( "={" ).append( KEY_TEXT )
                .append( "}&" );
        }

        if ( !StringUtils.isEmpty( gatewayConfig.getRecipientParameter() ) )
        {
            configTemplateBuilder.append( gatewayConfig.getRecipientParameter() ).append( "={" ).append( KEY_RECIPIENT )
                .append( "}&" );
        }

        for ( GenericGatewayParameter parameter : gatewayConfig.getParameters() )
        {
            if ( !parameter.isHeader() )
            {
                configTemplateBuilder.append( parameter.getKey() ).append( "={" ).append( parameter.getKey() )
                    .append( "}&" );
            }
        }
        return configTemplateBuilder.toString();
    }

    public class SystemSetting
        implements Serializable
    {
        private long id;

        private String name;

        private Serializable value;

        // -------------------------------------------------------------------------
        // Constructor
        // -------------------------------------------------------------------------

        public SystemSetting()
        {
        }

        // -------------------------------------------------------------------------
        // Logic
        // -------------------------------------------------------------------------

        public boolean hasValue()
        {
            return value != null;
        }

        // -------------------------------------------------------------------------
        // Getters and setters
        // -------------------------------------------------------------------------

        public long getId()
        {
            return id;
        }

        public void setId( long id )
        {
            this.id = id;
        }

        public String getName()
        {
            return name;
        }

        public void setName( String name )
        {
            this.name = name;
        }

        public Serializable getValue()
        {
            return value;
        }

        public void setValue( Serializable value )
        {
            this.value = value;
        }

        // -------------------------------------------------------------------------
        // hashCode and equals
        // -------------------------------------------------------------------------

        @Override
        public boolean equals( Object o )
        {
            if ( this == o )
            {
                return true;
            }

            if ( o == null )
            {
                return false;
            }

            if ( !(o instanceof SystemSetting) )
            {
                return false;
            }

            final SystemSetting other = (SystemSetting) o;

            return name.equals( other.getName() );
        }

        @Override
        public int hashCode()
        {
            int prime = 31;
            int result = 1;

            result = result * prime + name.hashCode();

            return result;
        }
    }
}
