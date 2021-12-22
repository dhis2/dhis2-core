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
package org.hisp.dhis.tracker.report;

import static org.hisp.dhis.tracker.TrackerType.TRACKED_ENTITY;
import static org.hisp.dhis.tracker.report.TrackerTimingsStats.COMMIT_OPS;
import static org.hisp.dhis.tracker.report.TrackerTimingsStats.PREHEAT_OPS;
import static org.hisp.dhis.tracker.report.TrackerTimingsStats.PREPARE_REQUEST_OPS;
import static org.hisp.dhis.tracker.report.TrackerTimingsStats.PROGRAMRULE_OPS;
import static org.hisp.dhis.tracker.report.TrackerTimingsStats.TOTAL_OPS;
import static org.hisp.dhis.tracker.report.TrackerTimingsStats.TOTAL_REQUEST_OPS;
import static org.hisp.dhis.tracker.report.TrackerTimingsStats.VALIDATION_OPS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.tracker.TrackerBundleReportMode;
import org.hisp.dhis.tracker.TrackerImportService;
import org.hisp.dhis.tracker.TrackerType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
class TrackerBundleImportReportTest extends DhisSpringTest
{

    @Autowired
    private TrackerImportService trackerImportService;

    @Autowired
    private ObjectMapper jsonMapper;

    @Test
    void testImportReportErrors()
    {
        TrackerImportReport report = trackerImportService.buildImportReport( createImportReport(),
            TrackerBundleReportMode.ERRORS );
        assertEquals( TrackerStatus.OK, report.getStatus() );
        assertStats( report );
        assertNotNull( report.getValidationReport() );
        assertNull( report.getValidationReport().getPerformanceReport() );
        assertNull( report.getValidationReport().getWarningReports() );
        assertNotNull( report.getValidationReport().getErrorReports() );
        assertNull( report.getTimingsStats() );
    }

    @Test
    void testImportReportWarnings()
    {
        TrackerImportReport report = trackerImportService.buildImportReport( createImportReport(),
            TrackerBundleReportMode.WARNINGS );
        assertEquals( TrackerStatus.OK, report.getStatus() );
        assertStats( report );
        assertNotNull( report.getValidationReport() );
        assertNull( report.getValidationReport().getPerformanceReport() );
        assertNotNull( report.getValidationReport().getErrorReports() );
        assertNotNull( report.getValidationReport().getWarningReports() );
        assertNull( report.getTimingsStats() );
    }

    @Test
    void testImportReportFull()
    {
        TrackerImportReport report = trackerImportService.buildImportReport( createImportReport(),
            TrackerBundleReportMode.FULL );
        assertEquals( TrackerStatus.OK, report.getStatus() );
        assertStats( report );
        assertNotNull( report.getValidationReport() );
        assertNotNull( report.getValidationReport().getPerformanceReport() );
        assertNotNull( report.getValidationReport().getErrorReports() );
        assertNotNull( report.getValidationReport().getWarningReports() );
        assertNotNull( report.getTimingsStats() );
        assertEquals( "1 sec.", report.getTimingsStats().getProgramRule() );
        assertEquals( "2 sec.", report.getTimingsStats().getCommit() );
        assertEquals( "3 sec.", report.getTimingsStats().getPreheat() );
        assertEquals( "4 sec.", report.getTimingsStats().getValidation() );
        assertEquals( "10 sec.", report.getTimingsStats().getTotalImport() );
    }

