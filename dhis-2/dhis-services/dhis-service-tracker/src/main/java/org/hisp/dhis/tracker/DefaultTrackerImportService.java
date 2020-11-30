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

import static org.hisp.dhis.tracker.report.TrackerTimingsStats.COMMIT_OPS;
import static org.hisp.dhis.tracker.report.TrackerTimingsStats.PREHEAT_OPS;
import static org.hisp.dhis.tracker.report.TrackerTimingsStats.PROGRAMRULE_OPS;
import static org.hisp.dhis.tracker.report.TrackerTimingsStats.TOTAL_OPS;
import static org.hisp.dhis.tracker.report.TrackerTimingsStats.VALIDATION_OPS;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.bundle.TrackerBundleMode;
import org.hisp.dhis.tracker.bundle.TrackerBundleService;
import org.hisp.dhis.tracker.job.TrackerSideEffectDataBundle;
import org.hisp.dhis.tracker.preprocess.TrackerPreprocessService;
import org.hisp.dhis.tracker.report.TrackerBundleReport;
import org.hisp.dhis.tracker.report.TrackerImportReport;
import org.hisp.dhis.tracker.report.TrackerStatus;
import org.hisp.dhis.tracker.report.TrackerTimingsStats;
import org.hisp.dhis.tracker.report.TrackerTypeReport;
import org.hisp.dhis.tracker.report.TrackerValidationReport;
import org.hisp.dhis.tracker.validation.TrackerValidationService;
import org.springframework.stereotype.Service;

