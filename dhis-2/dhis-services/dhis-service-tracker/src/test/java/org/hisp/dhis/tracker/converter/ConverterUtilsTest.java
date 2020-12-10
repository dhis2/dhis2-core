package org.hisp.dhis.tracker.converter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.hisp.dhis.tracker.domain.Event;
import org.junit.Test;

/**
 * @author Luciano Fiandesio
 */
public class ConverterUtilsTest
{

    @Test
    public void verifyPatchFieldsOnEmptyEntity()
    {
        final List<Field> patchFields = ConverterUtils.getPatchFields( Event.class, new Event() );
        assertThat( patchFields, hasSize( 1 ) );
        assertThat( patchFields.get( 0 ).getName(), is( "deleted" ) );
    }

    @Test
    public void verifyPatchFieldsSkipsCollectionsAndEmptyValues()
    {
        Event event = new Event();
        event.setFollowUp( true );
        event.setEvent( "abcde" );
        event.setProgram( "prog1" );
        event.setOrgUnit( "" );
        event.setNotes( new ArrayList<>() );
        final List<Field> patchFields = ConverterUtils.getPatchFields( Event.class, event );
        assertThat( patchFields, hasSize( 4 ) );
        List<String> fields = patchFields.stream().map( Field::getName ).collect( Collectors.toList() );
        assertThat( fields, containsInAnyOrder( "deleted", "event", "program", "followUp" ) );
    }

}