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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.hisp.dhis.random.BeanRandomizer;
import org.hisp.dhis.tracker.TrackerType;
import org.junit.jupiter.api.Test;

/**
 * @author Luciano Fiandesio
 */
class TrackerImportReportTest
{

    private final BeanRandomizer rnd = BeanRandomizer.create();

    @Test
    void testImportCalculatesIgnoredValues()
    {
        // Create Bundle report for tei and enrollment
        final Map<TrackerType, TrackerTypeReport> trackerTypeReportMap = new HashMap<>();
        trackerTypeReportMap.put( TrackerType.TRACKED_ENTITY, createTypeReport( TrackerType.TRACKED_ENTITY, 5, 3, 0 ) );
        trackerTypeReportMap.put( TrackerType.ENROLLMENT, createTypeReport( TrackerType.ENROLLMENT, 3, 3, 0 ) );
        TrackerBundleReport bundleReport = new TrackerBundleReport( TrackerStatus.OK, trackerTypeReportMap );
        // Create validation report with 3 objects
        TrackerValidationReport validationReport = TrackerValidationReport.builder()
            .errorReports( rnd.objects( TrackerErrorReport.class, 3 ).collect( Collectors.toList() ) ).build();
        // Create empty Timing Stats report
        TrackerTimingsStats timingsStats = new TrackerTimingsStats();
        // Create payload map
        Map<TrackerType, Integer> originalPayload = new HashMap<>();
        originalPayload.put( TrackerType.TRACKED_ENTITY, 10 );
        originalPayload.put( TrackerType.ENROLLMENT, 8 );
        // Method under test
        TrackerImportReport rep = TrackerImportReport.withImportCompleted( TrackerStatus.OK, bundleReport,
            validationReport, timingsStats, originalPayload );
        assertThat( rep.getStats().getCreated(), is( 8 ) );
        assertThat( rep.getStats().getUpdated(), is( 6 ) );
        assertThat( rep.getStats().getIgnored(), is( 3 ) );
        assertThat( rep.getStats().getDeleted(), is( 0 ) );
        assertThat( getBundleReportStats( rep, TrackerType.TRACKED_ENTITY ).getCreated(), is( 5 ) );
        assertThat( getBundleReportStats( rep, TrackerType.TRACKED_ENTITY ).getUpdated(), is( 3 ) );
        assertThat( getBundleReportStats( rep, TrackerType.TRACKED_ENTITY ).getDeleted(), is( 0 ) );
        assertThat( getBundleReportStats( rep, TrackerType.TRACKED_ENTITY ).getIgnored(), is( 2 ) );
        assertThat( getBundleReportStats( rep, TrackerType.ENROLLMENT ).getCreated(), is( 3 ) );
        assertThat( getBundleReportStats( rep, TrackerType.ENROLLMENT ).getUpdated(), is( 3 ) );
        assertThat( getBundleReportStats( rep, TrackerType.ENROLLMENT ).getDeleted(), is( 0 ) );
        assertThat( getBundleReportStats( rep, TrackerType.ENROLLMENT ).getIgnored(), is( 2 ) );
    }

    private TrackerStats getBundleReportStats( TrackerImportReport importReport, TrackerType type )
    {
        return importReport.getBundleReport().getTypeReportMap().get( type ).getStats();
    }

    private TrackerTypeReport createTypeReport( TrackerType type, int created, int updated, int deleted )
    {
        final TrackerStats teiStats = new TrackerStats();
        teiStats.setCreated( created );
        teiStats.setUpdated( updated );
        teiStats.setDeleted( deleted );
        return new TrackerTypeReport( type, teiStats, new ArrayList<>(), new ArrayList<>() );
    }
}
