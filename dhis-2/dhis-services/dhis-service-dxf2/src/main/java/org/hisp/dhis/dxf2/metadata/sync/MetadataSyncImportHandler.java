package org.hisp.dhis.dxf2.metadata.sync;

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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dxf2.metadata.sync.exception.MetadataSyncImportException;
import org.hisp.dhis.feedback.Status;
import org.hisp.dhis.dxf2.metadata.sync.exception.MetadataSyncServiceException;
import org.hisp.dhis.dxf2.metadata.version.MetadataVersionDelegate;
import org.hisp.dhis.dxf2.metadata.version.exception.MetadataVersionServiceException;
import org.hisp.dhis.dxf2.metadata.MetadataImportParams;
import org.hisp.dhis.dxf2.metadata.MetadataImportService;
import org.hisp.dhis.dxf2.metadata.feedback.ImportReport;
import org.hisp.dhis.metadata.version.MetadataVersion;
import org.hisp.dhis.metadata.version.VersionType;
import org.hisp.dhis.render.RenderFormat;
import org.hisp.dhis.render.RenderService;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Import handler for metadata sync service
 *
 * @author anilkumk
 */
public class MetadataSyncImportHandler
{
    private static final Log log = LogFactory.getLog( MetadataSyncImportHandler.class );

    @Autowired
    private MetadataVersionDelegate metadataVersionDelegate;

    @Autowired
    private RenderService renderService;

    @Autowired
    private MetadataImportService metadataImportService;

    public MetadataSyncSummary importMetadata( MetadataSyncParams syncParams, String versionSnapShot )
    {
        MetadataVersion version = getMetadataVersion( syncParams );
        MetadataImportParams importParams = syncParams.getImportParams();
        MetadataSyncSummary metadataSyncSummary = new MetadataSyncSummary();

        if ( importParams == null )
        {
            throw new MetadataSyncServiceException( "MetadataImportParams for the Sync cant be null." );
        }

        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> classListMap = parseClassListMap(
            versionSnapShot );

        if ( classListMap == null )
        {
            throw new MetadataSyncServiceException( "ClassListMap can't be null" );
        }

        importParams.setObjects( classListMap );

        ImportReport importReport = null;

        try
        {
            importReport = metadataImportService.importMetadata( importParams );

        }
        catch ( Exception e )
        {
            String message = "Exception occurred while trying to import the metadata. " + e.getMessage();
            log.error( message,e );
            throw new MetadataSyncImportException( message,e );
        }
        
        boolean addNewVersion = handleImportReport( importReport, version );

        if ( addNewVersion )
        {
            try
            {
                metadataVersionDelegate.addNewMetadataVersion( version );
            }
            catch ( MetadataVersionServiceException e )
            {
                throw new MetadataSyncServiceException( e.getMessage(), e );
            }

        }

        metadataSyncSummary.setImportReport( importReport );
        metadataSyncSummary.setMetadataVersion( version );

        return metadataSyncSummary;
    }

    //----------------------------------------------------------------------------------------
    // Private Methods
    //----------------------------------------------------------------------------------------

    private boolean handleImportReport( ImportReport importReport, MetadataVersion version )
    {
        if ( importReport == null )
        {
            return false;
        }
        
        Status importStatus = importReport.getStatus();
        return importStatus.equals( Status.OK ) || isBestEffort( version, importStatus );
    }

    private boolean isBestEffort( MetadataVersion version, Status importStatus )
    {
        return importStatus.equals( Status.WARNING ) && VersionType.BEST_EFFORT.equals( version.getType() );
    }

    private Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> parseClassListMap(
        String metadataVersionSnapshot )
    {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
            metadataVersionSnapshot.getBytes( StandardCharsets.UTF_8 ) );

        try
        {
            return renderService.fromMetadata( byteArrayInputStream, RenderFormat.JSON );
        }
        catch ( IOException ex )
        {
            String message = "Exception occurred while trying to do JSON conversion while parsing class list map";
            log.error( message );
            throw new MetadataSyncServiceException( message, ex );
        }
        catch ( Exception ex )
        {
            throw new MetadataSyncServiceException( ex.getMessage(), ex );
        }
    }

    private MetadataVersion getMetadataVersion( MetadataSyncParams syncParams )
    {
        if ( syncParams == null )
        {
            throw new MetadataSyncServiceException( "MetadataSyncParams cant be null" );
        }

        MetadataVersion version = syncParams.getVersion();

        if ( version == null )
        {
            throw new MetadataSyncServiceException( "MetadataVersion for the Sync cant be null." );
        }

        return version;
    }
}