    @Test
    void testSerializingAndDeserializingImportReport()
        throws JsonProcessingException
    {
        // Build BundleReport
        Map<TrackerType, TrackerTypeReport> typeReportMap = new HashMap<>();
        TrackerTypeReport typeReport = new TrackerTypeReport( TRACKED_ENTITY );
        TrackerObjectReport trackerObjectReport = new TrackerObjectReport( TRACKED_ENTITY );
        List<TrackerErrorReport> trackerErrorReports = new ArrayList<>();
        TrackerErrorReport errorReport1 = new TrackerErrorReport(
            "Could not find OrganisationUnit: ``, linked to Tracked Entity.", TrackerErrorCode.E1049, TRACKED_ENTITY,
            "BltTZV9HvEZ" );
        TrackerErrorReport errorReport2 = new TrackerErrorReport( "Could not find TrackedEntityType: `Q9GufDoplCL`.",
            TrackerErrorCode.E1049, TRACKED_ENTITY, "BltTZV9HvEZ" );
        trackerErrorReports.add( errorReport1 );
        trackerErrorReports.add( errorReport2 );
        trackerObjectReport.getErrorReports().addAll( trackerErrorReports );
        trackerObjectReport.setIndex( 0 );
        trackerObjectReport.setUid( "BltTZV9HvEZ" );
        typeReport.addObjectReport( trackerObjectReport );
        typeReport.getStats().setCreated( 1 );
        typeReport.getStats().setUpdated( 2 );
        typeReport.getStats().setDeleted( 3 );
        typeReportMap.put( TRACKED_ENTITY, typeReport );
        TrackerBundleReport bundleReport = new TrackerBundleReport( TrackerStatus.ERROR, typeReportMap );
        // Build TimingsStats
        TrackerTimingsStats timingsStats = new TrackerTimingsStats();
        timingsStats.set( COMMIT_OPS, "0.1 sec." );
        timingsStats.set( PREHEAT_OPS, "0.2 sec." );
        timingsStats.set( PROGRAMRULE_OPS, "0.3 sec." );
        timingsStats.set( TOTAL_OPS, "0.4 sec." );
        timingsStats.set( VALIDATION_OPS, "4 sec." );
        timingsStats.set( PREPARE_REQUEST_OPS, "0.5 sec." );
        timingsStats.set( TOTAL_REQUEST_OPS, "0.6 sec." );
        // Build ValidationReport
        TrackerValidationReport tvr = new TrackerValidationReport();
        // Error Reports - Validation Report
        tvr.getErrorReports()
            .add( new TrackerErrorReport( "Could not find OrganisationUnit: ``, linked to Tracked Entity.",
                TrackerErrorCode.E1049, TRACKED_ENTITY, "BltTZV9HvEZ" ) );
        // Warning Reports - Validation Report
        tvr.getWarningReports()
            .add( new TrackerWarningReport( "ProgramStage `l8oDIfJJhtg` does not allow user assignment",
                TrackerErrorCode.E1120, TrackerType.EVENT, "BltTZV9HvEZ" ) );
        // Create the TrackerImportReport
        final Map<TrackerType, Integer> bundleSize = new HashMap<>();
        bundleSize.put( TRACKED_ENTITY, 1 );
        TrackerImportReport toSerializeReport = TrackerImportReport.withImportCompleted( TrackerStatus.ERROR,
            bundleReport, tvr, timingsStats, bundleSize );
        // Serialize TrackerImportReport into String
        String jsonString = jsonMapper.writeValueAsString( toSerializeReport );
        // Deserialize the String back into TrackerImportReport
        TrackerImportReport deserializedReport = jsonMapper.readValue( jsonString, TrackerImportReport.class );
        // Verify Stats
        assertEquals( toSerializeReport.getStats().getIgnored(), deserializedReport.getStats().getIgnored() );
        assertEquals( toSerializeReport.getStats().getDeleted(), deserializedReport.getStats().getDeleted() );
        assertEquals( toSerializeReport.getStats().getUpdated(), deserializedReport.getStats().getUpdated() );
        assertEquals( toSerializeReport.getStats().getCreated(), deserializedReport.getStats().getCreated() );
        assertEquals( toSerializeReport.getStats().getTotal(), deserializedReport.getStats().getTotal() );
        // Verify Status
        assertEquals( toSerializeReport.getBundleReport().getStatus(),
            deserializedReport.getBundleReport().getStatus() );
        // Verify BundleReport
        assertEquals( toSerializeReport.getBundleReport().getStats().getIgnored(),
            deserializedReport.getBundleReport().getStats().getIgnored() );
        assertEquals( toSerializeReport.getBundleReport().getStats().getDeleted(),
            deserializedReport.getBundleReport().getStats().getDeleted() );
        assertEquals( toSerializeReport.getBundleReport().getStats().getUpdated(),
            deserializedReport.getBundleReport().getStats().getUpdated() );
        assertEquals( toSerializeReport.getBundleReport().getStats().getCreated(),
            deserializedReport.getBundleReport().getStats().getCreated() );
        assertEquals( toSerializeReport.getBundleReport().getStats().getTotal(),
            deserializedReport.getBundleReport().getStats().getTotal() );
        TrackerTypeReport serializedReportTrackerTypeReport = toSerializeReport.getBundleReport().getTypeReportMap()
            .get( TRACKED_ENTITY );
        TrackerTypeReport deserializedReportTrackerTypeReport = deserializedReport.getBundleReport().getTypeReportMap()
            .get( TRACKED_ENTITY );
        // sideEffectsDataBundle is no more relevant to object equivalence, so
        // just asserting on all other fields.
        assertEquals( serializedReportTrackerTypeReport.getTrackerType(),
            deserializedReportTrackerTypeReport.getTrackerType() );
        assertEquals( serializedReportTrackerTypeReport.getObjectReportMap(),
            deserializedReportTrackerTypeReport.getObjectReportMap() );
        assertEquals( serializedReportTrackerTypeReport.getObjectReports(),
            deserializedReportTrackerTypeReport.getObjectReports() );
        assertEquals( serializedReportTrackerTypeReport.getStats(), deserializedReportTrackerTypeReport.getStats() );
        // Verify Validation Report - Error Reports
        assertEquals( toSerializeReport.getValidationReport().getErrorReports().get( 0 ).getErrorMessage(),
            deserializedReport.getValidationReport().getErrorReports().get( 0 ).getErrorMessage() );
        assertEquals( toSerializeReport.getValidationReport().getErrorReports().get( 0 ).getErrorCode(),
            deserializedReport.getValidationReport().getErrorReports().get( 0 ).getErrorCode() );
        assertEquals( toSerializeReport.getValidationReport().getErrorReports().get( 0 ).getUid(),
            deserializedReport.getValidationReport().getErrorReports().get( 0 ).getUid() );
        // Verify Validation Report - Warning Reports
        assertEquals( toSerializeReport.getValidationReport().getWarningReports().get( 0 ).getWarningMessage(),
            deserializedReport.getValidationReport().getWarningReports().get( 0 ).getWarningMessage() );
        assertEquals( toSerializeReport.getValidationReport().getWarningReports().get( 0 ).getWarningCode(),
            deserializedReport.getValidationReport().getWarningReports().get( 0 ).getWarningCode() );
        assertEquals( toSerializeReport.getValidationReport().getWarningReports().get( 0 ).getUid(),
            deserializedReport.getValidationReport().getWarningReports().get( 0 ).getUid() );
        assertEquals( toSerializeReport.getValidationReport().getWarningReports().get( 0 ).getTrackerType(),
            deserializedReport.getValidationReport().getWarningReports().get( 0 ).getTrackerType() );
        // Verify TimingsStats
        assertEquals( toSerializeReport.getTimingsStats().getCommit(),
            deserializedReport.getTimingsStats().getCommit() );
        assertEquals( toSerializeReport.getTimingsStats().getPreheat(),
            deserializedReport.getTimingsStats().getPreheat() );
        assertEquals( toSerializeReport.getTimingsStats().getPrepareRequest(),
            deserializedReport.getTimingsStats().getPrepareRequest() );
        assertEquals( toSerializeReport.getTimingsStats().getProgramRule(),
            deserializedReport.getTimingsStats().getProgramRule() );
        assertEquals( toSerializeReport.getTimingsStats().getTotalImport(),
            deserializedReport.getTimingsStats().getTotalImport() );
        assertEquals( toSerializeReport.getTimingsStats().getTotalRequest(),
            deserializedReport.getTimingsStats().getTotalRequest() );
        assertEquals( toSerializeReport.getTimingsStats().getValidation(),
            deserializedReport.getTimingsStats().getValidation() );
        assertEquals( toSerializeReport.getStats(), deserializedReport.getStats() );
    }

