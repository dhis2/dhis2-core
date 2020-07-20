package org.hisp.dhis.dxf2.events.trackedentity.store;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author Luciano Fiandesio
 */
@Getter
@AllArgsConstructor
public class TableColumn implements QueryElement
{
    private String prefix;

    private String column;

    private String alias;

    public TableColumn( String prefix, String column )
    {
        this.prefix = prefix;
        this.column = column;
    }

    @Override
    public String useInSelect()
    {
        return prefix + "." + column + (alias == null ? "" : " as " + alias);
    }

    @Override
    public String getResultsetValue()
    {
        return alias == null ? column : alias;
    }
}
