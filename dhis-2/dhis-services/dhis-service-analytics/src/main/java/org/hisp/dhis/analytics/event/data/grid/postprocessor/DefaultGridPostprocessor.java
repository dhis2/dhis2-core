package org.hisp.dhis.analytics.event.data.grid.postprocessor;

import org.hisp.dhis.common.Grid;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author Luciano Fiandesio
 */
@Component
public class DefaultGridPostprocessor implements GridPostprocessor
{
    private final List<ColumnProcessor> processors;

    public DefaultGridPostprocessor( List<ColumnProcessor> processors )
    {
        this.processors = processors;
    }

    @Override
    public Grid postProcess( Grid grid )
    {
        for ( ColumnProcessor processor : processors )
        {
            grid = grid.replaceAllValues( processor.process( grid.getHeaders(), grid.getRows() ) );

        }

        return grid;
    }
}
