package org.hisp.dhis.dxf2.events.importer.shared.preprocess;

import com.google.common.collect.Sets;
import org.hamcrest.Matchers;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.event.DataValue;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.importer.context.WorkContext;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hisp.dhis.dxf2.events.event.EventUtils.FALLBACK_USERNAME;

/**
 * @author Luciano Fiandesio
 */
public class EventStoredByPreProcessorTest
{
    private EventStoredByPreProcessor preProcessor;

    @Before
    public void setUp()
    {
        preProcessor = new EventStoredByPreProcessor();
    }

    @Test
    public void t1()
    {
        WorkContext ctx = WorkContext.builder()
            .importOptions( ImportOptions.getDefaultImportOptions() )
            .build();
        Event event = new Event();
        event.setDataValues( Sets.newHashSet( new DataValue( "aaa", "one" ), new DataValue( "bbb", "two" ) ) );
        preProcessor.process( event, ctx );

        assertThat( event.getStoredBy(), is( FALLBACK_USERNAME ) );
        assertThat( event.getDataValues(),
            hasItems( allOf( Matchers.hasProperty( "storedBy", is( FALLBACK_USERNAME ) ) ) ) );
    }
}