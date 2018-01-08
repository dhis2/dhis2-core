package org.hisp.dhis.dxf2.metadata;

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

import com.google.api.client.util.Lists;
import com.google.common.base.Enums;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import org.hisp.dhis.feedback.TypeReport;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.preheat.PreheatIdentifier;
import org.hisp.dhis.preheat.PreheatMode;
import org.hisp.dhis.scheduling.JobId;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.system.notification.NotificationLevel;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserAccess;
import org.hisp.dhis.user.UserGroupAccess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Component
@Transactional
public class DefaultMetadataImportService implements MetadataImportService
{
    private static final Log log = LogFactory.getLog( DefaultMetadataImportService.class );

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private ObjectBundleService objectBundleService;

    @Autowired
    private ObjectBundleValidationService objectBundleValidationService;

    @Autowired
    private IdentifiableObjectManager manager;

    @Autowired
    private AclService aclService;

    @Autowired
    private Notifier notifier;

    @Autowired
    private IdentifiableObjectManager objectManager;

    @Override
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
            notifier.notify( params.getJobId(), message );
        }

        ObjectBundleParams bundleParams = params.toObjectBundleParams();
        ObjectBundle bundle = objectBundleService.create( bundleParams );

        prepareBundle( bundle, bundleParams );

        ObjectBundleValidationReport validationReport = objectBundleValidationService.validate( bundle );
        importReport.addTypeReports( validationReport.getTypeReportMap() );

        if ( !(!validationReport.getErrorReports().isEmpty() && AtomicMode.ALL == bundle.getAtomicMode()) )
        {
            Timer commitTimer = new SystemTimer().start();

            ObjectBundleCommitReport commitReport = objectBundleService.commit( bundle );
            importReport.addTypeReports( commitReport.getTypeReportMap() );

            if ( !importReport.getErrorReports().isEmpty() )
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
                .addTaskSummary( bundle.getJobId(), importReport );
        }

        if ( ObjectBundleMode.VALIDATE == params.getImportMode() )
        {
            return importReport;
        }

        Lists.newArrayList( importReport.getTypeReportMap().keySet() ).forEach( typeReportKey ->
        {
            if ( importReport.getTypeReportMap().get( typeReportKey ).getStats().getTotal() == 0 )
            {
                importReport.getTypeReportMap().remove( typeReportKey );
                return;
            }

            TypeReport typeReport = importReport.getTypeReportMap().get( typeReportKey );

            if ( ImportReportMode.ERRORS == params.getImportReportMode() )
            {
                Lists.newArrayList( typeReport.getObjectReportMap().keySet() ).forEach( objectReportKey ->
                {
                    if ( typeReport.getObjectReportMap().get( objectReportKey ).getErrorReportsByCode().isEmpty() )
                    {
                        typeReport.getObjectReportMap().remove( objectReportKey );
                    }
                } );
            }

            if ( ImportReportMode.DEBUG != params.getImportReportMode() )
            {
                typeReport.getObjectReports().forEach( objectReport -> objectReport.setDisplayName( null ) );
            }
        } );

        return importReport;
    }

    @Override
    public MetadataImportParams getParamsFromMap( Map<String, List<String>> parameters )
    {
        MetadataImportParams params = new MetadataImportParams();

        if ( params.getUser() == null )
        {
            params.setUser( currentUserService.getCurrentUser() );
        }

        params.setSkipSharing( getBooleanWithDefault( parameters, "skipSharing", false ) );
        params.setSkipValidation( getBooleanWithDefault( parameters, "skipValidation", false ) );
        params.setUserOverrideMode( getEnumWithDefault( UserOverrideMode.class, parameters, "userOverrideMode", UserOverrideMode.NONE ) );
        params.setImportMode( getEnumWithDefault( ObjectBundleMode.class, parameters, "importMode", ObjectBundleMode.COMMIT ) );
        params.setPreheatMode( getEnumWithDefault( PreheatMode.class, parameters, "preheatMode", PreheatMode.REFERENCE ) );
        params.setIdentifier( getEnumWithDefault( PreheatIdentifier.class, parameters, "identifier", PreheatIdentifier.UID ) );
        params.setImportStrategy( getEnumWithDefault( ImportStrategy.class, parameters, "importStrategy", ImportStrategy.CREATE_AND_UPDATE ) );
        params.setAtomicMode( getEnumWithDefault( AtomicMode.class, parameters, "atomicMode", AtomicMode.ALL ) );
        params.setMergeMode( getEnumWithDefault( MergeMode.class, parameters, "mergeMode", MergeMode.REPLACE ) );
        params.setFlushMode( getEnumWithDefault( FlushMode.class, parameters, "flushMode", FlushMode.AUTO ) );
        params.setImportReportMode( getEnumWithDefault( ImportReportMode.class, parameters, "importReportMode", ImportReportMode.ERRORS ) );

        if ( getBooleanWithDefault( parameters, "async", false ) )
        {
            JobId jobId = new JobId( JobType.METADATA_IMPORT, params.getUser().getUid() );
            notifier.clear( jobId );
            params.setJobId( jobId );
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
                throw new MetadataImportException( "UserOverrideMode.SELECTED is enabled, but overrideUser parameter does not point to a valid user." );
            }
        }

        return params;
    }

    //-----------------------------------------------------------------------------------
    // Utility Methods
    //-----------------------------------------------------------------------------------

    private boolean getBooleanWithDefault( Map<String, List<String>> parameters, String key, boolean defaultValue )
    {
        if ( parameters == null || parameters.get( key ) == null || parameters.get( key ).isEmpty() )
        {
            return defaultValue;
        }

        String value = String.valueOf( parameters.get( key ).get( 0 ) );

        return "true".equals( value.toLowerCase() );
    }

    private <T extends Enum<T>> T getEnumWithDefault( Class<T> enumKlass, Map<String, List<String>> parameters, String key, T defaultValue )
    {
        if ( parameters == null || parameters.get( key ) == null || parameters.get( key ).isEmpty() )
        {
            return defaultValue;
        }

        String value = String.valueOf( parameters.get( key ).get( 0 ) );

        return Enums.getIfPresent( enumKlass, value ).or( defaultValue );
    }

    private void prepareBundle( ObjectBundle bundle, ObjectBundleParams params )
    {
        if ( bundle.getUser() == null )
        {
            return;
        }

        for ( Class<? extends IdentifiableObject> klass : bundle.getObjectMap().keySet() )
        {
            bundle.getObjectMap().get( klass ).forEach( o -> prepareObject( (BaseIdentifiableObject) o, bundle, params ) );
        }
    }

    private void prepareObject( BaseIdentifiableObject object, ObjectBundle bundle, ObjectBundleParams params )
    {
        if ( StringUtils.isEmpty( object.getPublicAccess() ) )
        {
            aclService.resetSharing( object, bundle.getUser() );
        }

        if ( object.getUser() == null || objectManager.get( object.getUser().getUid() ) == null )
        {
            object.setUser( bundle.getUser() );
        }

        object.setUserAccesses( prepareUserAccess( object ) );

        object.setUserGroupAccesses( prepareUserGroupAccesses( object ) );

        object.setLastUpdatedBy( bundle.getUser() );
    }

    private Set<UserAccess> prepareUserAccess( BaseIdentifiableObject object )
    {
        Set<UserAccess> userAccesses = object.getUserAccesses();

        if ( userAccesses == null )
        {
            return new HashSet<>();
        }

        Set<UserAccess> returnUserAccess = new HashSet<>();

        userAccesses.forEach( ua -> {
            UserAccess persisted = objectManager.get( ua.getUid() );

            if ( persisted != null )
            {
                returnUserAccess.add( persisted );
            }
        } );

        return returnUserAccess;
    }

    private Set<UserGroupAccess> prepareUserGroupAccesses( BaseIdentifiableObject object )
    {
        Set<UserGroupAccess> userGroupAccesses = object.getUserGroupAccesses();

        if ( userGroupAccesses == null )
        {
            return new HashSet<>();
        }

        Set<UserGroupAccess> returnUserGroupAccess = new HashSet<>();

        userGroupAccesses.forEach( uga -> {
            UserGroupAccess persisted = objectManager.get( uga.getUid() );

            if ( persisted != null )
            {
                returnUserGroupAccess.add( persisted );
            }
        } );

        return returnUserGroupAccess;
    }
}
