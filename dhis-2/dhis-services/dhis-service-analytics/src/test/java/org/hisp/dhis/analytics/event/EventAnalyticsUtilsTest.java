package org.hisp.dhis.analytics.event;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.analytics.EventReportDimensionalItem;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.system.grid.ListGrid;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.junit.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.hisp.dhis.common.DimensionalObject.DIMENSION_SEP;
import static org.junit.Assert.assertEquals;

/**
 * @author Henning Håkonsen
 */
public class EventAnalyticsUtilsTest
    extends DhisConvenienceTest
{
    @Test
    public void testGetAggregatedDataValueMapping()
    {
        Grid grid = new ListGrid();

        grid.addRow();
        grid.addValue( "de1" );
        grid.addValue( "ou2" );
        grid.addValue( "pe1" );
        grid.addValue( 3 );

        grid.addRow();
        grid.addValue( "de2" );
        grid.addValue( "ou3" );
        grid.addValue( "pe2" );
        grid.addValue( 5 );

        Map<String, Object> map = EventAnalyticsUtils.getAggregatedEventDataMapping( grid );

        assertEquals( 3, map.get( "de1" + DIMENSION_SEP + "ou2" + DIMENSION_SEP + "pe1" ) );
        assertEquals( 5, map.get( "de2" + DIMENSION_SEP + "ou3" + DIMENSION_SEP + "pe2" ) );
    }

    @Test
    public void testGenerateEventDataPermutations()
    {
        Map<String, List<EventReportDimensionalItem>> tableRows = new LinkedHashMap<>();

        DataElement deA = createDataElement( 'A' );
        deA.setValueType( ValueType.BOOLEAN );

        TrackedEntityAttribute trackedEntityAttribute = createTrackedEntityAttribute( 'B' );
        OptionSet optionSet = new OptionSet( );
        optionSet.addOption( new Option( "name", "code" ) );
        trackedEntityAttribute.setOptionSet( optionSet );

        List<EventReportDimensionalItem> objects = new ArrayList<>( );
        Grid grid = new ListGrid( );

        EventDataItem edi1 = new EventDataItem( deA, null );
        grid.addMetaData( edi1.getParentUid(), edi1 );

        try
        {
            EventAnalyticsUtils.addEventReportDimensionalItems( edi1, objects, grid, edi1.getParentUid() );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }

        tableRows.put( edi1.getParentUid(), objects );

        EventDataItem edi2 = new EventDataItem( null, trackedEntityAttribute );
        grid.addMetaData( edi2.getParentUid(), edi2 );

        try
        {
            EventAnalyticsUtils.addEventReportDimensionalItems( edi2, objects, grid, edi2.getParentUid() );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }

        tableRows.put( edi2.getParentUid(), objects );

        List<Map<String, EventReportDimensionalItem>> rowPermutations = EventAnalyticsUtils.generateEventDataPermutations( tableRows );

        assertEquals( 9, rowPermutations.size() );
    }
}
