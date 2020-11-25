package org.hisp.dhis.tracker.report;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.hisp.dhis.random.BeanRandomizer;
import org.hisp.dhis.tracker.TrackerType;
import org.junit.Test;

/**
 * @author Luciano Fiandesio
 */
public class TrackerImportReportTest
{

    private BeanRandomizer rnd = new BeanRandomizer();

    @Test
    public void testImportCalculatesIgnoredValues()
    {
        // Create Bundle report for tei and enrollment
        final Map<TrackerType, TrackerTypeReport> trackerTypeReportMap = new HashMap<>();
        trackerTypeReportMap.put( TrackerType.TRACKED_ENTITY, createTypeReport( TrackerType.TRACKED_ENTITY, 5, 3, 0 ) );
        trackerTypeReportMap.put( TrackerType.ENROLLMENT, createTypeReport( TrackerType.ENROLLMENT, 3, 3, 0 ) );
        TrackerBundleReport bundleReport = new TrackerBundleReport( TrackerStatus.OK, trackerTypeReportMap );

        // Create validation report with 3 objects
        TrackerValidationReport validationReport = TrackerValidationReport.builder()
            .errorReports( rnd.randomObjects( TrackerErrorReport.class, 3 ) )
            .build();

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