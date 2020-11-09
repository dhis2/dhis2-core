package org.hisp.dhis.tracker;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.commons.timer.SystemTimer;
import org.hisp.dhis.commons.timer.Timer;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.bundle.TrackerBundleMode;
import org.hisp.dhis.tracker.bundle.TrackerBundleParams;
import org.hisp.dhis.tracker.bundle.TrackerBundleService;
import org.hisp.dhis.tracker.job.TrackerSideEffectDataBundle;
import org.hisp.dhis.tracker.preprocess.TrackerPreprocessService;
import org.hisp.dhis.tracker.report.TrackerBundleReport;
import org.hisp.dhis.tracker.report.TrackerImportReport;
import org.hisp.dhis.tracker.report.TrackerStatus;
import org.hisp.dhis.tracker.report.TrackerTypeReport;
import org.hisp.dhis.tracker.report.TrackerValidationReport;
import org.hisp.dhis.tracker.validation.TrackerValidationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.base.Enums;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Service
@Slf4j
public class DefaultTrackerImportService
    implements TrackerImportService
{
    private final TrackerBundleService trackerBundleService;

    private final TrackerValidationService trackerValidationService;

    private final TrackerPreprocessService trackerPreprocessService;

    private final TrackerUserService trackerUserService;

    private final Notifier notifier;

    public DefaultTrackerImportService(
        TrackerBundleService trackerBundleService,
        TrackerValidationService trackerValidationService,
        TrackerPreprocessService trackerPreprocessService,
        TrackerUserService trackerUserService,
        Notifier notifier )
    {
        this.trackerBundleService = trackerBundleService;
        this.trackerValidationService = trackerValidationService;
        this.trackerPreprocessService = trackerPreprocessService;
        this.notifier = notifier;
        this.trackerUserService = trackerUserService;
    }

    @Override
    @Transactional // TODO: This annotation must be removed. Performance killer.
    public TrackerImportReport importTracker( TrackerImportParams params )
    {
        Timer requestTimer = new SystemTimer().start();

        if ( params.getUser() == null )
        {
            params.setUser( trackerUserService.getUser( params.getUserId() ) );
        }

        TrackerImportReport importReport = new TrackerImportReport();

        if ( params.hasJobConfiguration() )
        {
            notifier.notify( params.getJobConfiguration(), "(" + params.getUsername() + ") Import:Start" );
        }
        
        try
        {

            TrackerBundle trackerBundle = preheatBundle( params, importReport );

            trackerBundle = preProcessBundle( trackerBundle, importReport );

            TrackerValidationReport validationReport = validateBundle( params, importReport, trackerBundle );

            if ( validationReport.hasErrors() && params.getAtomicMode() == AtomicMode.ALL )
            {
                importReport.setStatus( TrackerStatus.ERROR );
            }
            else
            {
                if ( TrackerImportStrategy.DELETE == params.getImportStrategy() )
                {
                    deleteBundle( params, importReport, trackerBundle );
                }
                else
                {
                    commitBundle( params, importReport, trackerBundle );
                }
            }

            importReport.getTimings().setTotalImport( requestTimer.toString() );
            
            if ( params.hasJobConfiguration() )
            {
                notifier.update( params.getJobConfiguration(), "(" + params.getUsername() + ") Import:Done took " + requestTimer, true );
                notifier.addJobSummary( params.getJobConfiguration(), importReport, TrackerImportReport.class );
            }
           
        }
        
        catch ( Exception e )
        {
            log.error( "Exception thrown during import.",e );

            importReport.setMessage( "Exception:" + e.getMessage() );
            importReport.setStatus( TrackerStatus.ERROR );

            if ( params.hasJobConfiguration() )
            {
                notifier.update( params.getJobConfiguration(), "(" + params.getUsername() + ") Import:Failed with exception: " + e.getMessage(), true );
                notifier.addJobSummary( params.getJobConfiguration(), importReport, TrackerImportReport.class );
            }
        }
       
        return importReport;
    }

    protected TrackerBundle preheatBundle( TrackerImportParams params, TrackerImportReport importReport )
    {
        Timer preheatTimer = new SystemTimer().start();

        TrackerBundleParams bundleParams = params.toTrackerBundleParams();
        TrackerBundle trackerBundle = trackerBundleService.create( bundleParams );

        importReport.getTimings().setPreheat( preheatTimer.toString() );
        return trackerBundle;
    }

    protected TrackerBundle preProcessBundle( TrackerBundle bundle, TrackerImportReport importReport )
    {
        Timer preProcessTimer = new SystemTimer().start();

        TrackerBundle trackerBundle = trackerBundleService.runRuleEngine( bundle );
        trackerBundle = trackerPreprocessService.preprocess( trackerBundle );

        importReport.getTimings().setProgramrule( preProcessTimer.toString() );
        return trackerBundle;
    }

    protected void commitBundle( TrackerImportParams params, TrackerImportReport importReport,
        TrackerBundle trackerBundle )
    {
        Timer commitTimer = new SystemTimer().start();

        TrackerBundleReport bundleReport = trackerBundleService.commit( trackerBundle );

        List<TrackerSideEffectDataBundle> sideEffectDataBundles = Stream.of( TrackerType.ENROLLMENT, TrackerType.EVENT )
            .map( trackerType -> safelyGetSideEffectsDataBundles( bundleReport, trackerType ) )
            .flatMap( Collection::stream )
            .collect( Collectors.toList() );

        trackerBundleService.handleTrackerSideEffects( sideEffectDataBundles );

        importReport.setBundleReport( bundleReport );

        importReport.getTimings().setCommit( commitTimer.toString() );

        if ( params.hasJobConfiguration() )
        {
            notifier.update( params.getJobConfiguration(),
                "(" + params.getUsername() + ") " + "Import:Commit took " + commitTimer );
        }
    }

    List<TrackerSideEffectDataBundle> safelyGetSideEffectsDataBundles( TrackerBundleReport bundleReport,
        TrackerType trackerType )
    {
        return Optional.ofNullable( bundleReport )
            .map( TrackerBundleReport::getTypeReportMap )
            .map( reportMap -> reportMap.get( trackerType ) )
            .map( TrackerTypeReport::getSideEffectDataBundles )
            .orElse( Collections.emptyList() );
    }

    protected void deleteBundle( TrackerImportParams params, TrackerImportReport importReport,
        TrackerBundle trackerBundle )
    {
        Timer commitTimer = new SystemTimer().start();

        importReport.setBundleReport( trackerBundleService.delete( trackerBundle ) );

        importReport.getTimings().setCommit( commitTimer.toString() );

        if ( params.hasJobConfiguration() )
        {
            notifier.update( params.getJobConfiguration(),
                "(" + params.getUsername() + ") " + "Import:Commit took " + commitTimer );
        }
    }

    protected TrackerValidationReport validateBundle( TrackerImportParams params, TrackerImportReport importReport,
        TrackerBundle trackerBundle )
    {
        Timer validationTimer = new SystemTimer().start();

        TrackerValidationReport validationReport = new TrackerValidationReport();

        // Do all the validation
        validationReport.add( trackerValidationService.validate( trackerBundle ) );

        importReport.getTimings().setValidation( validationTimer.toString() );
        importReport.setTrackerValidationReport( validationReport );

        if ( params.hasJobConfiguration() )
        {
            notifier
                .update( params.getJobConfiguration(),
                    "(" + params.getUsername() + ") Import:Validation took " + validationTimer );
        }
        return validationReport;
    }

    @Override
    public TrackerImportParams getParamsFromMap( Map<String, List<String>> parameters )
    {
        TrackerImportParams params = new TrackerImportParams();
        if ( params.getUser() == null )
        {
            params.setUser( trackerUserService.getUser( params.getUserId() ) );
        }
        params.setValidationMode( getEnumWithDefault( ValidationMode.class, parameters, "validationMode",
            ValidationMode.FULL ) );
        params.setImportMode(
            getEnumWithDefault( TrackerBundleMode.class, parameters, "importMode", TrackerBundleMode.COMMIT ) );
        params.setIdentifiers( getTrackerIdentifiers( parameters ) );
        params.setImportStrategy( getEnumWithDefault( TrackerImportStrategy.class, parameters, "importStrategy",
            TrackerImportStrategy.CREATE_AND_UPDATE ) );
        params.setAtomicMode( getEnumWithDefault( AtomicMode.class, parameters, "atomicMode", AtomicMode.ALL ) );
        params.setFlushMode( getEnumWithDefault( FlushMode.class, parameters, "flushMode", FlushMode.AUTO ) );

        return params;
    }

    @Override
    public TrackerImportReport buildImportReport( TrackerImportReport importReport, TrackerBundleReportMode reportMode )
    {

        TrackerImportReport filteredTrackerImportReport = new TrackerImportReport();
        TrackerValidationReport trackerValidationReport = new TrackerValidationReport();
        filteredTrackerImportReport.setTrackerValidationReport( trackerValidationReport );
        filteredTrackerImportReport.setTimings( importReport.getTimings() );
        filteredTrackerImportReport.getTrackerValidationReport()
            .setErrorReports( importReport.getTrackerValidationReport().getErrorReports() );
        filteredTrackerImportReport.getTrackerValidationReport()
            .setWarningReports( importReport.getTrackerValidationReport().getWarningReports() );
        filteredTrackerImportReport.setBundleReport( importReport.getBundleReport() );
        filteredTrackerImportReport.setStatus( importReport.getStatus() );
        filteredTrackerImportReport.setMessage( importReport.getMessage() );

        switch ( reportMode )
        {
        case ERRORS:
            filteredTrackerImportReport.getTrackerValidationReport().setPerformanceReport( null );
            filteredTrackerImportReport.getTrackerValidationReport().setWarningReports( null );
            filteredTrackerImportReport.setTimings( null );
            break;
        case WARNINGS:
            filteredTrackerImportReport.getTrackerValidationReport().setPerformanceReport( null );
            filteredTrackerImportReport.setTimings( null );
            break;
        case FULL:
            break;
        }

        return filteredTrackerImportReport;
    }

    //-----------------------------------------------------------------------------------
    // Utility Methods
    //-----------------------------------------------------------------------------------

    private TrackerIdentifierParams getTrackerIdentifiers( Map<String, List<String>> parameters )
    {
        TrackerIdScheme idScheme = getEnumWithDefault( TrackerIdScheme.class, parameters, "idScheme",
            TrackerIdScheme.UID );
        TrackerIdScheme orgUnitIdScheme = getEnumWithDefault( TrackerIdScheme.class, parameters, "orgUnitIdScheme",
            idScheme );
        TrackerIdScheme programIdScheme = getEnumWithDefault( TrackerIdScheme.class, parameters, "programIdScheme",
            idScheme );
        TrackerIdScheme programStageIdScheme = getEnumWithDefault( TrackerIdScheme.class, parameters,
            "programStageIdScheme", idScheme );
        TrackerIdScheme dataElementIdScheme = getEnumWithDefault( TrackerIdScheme.class, parameters,
            "dataElementIdScheme", idScheme );

        return TrackerIdentifierParams.builder()
            .idScheme( TrackerIdentifier.builder().idScheme( idScheme )
                .value( getAttributeUidOrNull( parameters, "idScheme" ) ).build() )
            .orgUnitIdScheme( TrackerIdentifier.builder().idScheme( orgUnitIdScheme )
                .value( getAttributeUidOrNull( parameters, "orgUnitIdScheme" ) ).build() )
            .programIdScheme( TrackerIdentifier.builder().idScheme( programIdScheme )
                .value( getAttributeUidOrNull( parameters, "programIdScheme" ) ).build() )
            .programStageIdScheme( TrackerIdentifier.builder().idScheme( programStageIdScheme )
                .value( getAttributeUidOrNull( parameters, "programStageIdScheme" ) ).build() )
            .dataElementIdScheme( TrackerIdentifier.builder().idScheme( dataElementIdScheme )
                .value( getAttributeUidOrNull( parameters, "dataElementIdScheme" ) ).build() )
            .build();
    }

    private <T extends Enum<T>> T getEnumWithDefault( Class<T> enumKlass, Map<String, List<String>> parameters,
        String key, T defaultValue )
    {
        if ( parameters == null || parameters.get( key ) == null || parameters.get( key ).isEmpty() )
        {
            return defaultValue;
        }

        if ( TrackerIdScheme.class.equals( enumKlass ) && IdScheme.isAttribute( parameters.get( key ).get( 0 ) ) )
        {
            return Enums.getIfPresent( enumKlass, "ATTRIBUTE" ).orNull();
        }

        String value = String.valueOf( parameters.get( key ).get( 0 ) );

        return Enums.getIfPresent( enumKlass, value ).or( defaultValue );
    }

    private String getAttributeUidOrNull( Map<String, List<String>> parameters, String key )
    {
        if ( parameters == null || parameters.get( key ) == null || parameters.get( key ).isEmpty() )
        {
            return null;
        }

        if ( IdScheme.isAttribute( parameters.get( key ).get( 0 ) ) )
        {
            String uid = "";

            // Get second half of string, separated by ':'
            String[] splitParam = parameters.get( key ).get( 0 ).split( ":" );

            if ( splitParam.length > 1 )
            {
                uid = splitParam[1];
            }

            if ( CodeGenerator.isValidUid( uid ) )
            {
                return uid;
            }
        }

        return null;
    }
}
