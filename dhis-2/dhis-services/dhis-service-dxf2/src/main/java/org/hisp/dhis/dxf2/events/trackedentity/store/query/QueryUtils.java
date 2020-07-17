package org.hisp.dhis.dxf2.events.trackedentity.store.query;

import java.util.Collection;
import java.util.stream.Collectors;

import org.hisp.dhis.dxf2.events.trackedentity.store.TableColumn;

/**
 * @author Luciano Fiandesio
 */
public class QueryUtils
{
    static String getSelect( Collection<TableColumn> columns )
    {
        return "SELECT "
            + columns.stream().map( TableColumn::useInSelect ).collect( Collectors.joining( ", " ) ) + " ";
    }
}
