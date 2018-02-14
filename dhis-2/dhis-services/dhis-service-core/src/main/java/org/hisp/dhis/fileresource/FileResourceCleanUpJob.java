package org.hisp.dhis.fileresource;

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

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.scheduling.AbstractJob;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.datavalue.DataValueAuditService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

/**
 * Deletes any orphaned FileResources. Queries for non-assigned or failed-upload
 * FileResources and deletes them from the database and/or file store.
 *
 * @author Halvdan Hoem Grelland
 */
public class FileResourceCleanUpJob
    extends AbstractJob
{
    private static final Log log = LogFactory.getLog( FileResourceCleanUpJob.class );

    @Autowired
    private FileResourceService fileResourceService;

    @Autowired
    private DataValueAuditService dataValueAuditService;

    @Autowired
    private SystemSettingManager systemSettingManager;

    @Autowired
    private DataElementService dataElementService;

    // -------------------------------------------------------------------------
    // Implementation
    // -------------------------------------------------------------------------

    @Override
    public JobType getJobType()
    {
        return JobType.FILE_RESOURCE_CLEANUP;
    }

    @Override
    public void execute( JobConfiguration jobConfiguration )
    {
        FileResourceRetentionStrategy retentionStrategy = (FileResourceRetentionStrategy) systemSettingManager.getSystemSetting(SettingKey.FILE_RESOURCE_RETENTION_STRATEGY);

        List<Pair<String, String>> deletedOrphans = new ArrayList<>();

        List<Pair<String, String>> deletedAuditFiles = new ArrayList<>();

        fileResourceService.getOrphanedFileResources()
            .forEach( fr -> {
                deletedOrphans.add( ImmutablePair.of( fr.getName(), fr.getUid() ) );
                fileResourceService.deleteFileResource( fr.getUid() );
            } );

        if ( retentionStrategy != FileResourceRetentionStrategy.FOREVER )
        {
            deletedAuditFiles = getExpiredFileResources( retentionStrategy );

            deletedAuditFiles.forEach( pair -> fileResourceService.deleteFileResource( pair.getRight() ) );
        }

        if ( !deletedOrphans.isEmpty() )
        {
            log.warn( String.format( "Deleted %d orphaned FileResources: %s", deletedOrphans.size(), prettyPrint( deletedOrphans ) ) );
        }

        if ( !deletedAuditFiles.isEmpty() )
        {
            log.warn( String.format( "Deleted %d expired FileResource audits: %s", deletedAuditFiles.size(), prettyPrint( deletedAuditFiles ) ) );
        }
    }

    private List<Pair<String, String>> getExpiredFileResources( FileResourceRetentionStrategy retentionStrategy )
    {
        List<Pair<String, String>> expiredFileResources = new ArrayList<>();

        List<DataElement> elements = dataElementService.getAllDataElementsByValueType( ValueType.FILE_RESOURCE );

        dataValueAuditService.getDataValueAudits( elements, new ArrayList<Period>(),
            new ArrayList<OrganisationUnit>(), null, null, null ).stream()
            .filter( audit -> new DateTime( audit.getCreated() ).plus( retentionStrategy.getRetentionTime() ).isBefore( DateTime.now() ) )
            .map( audit -> fileResourceService.getFileResource( audit.getValue() ) )
            .filter( fr -> fr != null )
            .forEach( fr -> expiredFileResources.add( ImmutablePair.of( fr.getName(), fr.getUid() ) ) );

        return expiredFileResources;
    }

    private String prettyPrint( List<Pair<String, String>> list )
    {
        if ( list.isEmpty() )
        {
            return "";
        }

        StringBuilder sb = new StringBuilder( "[ " );

        list.forEach(
            pair -> sb.append( pair.getLeft() ).append( " , uid: " ).append( pair.getRight() ).append( ", " ) );

        sb.deleteCharAt( sb.lastIndexOf( "," ) ).append( "]" );

        return sb.toString();
    }

}
