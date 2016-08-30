package org.hisp.dhis.dxf2.metadata;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import com.google.common.base.Enums;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.common.MergeMode;
import org.hisp.dhis.commons.timer.SystemTimer;
import org.hisp.dhis.commons.timer.Timer;
import org.hisp.dhis.dxf2.common.Status;
import org.hisp.dhis.dxf2.metadata.feedback.ImportReport;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleMode;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleParams;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleService;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleValidationService;
import org.hisp.dhis.dxf2.metadata.objectbundle.feedback.ObjectBundleCommitReport;
import org.hisp.dhis.dxf2.metadata.objectbundle.feedback.ObjectBundleValidationReport;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.preheat.PreheatIdentifier;
import org.hisp.dhis.preheat.PreheatMode;
import org.hisp.dhis.system.notification.NotificationLevel;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.user.CurrentUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

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
    private Notifier notifier;

    @Override
    public ImportReport importMetadata( MetadataImportParams params )
    {
        Timer timer = new SystemTimer().start();

        ImportReport report = new ImportReport();
        report.setStatus( Status.OK );

        if ( params.getUser() == null )
        {
            params.setUser( currentUserService.getCurrentUser() );
        }

        String message = "(" + params.getUsername() + ") Import:Start";
        log.info( message );

        if ( params.hasTaskId() )
        {
            notifier.notify( params.getTaskId(), message );
        }

        ObjectBundleParams bundleParams = params.toObjectBundleParams();
        ObjectBundle bundle = objectBundleService.create( bundleParams );

        ObjectBundleValidationReport validationReport = objectBundleValidationService.validate( bundle );
        report.addTypeReports( validationReport.getTypeReportMap() );

        if ( !(!validationReport.getErrorReports().isEmpty() && AtomicMode.ALL == bundle.getAtomicMode()) )
        {
            Timer commitTimer = new SystemTimer().start();

            ObjectBundleCommitReport commitReport = objectBundleService.commit( bundle );
            report.addTypeReports( commitReport.getTypeReportMap() );

            if ( !report.getErrorReports().isEmpty() )
            {
                report.setStatus( Status.WARNING );
            }

            log.info( "(" + bundle.getUsername() + ") Import:Commit took " + commitTimer.toString() );
        }
        else
        {
            report.setStatus( Status.ERROR );
        }

        message = "(" + bundle.getUsername() + ") Import:Done took " + timer.toString();

        log.info( message );

        if ( bundle.hasTaskId() )
        {
            notifier.notify( bundle.getTaskId(), NotificationLevel.INFO, message, true )
                .addTaskSummary( bundle.getTaskId(), report );
        }


        return report;
    }

    @Override
    public MetadataImportParams getParamsFromMap( Map<String, List<String>> parameters )
    {
        MetadataImportParams params = new MetadataImportParams();
        params.setSkipSharing( getBooleanWithDefault( parameters, "skipSharing", false ) );
        params.setSkipValidation( getBooleanWithDefault( parameters, "skipValidation", false ) );
        params.setImportMode( getEnumWithDefault( ObjectBundleMode.class, parameters, "importMode", ObjectBundleMode.COMMIT ) );
        params.setPreheatMode( getEnumWithDefault( PreheatMode.class, parameters, "preheatMode", PreheatMode.REFERENCE ) );
        params.setIdentifier( getEnumWithDefault( PreheatIdentifier.class, parameters, "identifier", PreheatIdentifier.UID ) );
        params.setImportStrategy( getEnumWithDefault( ImportStrategy.class, parameters, "importStrategy", ImportStrategy.CREATE_AND_UPDATE ) );
        params.setAtomicMode( getEnumWithDefault( AtomicMode.class, parameters, "atomicMode", AtomicMode.ALL ) );
        params.setMergeMode( getEnumWithDefault( MergeMode.class, parameters, "mergeMode", MergeMode.REPLACE ) );
        params.setFlushMode( getEnumWithDefault( FlushMode.class, parameters, "flushMode", FlushMode.AUTO ) );

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
}
