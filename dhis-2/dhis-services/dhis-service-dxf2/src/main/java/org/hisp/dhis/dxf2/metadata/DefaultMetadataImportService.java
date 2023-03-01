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
package org.hisp.dhis.dxf2.metadata;

import static org.hisp.dhis.dxf2.metadata.objectbundle.EventReportCompatibilityGuard.handleDeprecationIfEventReport;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.MergeMode;
import org.hisp.dhis.commons.timer.SystemTimer;
import org.hisp.dhis.commons.timer.Timer;
import org.hisp.dhis.dxf2.metadata.feedback.ImportReport;
import org.hisp.dhis.dxf2.metadata.feedback.ImportReportMode;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleMode;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleParams;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleService;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleValidationService;
import org.hisp.dhis.dxf2.metadata.objectbundle.feedback.ObjectBundleCommitReport;
import org.hisp.dhis.dxf2.metadata.objectbundle.feedback.ObjectBundleValidationReport;
import org.hisp.dhis.feedback.Status;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.metadata.changelog.MetadataChangelogService;
import org.hisp.dhis.preheat.PreheatIdentifier;
import org.hisp.dhis.preheat.PreheatMode;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.system.notification.NotificationLevel;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.base.Enums;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Slf4j
@RequiredArgsConstructor
@Service( "org.hisp.dhis.dxf2.metadata.MetadataImportService" )
public class DefaultMetadataImportService implements MetadataImportService
{
    private final CurrentUserService currentUserService;

    private final ObjectBundleService objectBundleService;

    private final ObjectBundleValidationService objectBundleValidationService;

    private final IdentifiableObjectManager manager;

    private final AclService aclService;

    private final Notifier notifier;

    private final MetadataChangelogService metadataChangelogService;

    @Override
    @Transactional
    public ImportReport importMetadata( MetadataImportParams params )
    {
        Timer timer = new SystemTimer().start();

        ImportReport importReport = new ImportReport();
        importReport.setImportParams( params );
        importReport.setStatus( Status.OK );

        if ( params.getUser() == null )
        {
            params.setUser( currentUserService.getCurrentUser() );
        }

        if ( params.getUserOverrideMode() == UserOverrideMode.CURRENT )
        {
            params.setOverrideUser( currentUserService.getCurrentUser() );
        }

        String message = "(" + params.getUsername() + ") Import:Start";
        log.info( message );

        if ( params.hasJobId() )
        {
            notifier.notify( params.getId(), message );
        }

        preCreateBundle( params );

        ObjectBundleParams bundleParams = params.toObjectBundleParams();
        handleDeprecationIfEventReport( bundleParams );
        ObjectBundle bundle = objectBundleService.create( bundleParams );

        postCreateBundle( bundle, bundleParams );

        ObjectBundleValidationReport validationReport = objectBundleValidationService.validate( bundle );
        importReport.addTypeReports( validationReport );

        if ( !validationReport.hasErrorReports() || AtomicMode.NONE == bundle.getAtomicMode() )
        {
            Timer commitTimer = new SystemTimer().start();

            ObjectBundleCommitReport commitReport = objectBundleService.commit( bundle );
            importReport.addTypeReports( commitReport );

            if ( importReport.hasErrorReports() )
            {
                importReport.setStatus( Status.WARNING );
            }

            log.info( "(" + bundle.getUsername() + ") Import:Commit took " + commitTimer.toString() );
        }
        else
        {
            importReport.getStats().ignored();
            importReport.getTypeReports().forEach( tr -> tr.getStats().ignored() );

            importReport.setStatus( Status.ERROR );
        }

        message = "(" + bundle.getUsername() + ") Import:Done took " + timer.toString();

        log.info( message );

        if ( bundle.hasJobId() )
        {
            notifier.notify( bundle.getJobId(), NotificationLevel.INFO, message, true )
                .addJobSummary( bundle.getJobId(), importReport, ImportReport.class );
        }

        if ( ObjectBundleMode.VALIDATE == params.getImportMode() )
        {
            return importReport;
        }

        if ( params.hasMetadataChangelog() )
        {
            metadataChangelogService.updateMetadataChangelog( params.getMetadataChangelog().setSuccess( true ) );
        }

        importReport.clean();
        importReport.forEachTypeReport( typeReport -> {
            ImportReportMode mode = params.getImportReportMode();
            if ( ImportReportMode.ERRORS == mode )
            {
                typeReport.clean();
            }
            if ( ImportReportMode.DEBUG != mode )
            {
                typeReport.getObjectReports().forEach( objectReport -> objectReport.setDisplayName( null ) );
            }
        } );

        return importReport;
    }