import com.google.common.base.Enums;
import com.google.common.collect.ImmutableMap;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DefaultTrackerImportService
    implements TrackerImportService
{
    @NonNull private final TrackerBundleService trackerBundleService;

    @NonNull private final TrackerValidationService trackerValidationService;

    @NonNull private final TrackerPreprocessService trackerPreprocessService;

    @NonNull private final TrackerUserService trackerUserService;

    @NonNull private final Notifier notifier;

    @Override
    public TrackerImportReport importTracker( TrackerImportParams params )
    {
        params.setUser( trackerUserService.getUser( params.getUserId() ) );

        // Init the Notifier
        ImportNotifier notifier = new ImportNotifier( this.notifier, params );

        // Keeps track of the elapsed time of each Import stage
        TrackerTimingsStats opsTimer = new TrackerTimingsStats();

        notifier.startImport();

        TrackerValidationReport validationReport = null;

        TrackerBundleReport bundleReport;

        try
        {
            //
            // pre-heat
            //
            TrackerBundle trackerBundle = opsTimer.exec( PREHEAT_OPS,
                () -> preheatBundle( params ) );

            Map<TrackerType, Integer> bundleSize = calculatePayloadSize( trackerBundle );

            //
            // preprocess
            //
            opsTimer.execVoid( PROGRAMRULE_OPS,
                () -> preProcessBundle( trackerBundle ) );

            //
            // validate
            //
            validationReport = opsTimer.exec( VALIDATION_OPS,
                () -> validateBundle( trackerBundle ) );

            notifier.notifyOps( VALIDATION_OPS, opsTimer );


            if ( validationReport.hasErrors() && params.getAtomicMode() == AtomicMode.ALL )
            {
                TrackerImportReport trackerImportReport = TrackerImportReport
                    .withValidationErrors( validationReport, opsTimer.stopTimer(),
                        bundleSize.values().stream().mapToInt( Integer::intValue ).sum() );

                notifier.endImport( trackerImportReport );

                return trackerImportReport;
            }
            else
            {
                if ( TrackerImportStrategy.DELETE == params.getImportStrategy() )
                {
                    bundleReport = opsTimer.exec( COMMIT_OPS, () -> deleteBundle( trackerBundle) );
                }
                else
                {
                    bundleReport = opsTimer.exec( COMMIT_OPS, () -> commitBundle( trackerBundle) );
                }

                notifier.notifyOps( COMMIT_OPS, opsTimer );

                TrackerImportReport trackerImportReport = TrackerImportReport.withImportCompleted( TrackerStatus.OK,
                    bundleReport, validationReport,
                    opsTimer.stopTimer(), bundleSize );

                notifier.endImport( trackerImportReport );

                return trackerImportReport;
            }
        }
        catch ( Exception e )
        {
            log.error( "Exception thrown during import.", e );

            TrackerImportReport report = TrackerImportReport.withError( "Exception:" + e.getMessage(),
                validationReport, opsTimer.stopTimer() );

            notifier.endImportWithError( report, e );

            return report;
        }
    }

    private Map<TrackerType, Integer> calculatePayloadSize( TrackerBundle bundle )
    {
        return ImmutableMap.<TrackerType, Integer> builder()
            .put( TrackerType.TRACKED_ENTITY, bundle.getTrackedEntities().size() )
            .put( TrackerType.ENROLLMENT, bundle.getEnrollments().size() )
            .put( TrackerType.EVENT, bundle.getEvents().size() )
            .put( TrackerType.RELATIONSHIP, bundle.getRelationships().size() ).build();
    }

    protected TrackerBundle preheatBundle( TrackerImportParams params )
    {
        return  trackerBundleService.create( params );
    }

    protected void preProcessBundle( TrackerBundle bundle )
    {
        TrackerBundle trackerBundle = trackerBundleService.runRuleEngine( bundle );
        trackerPreprocessService.preprocess( trackerBundle );
    }

    protected TrackerBundleReport commitBundle( TrackerBundle trackerBundle )
    {
        TrackerBundleReport bundleReport = trackerBundleService.commit( trackerBundle );

        List<TrackerSideEffectDataBundle> sideEffectDataBundles = Stream.of( TrackerType.ENROLLMENT, TrackerType.EVENT )
            .map( trackerType -> safelyGetSideEffectsDataBundles( bundleReport, trackerType ) )
            .flatMap( Collection::stream )
            .collect( Collectors.toList() );

        trackerBundleService.handleTrackerSideEffects( sideEffectDataBundles );

        return bundleReport;
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

    protected TrackerBundleReport deleteBundle( TrackerBundle trackerBundle )
    {
        return trackerBundleService.delete( trackerBundle );
    }

    protected TrackerValidationReport validateBundle( TrackerBundle trackerBundle )
    {
        TrackerValidationReport validationReport = new TrackerValidationReport();

        validationReport.add( trackerValidationService.validate( trackerBundle ) );
        
        return validationReport;
    }

    @Override
    public TrackerImportParams getParamsFromMap( Map<String, List<String>> parameters )
    {
        TrackerImportParams params = new TrackerImportParams();

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
        return importReport.copy( reportMode );
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
    
    @AllArgsConstructor
    static class ImportNotifier
    {

        private Notifier notifier;

        private TrackerImportParams params;

        public void startImport()
        {
            notifier.notify( params.getJobConfiguration(), "(" + params.getUsername() + ") Import:Start" );
        }

        public void notifyOps( String validationOps, TrackerTimingsStats opsTimer )
        {

            if ( params.hasJobConfiguration() )
            {
                notifier
                    .update( params.getJobConfiguration(),
                        "(" + params.getUsername() + ") Import:" + validationOps + " took "
                            + opsTimer.get( validationOps ) );
            }
        }

        public void endImport( TrackerImportReport importReport )
        {
            if ( params.hasJobConfiguration() )
            {
                notifier.update( params.getJobConfiguration(), "(" + params.getUsername() + ") Import:Done took " +
                    importReport.getTimingsStats().get( TOTAL_OPS ), true );

                notifier.addJobSummary( params.getJobConfiguration(), importReport, TrackerImportReport.class );
            }
        }

        public void endImportWithError( TrackerImportReport importReport, Exception e) {

            if ( params.hasJobConfiguration() )
            {
                notifier.update( params.getJobConfiguration(), "(" + params.getUsername() + ") Import:Failed with exception: " + e.getMessage(), true );
                notifier.addJobSummary( params.getJobConfiguration(), importReport, TrackerImportReport.class );
            }
        }
    }
}