    private void assertStats( TrackerImportReport report )
    {
        assertNotNull( report.getStats() );
        assertEquals( 1, report.getStats().getCreated() );
        assertEquals( 0, report.getStats().getUpdated() );
        assertEquals( 0, report.getStats().getDeleted() );
        assertEquals( 0, report.getStats().getIgnored() );
    }

    private TrackerImportReport createImportReport()
    {
        final Map<TrackerType, Integer> bundleSize = new HashMap<>();
        bundleSize.put( TRACKED_ENTITY, 1 );
        return TrackerImportReport.withImportCompleted( TrackerStatus.OK, createBundleReport(),
            createValidationReport(), createTimingStats(), bundleSize );
    }

    private TrackerTimingsStats createTimingStats()
    {
        TrackerTimingsStats timingsStats = new TrackerTimingsStats();
        timingsStats.set( PROGRAMRULE_OPS, "1 sec." );
        timingsStats.set( COMMIT_OPS, "2 sec." );
        timingsStats.set( PREHEAT_OPS, "3 sec." );
        timingsStats.set( VALIDATION_OPS, "4 sec." );
        timingsStats.set( TOTAL_OPS, "10 sec." );
        return timingsStats;
    }

    private TrackerValidationReport createValidationReport()
    {
        return new TrackerValidationReport();
    }

    private TrackerBundleReport createBundleReport()
    {
        TrackerBundleReport bundleReport = new TrackerBundleReport();
        TrackerTypeReport typeReport = new TrackerTypeReport( TRACKED_ENTITY );
        TrackerObjectReport objectReport = new TrackerObjectReport( TRACKED_ENTITY, "TEI_UID", 1 );
        typeReport.addObjectReport( objectReport );
        typeReport.getStats().incCreated();
        bundleReport.getTypeReportMap().put( TRACKED_ENTITY, typeReport );
        return bundleReport;
    }
}
