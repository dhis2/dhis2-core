package org.hisp.dhis.dxf2.events.trackedentity.store;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author Luciano Fiandesio
 */
@Getter
@AllArgsConstructor
public class Subselect implements QueryElement
{
    private String query;

    private String alias;

    @Override
    public String useInSelect()
    {
        return query + " as " + alias;
    }

    @Override
    public String getResultsetValue()
    {
        return alias;
    }
}
