package org.hisp.dhis.dxf2.events.trackedentity.store.query;

import java.util.Collection;
import java.util.stream.Collectors;

import org.hisp.dhis.dxf2.events.trackedentity.store.QueryElement;

/**
 * @author Luciano Fiandesio
 */
public class QueryUtils
{
    static String getSelect( Collection<? extends QueryElement> columns )
    {
        return "SELECT "
            + columns.stream().map( QueryElement::useInSelect ).collect( Collectors.joining( ", " ) ) + " ";
    }
}
