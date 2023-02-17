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
package org.hisp.dhis.sms.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.common.CodeGenerator;
import org.jasypt.encryption.pbe.PBEStringEncryptor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * @author Zubair <rajazubair.asghar@gmail.com>
 */
@Slf4j
@RequiredArgsConstructor
@Service( "org.hisp.dhis.sms.config.GatewayAdministrationService" )
public class DefaultGatewayAdministrationService
    implements GatewayAdministrationService
{
    private AtomicBoolean hasGateways = null;

    private final SmsConfigurationManager smsConfigurationManager;

    @Qualifier( "tripleDesStringEncryptor" )
    private final PBEStringEncryptor pbeStringEncryptor;

    // -------------------------------------------------------------------------
    // GatewayAdministrationService implementation
    // -------------------------------------------------------------------------

    @EventListener
    public void handleContextRefresh( ContextRefreshedEvent contextRefreshedEvent )
    {
        initState();
        updateHasGatewaysState();
    }

    public synchronized void initState()
    {
        if ( hasGateways == null )
        {
            hasGateways = new AtomicBoolean();
        }
    }

    @Override
    public void setDefaultGateway( SmsGatewayConfig config )
    {
        SmsConfiguration configuration = getSmsConfiguration();

        configuration.getGateways()
            .forEach( gateway -> gateway.setDefault( Objects.equals( gateway.getUid(), config.getUid() ) ) );

        smsConfigurationManager.updateSmsConfiguration( configuration );
        updateHasGatewaysState();
    }

    @Override
    public boolean addGateway( SmsGatewayConfig config )
    {
        initState();

        if ( config == null )
        {
            return false;
        }
        SmsGatewayConfig persisted = smsConfigurationManager.checkInstanceOfGateway( config.getClass() );

        if ( persisted != null )
        {
            return true;
        }

        config.setUid( CodeGenerator.generateCode( 10 ) );

        SmsConfiguration smsConfiguration = getSmsConfiguration();

        config.setDefault( smsConfiguration.getGateways().isEmpty() );

        if ( config instanceof GenericHttpGatewayConfig )
        {
            ((GenericHttpGatewayConfig) config).getParameters().stream()
                .filter( GenericGatewayParameter::isConfidential )
                .forEach( p -> p.setValue( pbeStringEncryptor.encrypt( p.getValue() ) ) );
        }

        config.setPassword( pbeStringEncryptor.encrypt( config.getPassword() ) );

        smsConfiguration.getGateways().add( config );

        smsConfigurationManager.updateSmsConfiguration( smsConfiguration );
        updateHasGatewaysState();

        return true;
    }

    @Override
    public void updateGateway( SmsGatewayConfig persistedConfig, SmsGatewayConfig updatedConfig )
    {
        initState();

        if ( persistedConfig == null || updatedConfig == null )
        {
            log.warn( "Gateway configurations cannot be null" );
            return;
        }

        updatedConfig.setUid( persistedConfig.getUid() );
        updatedConfig.setDefault( persistedConfig.isDefault() );

        if ( persistedConfig.getPassword() != null
            && !persistedConfig.getPassword().equals( updatedConfig.getPassword() ) )
        {
            updatedConfig.setPassword( pbeStringEncryptor.encrypt( updatedConfig.getPassword() ) );
        }

        if ( persistedConfig instanceof GenericHttpGatewayConfig )
        {
            List<GenericGatewayParameter> newList = new ArrayList<>();

            GenericHttpGatewayConfig persistedGenericConfig = (GenericHttpGatewayConfig) persistedConfig;
            GenericHttpGatewayConfig updatedGenericConfig = (GenericHttpGatewayConfig) updatedConfig;

            List<GenericGatewayParameter> persistedList = persistedGenericConfig.getParameters()
                .stream().filter( GenericGatewayParameter::isConfidential )
                .collect( Collectors.toList() );

            List<GenericGatewayParameter> updatedList = updatedGenericConfig.getParameters()
                .stream().filter( GenericGatewayParameter::isConfidential )
                .collect( Collectors.toList() );

            for ( GenericGatewayParameter p : updatedList )
            {
                if ( !isPresent( persistedList, p ) )
                {
                    p.setValue( pbeStringEncryptor.encrypt( p.getValue() ) );
                }

                newList.add( p );
            }

            updatedGenericConfig.setParameters( Stream
                .concat( updatedGenericConfig.getParameters().stream(), newList.stream() )
                .distinct().collect( Collectors.toList() ) );

            updatedConfig = updatedGenericConfig;
        }

        SmsConfiguration configuration = getSmsConfiguration();

        configuration.getGateways().remove( persistedConfig );
        configuration.getGateways().add( updatedConfig );

        smsConfigurationManager.updateSmsConfiguration( configuration );
        updateHasGatewaysState();
    }

    @Override
    public boolean removeGatewayByUid( String uid )
    {
        SmsConfiguration smsConfiguration = getSmsConfiguration();

        List<SmsGatewayConfig> gateways = smsConfiguration.getGateways();
        SmsGatewayConfig removed = gateways.stream()
            .filter( gateway -> gateway.getUid().equals( uid ) )
            .findFirst().orElse( null );
        if ( removed == null )
        {
            return false;
        }
        gateways.remove( removed );
        if ( removed.isDefault() && !gateways.isEmpty() )
        {
            gateways.get( 0 ).setDefault( true );
        }
        smsConfigurationManager.updateSmsConfiguration( smsConfiguration );
        updateHasGatewaysState();
        return true;
    }

    @Override
    public SmsGatewayConfig getByUid( String uid )
    {
        return getSmsConfiguration().getGateways().stream()
            .filter( gw -> gw.getUid().equals( uid ) )
            .findFirst().orElse( null );
    }

    @Override
    public SmsGatewayConfig getDefaultGateway()
    {
        return getSmsConfiguration().getGateways().stream()
            .filter( SmsGatewayConfig::isDefault )
            .findFirst().orElse( null );
    }

    @Override
    public boolean hasDefaultGateway()
    {
        initState();

        return getDefaultGateway() != null;
    }

    @Override
    public boolean hasGateways()
    {
        initState();

        return hasGateways.get();
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private SmsConfiguration getSmsConfiguration()
    {
        initState();

        SmsConfiguration smsConfiguration = smsConfigurationManager.getSmsConfiguration();

        if ( smsConfiguration != null )
        {
            return smsConfiguration;
        }

        return new SmsConfiguration();
    }

    private void updateHasGatewaysState()
    {
        SmsConfiguration smsConfiguration = smsConfigurationManager.getSmsConfiguration();

        if ( smsConfiguration == null )
        {
            log.info( "SMS configuration not found" );
            hasGateways.set( false );
            return;
        }

        List<SmsGatewayConfig> gatewayList = smsConfiguration.getGateways();

        if ( gatewayList == null || gatewayList.isEmpty() )
        {
            log.info( "No Gateway configuration found" );

            hasGateways.set( false );
            return;
        }

        log.info( "Gateway configuration found: " + gatewayList );

        hasGateways.set( true );
    }

    private boolean isPresent( List<GenericGatewayParameter> parameters, GenericGatewayParameter parameter )
    {
        return parameters.stream().anyMatch( p -> p.equals( parameter ) );
    }
}