    @Override
    @Transactional( readOnly = true )
    public MetadataImportParams getParamsFromMap( Map<String, List<String>> parameters )
    {
        MetadataImportParams params = new MetadataImportParams();

        if ( params.getUser() == null )
        {
            params.setUser( currentUserService.getCurrentUser() );
        }

        params.setSkipSharing( getBooleanWithDefault( parameters, "skipSharing", false ) );
        params.setSkipTranslation( getBooleanWithDefault( parameters, "skipTranslation", false ) );
        params.setSkipValidation( getBooleanWithDefault( parameters, "skipValidation", false ) );
        params.setUserOverrideMode(
            getEnumWithDefault( UserOverrideMode.class, parameters, "userOverrideMode", UserOverrideMode.NONE ) );
        params.setImportMode(
            getEnumWithDefault( ObjectBundleMode.class, parameters, "importMode", ObjectBundleMode.COMMIT ) );
        params.setPreheatMode(
            getEnumWithDefault( PreheatMode.class, parameters, "preheatMode", PreheatMode.REFERENCE ) );
        params.setIdentifier(
            getEnumWithDefault( PreheatIdentifier.class, parameters, "identifier", PreheatIdentifier.UID ) );
        params.setImportStrategy( getEnumWithDefault( ImportStrategy.class, parameters, "importStrategy",
            ImportStrategy.CREATE_AND_UPDATE ) );
        params.setAtomicMode( getEnumWithDefault( AtomicMode.class, parameters, "atomicMode", AtomicMode.ALL ) );
        params.setMergeMode( getEnumWithDefault( MergeMode.class, parameters, "mergeMode", MergeMode.REPLACE ) );
        params.setFlushMode( getEnumWithDefault( FlushMode.class, parameters, "flushMode", FlushMode.AUTO ) );
        params.setImportReportMode(
            getEnumWithDefault( ImportReportMode.class, parameters, "importReportMode", ImportReportMode.ERRORS ) );
        params.setFirstRowIsHeader( getBooleanWithDefault( parameters, "firstRowIsHeader", true ) );

        if ( getBooleanWithDefault( parameters, "async", false ) )
        {
            JobConfiguration jobId = new JobConfiguration( "metadataImport", JobType.METADATA_IMPORT,
                params.getUser().getUid(), true );
            notifier.clear( jobId );
            params.setId( jobId );
        }

        if ( params.getUserOverrideMode() == UserOverrideMode.SELECTED )
        {
            User overrideUser = null;

            if ( parameters.containsKey( "overrideUser" ) )
            {
                List<String> overrideUsers = parameters.get( "overrideUser" );
                overrideUser = manager.get( User.class, overrideUsers.get( 0 ) );
            }

            if ( overrideUser == null )
            {
                throw new MetadataImportException(
                    "UserOverrideMode.SELECTED is enabled, but overrideUser parameter does not point to a valid user." );
            }
            else
            {
                params.setOverrideUser( overrideUser );
            }
        }

        return params;
    }

    // -----------------------------------------------------------------------------------
    // Utility Methods
    // -----------------------------------------------------------------------------------

    private boolean getBooleanWithDefault( Map<String, List<String>> parameters, String key, boolean defaultValue )
    {
        if ( parameters == null || parameters.get( key ) == null || parameters.get( key ).isEmpty() )
        {
            return defaultValue;
        }

        String value = String.valueOf( parameters.get( key ).get( 0 ) );

        return "true".equals( value.toLowerCase() );
    }

    private <T extends Enum<T>> T getEnumWithDefault( Class<T> enumKlass, Map<String, List<String>> parameters,
        String key, T defaultValue )
    {
        if ( parameters == null || parameters.get( key ) == null || parameters.get( key ).isEmpty() )
        {
            return defaultValue;
        }

        String value = String.valueOf( parameters.get( key ).get( 0 ) );

        return Enums.getIfPresent( enumKlass, value ).or( defaultValue );
    }

    private void preCreateBundle( MetadataImportParams params )
    {
        if ( params.getUser() == null )
        {
            return;
        }

        for ( Class<? extends IdentifiableObject> klass : params.getObjects().keySet() )
        {
            params.getObjects().get( klass )
                .forEach( o -> preCreateBundleObject( (BaseIdentifiableObject) o, params ) );
        }
    }

    private void preCreateBundleObject( BaseIdentifiableObject object, MetadataImportParams params )
    {
        if ( object.getUserAccesses() == null )
        {
            object.setUserAccesses( new HashSet<>() );
        }

        if ( object.getSharing().getUsers() == null )
        {
            object.getSharing().setDtoUserAccesses( object.getUserAccesses() );
        }

        if ( object.getUserGroupAccesses() == null )
        {
            object.setUserGroupAccesses( new HashSet<>() );
        }

        if ( StringUtils.isEmpty( object.getPublicAccess() ) )
        {
            aclService.resetSharing( object, params.getUser() );
        }

        object.setLastUpdatedBy( params.getUser() );
    }

    private void postCreateBundle( ObjectBundle bundle, ObjectBundleParams params )
    {
        if ( bundle.getUser() == null )
        {
            return;
        }

        bundle.forEach( object -> postCreateBundleObject( object, bundle, params ) );
    }

    private void postCreateBundleObject( IdentifiableObject object, ObjectBundle bundle, ObjectBundleParams params )
    {
        if ( object.getCreatedBy() == null )
        {
            return;
        }

        IdentifiableObject userByReference = bundle.getPreheat().get( params.getPreheatIdentifier(),
            User.class, params.getPreheatIdentifier().getIdentifier( object.getCreatedBy() ) );

        if ( userByReference != null )
        {
            object.setCreatedBy( (User) userByReference );
        }
    }
}
