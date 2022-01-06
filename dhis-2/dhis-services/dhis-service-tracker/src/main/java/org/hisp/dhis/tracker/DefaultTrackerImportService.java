/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.tracker;

import static org.hisp.dhis.tracker.report.TrackerTimingsStats.COMMIT_OPS;
import static org.hisp.dhis.tracker.report.TrackerTimingsStats.PREHEAT_OPS;
import static org.hisp.dhis.tracker.report.TrackerTimingsStats.PREPROCESS_OPS;
import static org.hisp.dhis.tracker.report.TrackerTimingsStats.PROGRAMRULE_OPS;
import static org.hisp.dhis.tracker.report.TrackerTimingsStats.TOTAL_OPS;
import static org.hisp.dhis.tracker.report.TrackerTimingsStats.VALIDATE_PROGRAMRULE_OPS;
import static org.hisp.dhis.tracker.report.TrackerTimingsStats.VALIDATION_OPS;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.system.notification.NotificationLevel;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
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

import com.google.common.collect.ImmutableMap;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DefaultTrackerImportService
    implements TrackerImportService
{
    @NonNull
    private final TrackerBundleService trackerBundleService;

    @NonNull
    private final TrackerValidationService trackerValidationService;

    @NonNull
    private final TrackerPreprocessService trackerPreprocessService;

    @NonNull
    private final TrackerUserService trackerUserService;

    @NonNull
    private final Notifier notifier;

    @Override
    public TrackerImportReport importTracker( TrackerImportParams params )
    {
        params.setUser( trackerUserService.getUser( params.getUserId() ) );

        TrackerTimingsStats opsTimer = new TrackerTimingsStats();

        startImport( params );

        TrackerValidationReport validationReport = new TrackerValidationReport();

        TrackerBundleReport bundleReport;

        try
        {
            TrackerBundle trackerBundle = preHeat( params, opsTimer );

            Map<TrackerType, Integer> bundleSize = calculatePayloadSize( trackerBundle );

            preProcess( opsTimer, trackerBundle );

            if ( addToValidationReport( params, opsTimer, validationReport, trackerBundle ) )
            {
                return buildReportAndNotify( params, validationReport, opsTimer, bundleSize );
            }

            bundleReport = commit( params, opsTimer, trackerBundle );

            postCommit( trackerBundle );

            TrackerImportReport trackerImportReport = TrackerImportReport.withImportCompleted(
                TrackerStatus.OK,
                bundleReport, validationReport,
                opsTimer.stopTimer(), bundleSize );

            endImport( params, trackerImportReport );

            return trackerImportReport;
        }
        catch ( Exception e )
        {
            log.error( "Exception thrown during import.", e );

            TrackerImportReport report = TrackerImportReport.withError( "Exception:" + e.getMessage(),
                validationReport, opsTimer.stopTimer() );

            endImportWithError( params, report, e );

            return report;
        }
    }

    private TrackerBundle preHeat( TrackerImportParams params, TrackerTimingsStats opsTimer )
    {
        TrackerBundle trackerBundle = opsTimer.exec( PREHEAT_OPS,
            () -> preheatBundle( params ) );

        notifyOps( params, PREHEAT_OPS, opsTimer );

        return trackerBundle;
    }

    private void preProcess( TrackerTimingsStats opsTimer, TrackerBundle trackerBundle )
    {
        opsTimer.execVoid( PREPROCESS_OPS,
            () -> preProcessBundle( trackerBundle ) );
    }

    private boolean addToValidationReport( TrackerImportParams params, TrackerTimingsStats opsTimer,
        TrackerValidationReport validationReport, TrackerBundle trackerBundle )
    {
        validationReport.add( opsTimer.exec( VALIDATION_OPS,
            () -> validateBundle( params, trackerBundle, opsTimer ) ) );

        if ( exitOnError( validationReport, params ) )
        {
            return true;
        }

        if ( !trackerBundle.isSkipRuleEngine() && !params.getImportStrategy().isDelete() )
        {
            validationReport.add( execRuleEngine( params, opsTimer, trackerBundle ) );
        }

        return exitOnError( validationReport, params );
    }

    private TrackerBundleReport commit( TrackerImportParams params, TrackerTimingsStats opsTimer,
        TrackerBundle trackerBundle )
    {
        TrackerBundleReport bundleReport;
        if ( TrackerImportStrategy.DELETE == params.getImportStrategy() )
        {
            bundleReport = opsTimer.exec( COMMIT_OPS, () -> deleteBundle( trackerBundle ) );
        }
        else
        {
            bundleReport = opsTimer.exec( COMMIT_OPS, () -> commitBundle( trackerBundle ) );
        }

        notifyOps( params, COMMIT_OPS, opsTimer );
        return bundleReport;
    }

    private void postCommit( TrackerBundle trackerBundle )
    {
        trackerBundleService.postCommit( trackerBundle );
    }

    protected TrackerValidationReport validateBundle( TrackerImportParams params, TrackerBundle trackerBundle,
        TrackerTimingsStats opsTimer )
    {
        TrackerValidationReport validationReport = trackerValidationService.validate( trackerBundle );

        notifyOps( params, VALIDATION_OPS, opsTimer );

        return validationReport;
    }

    private TrackerValidationReport execRuleEngine( TrackerImportParams params, TrackerTimingsStats opsTimer,
        TrackerBundle trackerBundle )
    {
        opsTimer.execVoid( PROGRAMRULE_OPS,
            () -> runRuleEngine( trackerBundle ) );

        notifyOps( params, PROGRAMRULE_OPS, opsTimer );

        TrackerValidationReport report = opsTimer.exec( VALIDATE_PROGRAMRULE_OPS,
            () -> validateRuleEngine( trackerBundle ) );

        notifyOps( params, VALIDATE_PROGRAMRULE_OPS, opsTimer );

        return report;
    }

    protected TrackerValidationReport validateRuleEngine( TrackerBundle trackerBundle )
    {
        TrackerValidationReport ruleEngineValidationReport = new TrackerValidationReport();

        ruleEngineValidationReport.add( trackerValidationService.validateRuleEngine( trackerBundle ) );

        return ruleEngineValidationReport;
    }

    private TrackerImportReport buildReportAndNotify( TrackerImportParams params,
        TrackerValidationReport validationReport,
        TrackerTimingsStats opsTimer, Map<TrackerType, Integer> bundleSize )
    {
        TrackerImportReport trackerImportReport = TrackerImportReport.withValidationErrors( validationReport,
            opsTimer.stopTimer(),
            bundleSize.values().stream().mapToInt( Integer::intValue ).sum() );

        endImport( params, trackerImportReport );

        return trackerImportReport;
    }

    private boolean exitOnError( TrackerValidationReport validationReport, TrackerImportParams params )
    {
        return validationReport.hasErrors() && params.getAtomicMode() == AtomicMode.ALL;
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
        return trackerBundleService.create( params );
    }

    protected void preProcessBundle( TrackerBundle bundle )
    {
        trackerPreprocessService.preprocess( bundle );
    }

    protected void runRuleEngine( TrackerBundle bundle )
    {
        trackerBundleService.runRuleEngine( bundle );
    }

    protected TrackerBundleReport commitBundle( TrackerBundle trackerBundle )
    {
        TrackerBundleReport bundleReport = trackerBundleService.commit( trackerBundle );

        if ( !trackerBundle.isSkipSideEffects() )
        {
            List<TrackerSideEffectDataBundle> sideEffectDataBundles = Stream
                .of( TrackerType.ENROLLMENT, TrackerType.EVENT )
                .map( trackerType -> safelyGetSideEffectsDataBundles( bundleReport, trackerType ) )
                .flatMap( Collection::stream )
                .collect( Collectors.toList() );

            trackerBundleService.handleTrackerSideEffects( sideEffectDataBundles );
        }

        return bundleReport;
    }

    private List<TrackerSideEffectDataBundle> safelyGetSideEffectsDataBundles( TrackerBundleReport bundleReport,
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

    private void startImport( TrackerImportParams params )
    {
        if ( null != params.getJobConfiguration() )
        {
            notifier.notify( params.getJobConfiguration(),
                params.userStartInfo() + " Import:Start" );
        }
    }

    private void notifyOps( TrackerImportParams params, String validationOps, TrackerTimingsStats opsTimer )
    {
        if ( null != params.getJobConfiguration() )
        {
            notifier
                .update( params.getJobConfiguration(), NotificationLevel.DEBUG,
                    params +
                        validationOps + " completed in "
                        + opsTimer.get( validationOps ) + " Import:"
                        + validationOps );
        }
    }

    private void endImport( TrackerImportParams params, TrackerImportReport importReport )
    {
        if ( null != params.getJobConfiguration() )
        {
            notifier.update( params.getJobConfiguration(),
                params +
                    " finished in " +
                    importReport.getTimingsStats().get( TOTAL_OPS ) + " Import:Done",
                true );

            if ( params.getJobConfiguration().isInMemoryJob() )
            {
                notifier.addJobSummary( params.getJobConfiguration(), importReport, TrackerImportReport.class );
            }
        }
    }

    private void endImportWithError( TrackerImportParams params, TrackerImportReport importReport, Exception e )
    {
        if ( null != params.getJobConfiguration() && params.getJobConfiguration().isInMemoryJob() )
        {
            notifier.update( params.getJobConfiguration(), NotificationLevel.ERROR,
                params +
                    " failed with exception: "
                    + e.getMessage() + " Import:Error",
                true );

            if ( params.getJobConfiguration().isInMemoryJob() )
            {
                notifier.addJobSummary( params.getJobConfiguration(), importReport, TrackerImportReport.class );
            }
        }
    }

    /**
     * Clone the TrackerImportReport and filters out validation data based on
     * the provided {@link TrackerBundleReport}.
     *
     * @return a copy of the current TrackerImportReport
     */
    @Override
    public TrackerImportReport buildImportReport( TrackerImportReport trackerImportReport,
        TrackerBundleReportMode reportMode )
    {
        TrackerImportReport.TrackerImportReportBuilder trackerImportReportClone = TrackerImportReport.builder()
            .status( trackerImportReport.getStatus() )
            .stats( trackerImportReport.getStats() )
            .bundleReport( trackerImportReport.getBundleReport() ).message( trackerImportReport.getMessage() );

        TrackerValidationReport validationReport = new TrackerValidationReport();

        Optional.ofNullable( trackerImportReport.getValidationReport() )
            .ifPresent( trackerValidationReport -> {
                validationReport.setErrorReports( trackerValidationReport.getErrorReports() );
                validationReport.setWarningReports( trackerValidationReport.getWarningReports() );
                validationReport.setPerformanceReport( trackerValidationReport.getPerformanceReport() );
            } );

        switch ( reportMode )
        {
        case ERRORS:
            validationReport.setPerformanceReport( null );
            validationReport.setWarningReports( null );
            break;
        case WARNINGS:
            validationReport.setPerformanceReport( null );
            break;
        case FULL:
            trackerImportReportClone.timingsStats( trackerImportReport.getTimingsStats() );
            break;
        }

        trackerImportReportClone.validationReport( validationReport );

        return trackerImportReportClone.build();
    }
}
