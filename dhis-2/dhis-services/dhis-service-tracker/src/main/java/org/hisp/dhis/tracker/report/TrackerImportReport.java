package org.hisp.dhis.tracker.report;

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

import java.util.Map;

import org.hisp.dhis.tracker.TrackerBundleReportMode;
import org.hisp.dhis.tracker.TrackerType;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;

/**
 * This immutable object collects all the relevant information created during a
 * Tracker Import process.
 *
 * The TrackerImportReport is created at the beginning of a Tracker Import
 * session and returned to the caller.
 *
 * @author Luciano Fiandesio
 */
@Getter
public class TrackerImportReport
{
    /**
     * The global status of the Import operation.
     *
     * - OK - no errors ( including validation errors )
     *
     * - WARNING - at least one Warning was collected during the Import
     *
     * - ERROR - at least on Error was collected during the Import
     */
    @JsonProperty
    private TrackerStatus status = TrackerStatus.OK;

    /**
     * A list of all validation errors occurred during the validation process
     */
    @JsonProperty
    private TrackerValidationReport validationReport;

    /**
     * A final summary broken down by operation (Insert, Update, Delete, Ignored)
     * showing how many entities where processed
     */
    @JsonProperty
    private TrackerStats stats;

    /**
     * A report object containing the elapsed time for each Import stage
     */
    @JsonProperty
    private TrackerTimingsStats timingsStats;

    /**
     * A report containing the outcome of the commit stage (e.g. how many entities
     * were persisted)
     */
    @JsonProperty
    private TrackerBundleReport bundleReport;

    /**
     * A message to attach to the report. This message is designed to be used only
     * if a catastrophic error occurs and the Import Process has to stop abruptly.
     */
    @JsonProperty
    private String message;

    private TrackerImportReport()
    {
    }

    /**
     * Factory method to use in case one ore more Validation errors are present in
     * the {@link TrackerValidationReport} and the Import process needs to exit
     * without attempting persistence.
     *
     * Import statistics are calculated assuming that the persistence stage was
     * never attempted, therefore all bundle objects were ignored.
     *
     * @param validationReport The validation report
     * @param timingsStats The timing stats
     * @param bundleSize The sum of all bundle objects
     *
     */
    public static TrackerImportReport withValidationErrors(
        TrackerValidationReport validationReport,
        TrackerTimingsStats timingsStats, int bundleSize )
    {
        TrackerImportReport report = new TrackerImportReport();
        report.status = TrackerStatus.ERROR;
        report.validationReport = validationReport;
        report.timingsStats = timingsStats;

        // TrackerBundleReport is missing, nothing was persisted, assume all was ignored

        TrackerStats stats = new TrackerStats();
        stats.setIgnored( bundleSize );
        report.stats = stats;
        return report;
    }

    /**
     * Factory method to use in case of an unrecoverable error during the Tracker
     * Import. This factory method will set the status to ERROR.
     *
     * This method should only be used when the Import process has to exit.
     *
     * Import statistics are not calculated.
     *
     * @param message The error message
     * @param validationReport The validation report if available
     * @param timingsStats The timing stats if available
     *
     */
    public static TrackerImportReport withError( String message, TrackerValidationReport validationReport,
        TrackerTimingsStats timingsStats )
    {
        TrackerImportReport report = new TrackerImportReport();
        report.status = TrackerStatus.ERROR;
        report.message = message;
        report.validationReport = validationReport;
        report.timingsStats = timingsStats;

        // TODO shall we calculate stats in this case?

        return report;
    }

    /**
     * Factory method to use when a Tracker Import process completes.
     *
     * Import statistics are calculated based on the {@link TrackerBundleReport} and
     * {@link TrackerValidationReport}.
     * 
     * @param status The outcome of the process
     * @param bundleReport The report containing how many bundle objects were
     *        successfully persisted
     * @param validationReport The validation report if available
     * @param timingsStats The timing stats if available
     * @param bundleSize a map containing the size of each entity type in the Bundle
     *        - before the validation
     */
    public static TrackerImportReport withImportCompleted( TrackerStatus status, TrackerBundleReport bundleReport,
        TrackerValidationReport validationReport,
        TrackerTimingsStats timingsStats, Map<TrackerType, Integer> bundleSize )
    {
        TrackerImportReport report = new TrackerImportReport();
        report.status = status;
        report.validationReport = validationReport;
        report.timingsStats = timingsStats;

        report.bundleReport = processBundleReport( bundleReport, bundleSize );

        TrackerStats stats = new TrackerStats();
        stats.merge( bundleReport.getStats() );
        stats.setIgnored( Math.toIntExact( validationReport.size() ) );
        report.stats = stats;

        return report;
    }

    /**
     * Calculates the 'ignored' value for each type of entity in the
     * {@link TrackerBundleReport}.
     * 
     * The 'ignored' value is calculated by subtracting the sum of all processed
     * entities from the TrackerBundleReport (by type) from the bundle size
     * specified in the 'bundleSize' map.
     */
    private static TrackerBundleReport processBundleReport( TrackerBundleReport bundleReport,
        Map<TrackerType, Integer> bundleSize )
    {
        for ( final TrackerType value : TrackerType.values() )
        {
            final TrackerTypeReport trackerTypeReport = bundleReport.getTypeReportMap().get( value );
            if ( trackerTypeReport != null )
            {
                final TrackerStats stats = trackerTypeReport.getStats();
                if ( stats != null )
                {
                    int statsSize = stats.getDeleted() + stats.getCreated() + stats.getUpdated();
                    stats.setIgnored( bundleSize.getOrDefault( value,  statsSize) - statsSize );
                }
            }
        }
        return bundleReport;
    }

    /**
     * Clone the TrackerImportReport and filters out validation data based on the
     * provided {@link TrackerBundleReport}.
     *
     * @return a copy of the current TrackerImportReport
     */
    public TrackerImportReport copy( TrackerBundleReportMode reportMode )
    {
        TrackerImportReport trackerImportReport = new TrackerImportReport();
        trackerImportReport.status = this.status;

        TrackerValidationReport validationReport = new TrackerValidationReport();

        validationReport.setErrorReports( this.validationReport.getErrorReports() );
        validationReport.setWarningReports( this.validationReport.getWarningReports() );
        validationReport.setPerformanceReport( this.validationReport.getPerformanceReport() );

        trackerImportReport.validationReport = validationReport;
        trackerImportReport.stats = this.stats;
        trackerImportReport.timingsStats = this.timingsStats;
        trackerImportReport.bundleReport = this.bundleReport;

        switch ( reportMode )
        {
        case ERRORS:
            trackerImportReport.getValidationReport().setPerformanceReport( null );
            trackerImportReport.getValidationReport().setWarningReports( null );
            trackerImportReport.timingsStats = null;
            break;
        case WARNINGS:
            trackerImportReport.getValidationReport().setPerformanceReport( null );
            trackerImportReport.timingsStats = null;
            break;
        case FULL:
            break;
        }

        return trackerImportReport;
    }
}
