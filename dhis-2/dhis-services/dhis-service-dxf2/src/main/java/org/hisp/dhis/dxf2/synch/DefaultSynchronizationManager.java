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
package org.hisp.dhis.dxf2.synch;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.util.Date;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.dxf2.datavalueset.DataValueSetService;
import org.hisp.dhis.dxf2.importsummary.ImportConflicts;
import org.hisp.dhis.dxf2.importsummary.ImportCount;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.dxf2.metadata.AtomicMode;
import org.hisp.dhis.dxf2.metadata.Metadata;
import org.hisp.dhis.dxf2.metadata.MetadataImportParams;
import org.hisp.dhis.dxf2.metadata.MetadataImportService;
import org.hisp.dhis.dxf2.metadata.feedback.ImportReport;
import org.hisp.dhis.dxf2.sync.SyncEndpoint;
import org.hisp.dhis.dxf2.sync.SyncUtils;
import org.hisp.dhis.dxf2.webmessage.AbstractWebMessageResponse;
import org.hisp.dhis.dxf2.webmessage.WebMessageParseException;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.util.CodecUtils;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Lars Helge Overland
 */
@Slf4j
@Component( "org.hisp.dhis.dxf2.synch.SynchronizationManager" )
public class DefaultSynchronizationManager
    implements SynchronizationManager
{
    private static final String HEADER_AUTHORIZATION = "Authorization";

    private final DataValueSetService dataValueSetService;

    private final DataValueService dataValueService;

    private final MetadataImportService importService;

    private final SchemaService schemaService;

    private final CurrentUserService currentUserService;

    private final SystemSettingManager systemSettingManager;

    private final RestTemplate restTemplate;

    private final ObjectMapper jsonMapper;

    public DefaultSynchronizationManager(
        DataValueSetService dataValueSetService,
        DataValueService dataValueService,
        MetadataImportService importService,
        SchemaService schemaService,
        CurrentUserService currentUserService,
        SystemSettingManager systemSettingManager,
        RestTemplate restTemplate,
        ObjectMapper jsonMapper )
    {
        checkNotNull( dataValueSetService );
        checkNotNull( dataValueService );
        checkNotNull( importService );
        checkNotNull( schemaService );
        checkNotNull( currentUserService );
        checkNotNull( systemSettingManager );
        checkNotNull( restTemplate );
        checkNotNull( jsonMapper );

        this.dataValueSetService = dataValueSetService;
        this.dataValueService = dataValueService;
        this.importService = importService;
        this.schemaService = schemaService;
        this.currentUserService = currentUserService;
        this.systemSettingManager = systemSettingManager;
        this.restTemplate = restTemplate;
        this.jsonMapper = jsonMapper;
    }

    // -------------------------------------------------------------------------
    // SynchronizationManager implementation
    // -------------------------------------------------------------------------

    @Override
    public AvailabilityStatus isRemoteServerAvailable()
    {
        return SyncUtils.isRemoteServerAvailable( systemSettingManager, restTemplate );
    }

    @Override
    public ImportConflicts executeDataValuePush()
        throws WebMessageParseException
    {
        AvailabilityStatus availability = isRemoteServerAvailable();

        if ( !availability.isAvailable() )
        {
            log.info( "Aborting data values push, server not available" );
            return null;
        }

        String url = systemSettingManager.getStringSetting( SettingKey.REMOTE_INSTANCE_URL )
            + SyncEndpoint.DATA_VALUE_SETS.getPath();
        String username = systemSettingManager.getStringSetting( SettingKey.REMOTE_INSTANCE_USERNAME );
        String password = systemSettingManager.getStringSetting( SettingKey.REMOTE_INSTANCE_PASSWORD );

        SystemInstance instance = new SystemInstance( url, username, password );

        return executeDataValuePush( instance );
    }

    /**
     * Executes a push of data values to the given remote instance.
     *
     * @param instance the remote system instance.
     * @return an ImportSummary.
     */
    private ImportConflicts executeDataValuePush( SystemInstance instance )
        throws WebMessageParseException
    {
        // ---------------------------------------------------------------------
        // Set time for last success to start of process to make data saved
        // subsequently part of next synch process without being ignored
        // ---------------------------------------------------------------------
        final Date startTime = new Date();
        final Date lastSuccessTime = SyncUtils.getLastSyncSuccess( systemSettingManager,
            SettingKey.LAST_SUCCESSFUL_DATA_VALUE_SYNC );
        final Date skipChangedBefore = systemSettingManager
            .getDateSetting( SettingKey.SKIP_SYNCHRONIZATION_FOR_DATA_CHANGED_BEFORE );
        final Date lastUpdatedAfter = lastSuccessTime.after( skipChangedBefore ) ? lastSuccessTime : skipChangedBefore;
        final int objectsToSynchronize = dataValueService.getDataValueCountLastUpdatedAfter( lastUpdatedAfter, true );

        log.info( "DataValues last changed before " + skipChangedBefore + " will not be synchronized." );

        if ( objectsToSynchronize == 0 )
        {
            SyncUtils.setLastSyncSuccess( systemSettingManager, SettingKey.LAST_SUCCESSFUL_DATA_VALUE_SYNC,
                startTime );
            log.debug( "Skipping data values push, no new or updated data values" );

            ImportCount importCount = new ImportCount( 0, 0, 0, 0 );
            return new ImportSummary( ImportStatus.SUCCESS, "No new or updated data values to push.", importCount );
        }

        log.info( "Data Values: " + objectsToSynchronize + " to push since last synchronization success: "
            + lastSuccessTime );
        log.info( "Remote server POST URL: " + instance.getUrl() );

        final RequestCallback requestCallback = request -> {
            request.getHeaders().setContentType( MediaType.APPLICATION_JSON );
            request.getHeaders().add( HEADER_AUTHORIZATION,
                CodecUtils.getBasicAuthString( instance.getUsername(), instance.getPassword() ) );

            dataValueSetService
                .exportDataValueSetJson( lastUpdatedAfter, request.getBody(), new IdSchemes() );
        };

        final int maxSyncAttempts = systemSettingManager.getIntSetting( SettingKey.MAX_SYNC_ATTEMPTS );

        Optional<AbstractWebMessageResponse> responseSummary = SyncUtils.runSyncRequest(
            restTemplate,
            requestCallback,
            SyncEndpoint.DATA_VALUE_SETS.getKlass(),
            instance.getUrl(),
            maxSyncAttempts );

        ImportSummary summary = null;
        if ( responseSummary.isPresent() )
        {
            summary = (ImportSummary) responseSummary.get();

            if ( ImportStatus.SUCCESS.equals( summary.getStatus() ) )
            {
                log.info( "Push successful: " + summary );
            }
            else
            {
                log.warn( "Push failed: " + summary );
            }
        }

        return summary;
    }

    @Override
    public ImportReport executeMetadataPull( String url )
    {
        User user = currentUserService.getCurrentUser();

        String userUid = user != null ? user.getUid() : null;

        log.info( String.format( "Metadata pull, url: %s, user: %s", url, userUid ) );

        String json = restTemplate.getForObject( url, String.class );

        Metadata metadata = null;

        try
        {
            metadata = jsonMapper.readValue( json, Metadata.class );
        }
        catch ( IOException ex )
        {
            throw new RuntimeException( "Failed to parse remote JSON document", ex );
        }

        MetadataImportParams importParams = new MetadataImportParams();
        importParams.setSkipSharing( true );
        importParams.setAtomicMode( AtomicMode.NONE );
        importParams.addMetadata( schemaService.getMetadataSchemas(), metadata );

        return importService.importMetadata( importParams );
    }
}
