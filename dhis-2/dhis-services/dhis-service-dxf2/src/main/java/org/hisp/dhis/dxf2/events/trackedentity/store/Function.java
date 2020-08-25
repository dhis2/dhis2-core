package org.hisp.dhis.dxf2.events.trackedentity.store;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author Luciano Fiandesio
 */
@Getter
@AllArgsConstructor
public class Function implements QueryElement
{
    private String function;

    private String prefix;

    private String column;

    private String alias;

    @Override
    public String useInSelect()
    {
        return this.function + "(" + prefix + "." + column + ") as " + alias;
    }

    @Override
    public String getResultsetValue()
    {
        return alias == null ? column : alias;
    }
}
