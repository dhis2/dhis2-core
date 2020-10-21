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

import static org.junit.Assert.*;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.tracker.TrackerBundleReportMode;
import org.hisp.dhis.tracker.TrackerImportService;
import org.hisp.dhis.tracker.TrackerType;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class TrackerBundleImportReportTest extends DhisSpringTest
{
    @Autowired
    private TrackerImportService trackerImportService;

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
