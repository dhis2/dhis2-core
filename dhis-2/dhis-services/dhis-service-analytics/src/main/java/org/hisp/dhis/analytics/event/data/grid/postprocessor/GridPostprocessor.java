package org.hisp.dhis.analytics.event.data.grid.postprocessor;

import org.hisp.dhis.common.Grid;

/**
 * Process a {@see Grid} through Grid post-processors in order to replace or
 * correct data, for instance replacing a UID with a name. The GridPostprocessor
 * is designed to be invoked at the very end of the Grid population process
 * (that is, when all data has been aggregated and the Grid is ready to be
 * returned).
 * 
 * @author Luciano Fiandesio
 */
public interface GridPostprocessor
{
    /**
     * Post-process the provided Grid by passing it to a list of
     * {@see ColumnProcessor}. Each processor is responsible for modifying the values of one column of the
     * Grid.
     * 
     * @param grid a {@see Grid}
     * @return a {@see Grid
     */
    Grid postProcess( Grid grid );
}
