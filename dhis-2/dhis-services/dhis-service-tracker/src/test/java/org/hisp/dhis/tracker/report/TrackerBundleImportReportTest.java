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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.tracker.TrackerBundleReportMode;
import org.hisp.dhis.tracker.TrackerImportService;
import org.hisp.dhis.tracker.TrackerType;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class TrackerBundleImportReportTest extends DhisSpringTest
{
    @Autowired
    private TrackerImportService trackerImportService;
    
    @Autowired
    private ObjectMapper jsonMapper;

    @Test
    public void testImportReportBasic()
    {
        TrackerImportReport report = trackerImportService.buildImportReport( createImportReport(), TrackerBundleReportMode.BASIC );

        assertEquals( TrackerStatus.OK, report.getStatus() );
        assertStats( report );
        assertNull( report.getTrackerValidationReport() );
        assertNull( report.getTimings() );
    }

    @Test
    public void testImportReportErrors()
    {
        TrackerImportReport report = trackerImportService.buildImportReport( createImportReport(), TrackerBundleReportMode.ERRORS );

        assertEquals( TrackerStatus.OK, report.getStatus() );
        assertStats( report );
        assertNotNull( report.getTrackerValidationReport() );
        assertNull( report.getTrackerValidationReport().getPerformanceReport() );
        assertNull( report.getTrackerValidationReport().getWarningReports() );
        assertNotNull( report.getTrackerValidationReport().getErrorReports() );
        assertNull( report.getTimings() );
    }

    @Test
    public void testImportReportWarnings()
    {
        TrackerImportReport report = trackerImportService.buildImportReport( createImportReport(), TrackerBundleReportMode.WARNINGS );

        assertEquals( TrackerStatus.OK, report.getStatus() );
        assertStats( report );
        assertNotNull( report.getTrackerValidationReport() );
        assertNull( report.getTrackerValidationReport().getPerformanceReport() );
        assertNotNull( report.getTrackerValidationReport().getErrorReports() );
        assertNotNull( report.getTrackerValidationReport().getWarningReports() );
        assertNull( report.getTimings() );
    }

    @Test
    public void testImportReportFull()
    {
        TrackerImportReport report = trackerImportService.buildImportReport( createImportReport(), TrackerBundleReportMode.FULL );

        assertEquals( TrackerStatus.OK, report.getStatus() );
        assertStats( report );
        assertNotNull( report.getTrackerValidationReport() );
        assertNotNull( report.getTrackerValidationReport().getPerformanceReport() );
        assertNotNull( report.getTrackerValidationReport().getErrorReports() );
        assertNotNull( report.getTrackerValidationReport().getWarningReports() );
        assertNotNull( report.getTimings() );
        assertEquals("1 sec.", report.getTimings().getProgramrule() );
        assertEquals("2 sec.", report.getTimings().getCommit() );
        assertEquals("3 sec.", report.getTimings().getPreheat() );
        assertEquals("4 sec.", report.getTimings().getValidation() );
        assertEquals("10 sec.", report.getTimings().getTotalImport() );
    }
    
    @Test
    public void testSerializingAndDeserializingImportReport()
        throws JsonMappingException,
        JsonProcessingException
    {
        TrackerImportReport toSerializeReport = new TrackerImportReport();
        toSerializeReport.getBundleReport().setStatus( TrackerStatus.ERROR );
        Map<TrackerType, TrackerTypeReport> typeReportMap = new HashMap<>();
        TrackerTypeReport typeReport = new TrackerTypeReport( TrackerType.TRACKED_ENTITY );
        TrackerObjectReport trackerObjectReport = new TrackerObjectReport( TrackerType.TRACKED_ENTITY );
        List<TrackerErrorReport> trackerErrorReports = new ArrayList<>();
        TrackerErrorReport errorReport1 = new TrackerErrorReport( "Could not find OrganisationUnit: ``, linked to Tracked Entity.", TrackerErrorCode.E1049,
            TrackerType.TRACKED_ENTITY, "BltTZV9HvEZ" );
        TrackerErrorReport errorReport2 = new TrackerErrorReport( "Could not find TrackedEntityType: `Q9GufDoplCL`.", TrackerErrorCode.E1049,
            TrackerType.TRACKED_ENTITY, "BltTZV9HvEZ" );
        trackerErrorReports.add( errorReport1 );
        trackerErrorReports.add( errorReport2 );
        trackerObjectReport.getErrorReports().addAll( trackerErrorReports );
        trackerObjectReport.setIndex( 0 );
        trackerObjectReport.setUid( "BltTZV9HvEZ" );

        typeReport.addObjectReport( trackerObjectReport );
        typeReport.getStats().setCreated( 1 );
        typeReport.getStats().setUpdated( 2 );
        typeReport.getStats().setDeleted( 3 );
        typeReportMap.put( TrackerType.TRACKED_ENTITY, typeReport );
        toSerializeReport.getBundleReport().setTypeReportMap( typeReportMap );

        TrackerTimingsStats timingStats = new TrackerTimingsStats();
        timingStats.setCommit( "0.1 sec." );
        timingStats.setPreheat( "0.2 sec." );
        timingStats.setProgramrule( "0.3 sec." );
        timingStats.setTotalImport( "0.4 sec." );
        timingStats.setPrepareRequest( "0.5 sec." );
        timingStats.setTotalRequest( "0.6 sec." );

        toSerializeReport.setTimings( timingStats );

        TrackerValidationReport tvr = toSerializeReport.getTrackerValidationReport();

        tvr.getErrorReports().add( new TrackerErrorReport( "Could not find OrganisationUnit: ``, linked to Tracked Entity.", TrackerErrorCode.E1049,
            TrackerType.TRACKED_ENTITY, "BltTZV9HvEZ" ) );
        toSerializeReport.setTrackerValidationReport( tvr );
        toSerializeReport.setStatus( TrackerStatus.ERROR );

        String jsonString = jsonMapper.writeValueAsString( toSerializeReport );

        TrackerImportReport deserializedReport = jsonMapper.readValue( jsonString, TrackerImportReport.class );

        assertEquals( toSerializeReport.getStats().getIgnored(), deserializedReport.getStats().getIgnored() );
        assertEquals( toSerializeReport.getStats().getDeleted(), deserializedReport.getStats().getDeleted() );
        assertEquals( toSerializeReport.getStats().getUpdated(), deserializedReport.getStats().getUpdated() );
        assertEquals( toSerializeReport.getStats().getCreated(), deserializedReport.getStats().getCreated() );
        assertEquals( toSerializeReport.getStats().getTotal(), deserializedReport.getStats().getTotal() );

        assertEquals( toSerializeReport.getBundleReport().getStatus(), deserializedReport.getBundleReport().getStatus() );

        assertEquals( toSerializeReport.getBundleReport().getStats().getIgnored(), deserializedReport.getBundleReport().getStats().getIgnored() );
        assertEquals( toSerializeReport.getBundleReport().getStats().getDeleted(), deserializedReport.getBundleReport().getStats().getDeleted() );
        assertEquals( toSerializeReport.getBundleReport().getStats().getUpdated(), deserializedReport.getBundleReport().getStats().getUpdated() );
        assertEquals( toSerializeReport.getBundleReport().getStats().getCreated(), deserializedReport.getBundleReport().getStats().getCreated() );
        assertEquals( toSerializeReport.getBundleReport().getStats().getTotal(), deserializedReport.getBundleReport().getStats().getTotal() );

        assertEquals( toSerializeReport.getBundleReport().getTypeReportMap().get( TrackerType.TRACKED_ENTITY ),
            deserializedReport.getBundleReport().getTypeReportMap().get( TrackerType.TRACKED_ENTITY ) );

        assertEquals( toSerializeReport.getTrackerValidationReport().getErrorReports().get( 0 ).getErrorMessage(),
            deserializedReport.getTrackerValidationReport().getErrorReports().get( 0 ).getErrorMessage() );
        assertEquals( toSerializeReport.getTrackerValidationReport().getErrorReports().get( 0 ).getErrorCode(),
            deserializedReport.getTrackerValidationReport().getErrorReports().get( 0 ).getErrorCode() );
        assertEquals( toSerializeReport.getTrackerValidationReport().getErrorReports().get( 0 ).getUid(),
            deserializedReport.getTrackerValidationReport().getErrorReports().get( 0 ).getUid() );

        assertEquals( toSerializeReport.getTimings().getCommit(), deserializedReport.getTimings().getCommit() );
        assertEquals( toSerializeReport.getTimings().getPreheat(), deserializedReport.getTimings().getPreheat() );
        assertEquals( toSerializeReport.getTimings().getPrepareRequest(), deserializedReport.getTimings().getPrepareRequest() );
        assertEquals( toSerializeReport.getTimings().getProgramrule(), deserializedReport.getTimings().getProgramrule() );
        assertEquals( toSerializeReport.getTimings().getTotalImport(), deserializedReport.getTimings().getTotalImport() );
        assertEquals( toSerializeReport.getTimings().getTotalRequest(), deserializedReport.getTimings().getTotalRequest() );
        assertEquals( toSerializeReport.getTimings().getValidation(), deserializedReport.getTimings().getValidation() );

        assertEquals( toSerializeReport.getStats(), deserializedReport.getStats() );

    }

    private void assertStats( TrackerImportReport report) {
        assertNotNull( report.getStats() );
        assertEquals(1, report.getStats().getCreated() );
        assertEquals(0, report.getStats().getUpdated() );
        assertEquals(0, report.getStats().getDeleted() );
        assertEquals(0, report.getStats().getIgnored() );
    }

    private TrackerImportReport createImportReport(){
        TrackerImportReport importReport = new TrackerImportReport();

        importReport.setStatus( TrackerStatus.OK );
        importReport.setTimings( createTimingStats() );
        importReport.setTrackerValidationReport( createValidationReport() );
        importReport.setBundleReport( createBundleReport() );
        return importReport;
    }

    private TrackerTimingsStats createTimingStats() {
        TrackerTimingsStats timingsStats = new TrackerTimingsStats();
        timingsStats.setProgramrule("1 sec.");
        timingsStats.setCommit("2 sec.");
        timingsStats.setPreheat("3 sec.");
        timingsStats.setValidation("4 sec.");
        timingsStats.setTotalImport("10 sec.");

        return timingsStats;
    }

    private TrackerValidationReport createValidationReport() {
        TrackerValidationReport validationReport = new TrackerValidationReport();

        return validationReport;
    }

    private TrackerBundleReport createBundleReport() {
        TrackerBundleReport bundleReport = new TrackerBundleReport();
        TrackerTypeReport typeReport = new TrackerTypeReport( TrackerType.TRACKED_ENTITY );
        TrackerObjectReport objectReport = new TrackerObjectReport( TrackerType.TRACKED_ENTITY, "TEI_UID", 1 );
        typeReport.addObjectReport( objectReport );
        typeReport.getStats().incCreated();
        bundleReport.getTypeReportMap().put( TrackerType.TRACKED_ENTITY, typeReport);

        return bundleReport;
    }
}
