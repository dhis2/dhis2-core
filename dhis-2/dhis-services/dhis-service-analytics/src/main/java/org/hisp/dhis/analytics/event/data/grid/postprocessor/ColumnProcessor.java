package org.hisp.dhis.analytics.event.data.grid.postprocessor;

import java.util.List;

import org.hisp.dhis.common.GridHeader;

/**
 * A ColumnProcessor is responsible for modifying the data of the column of a
 * {@see Grid} object.
 * 
 * Instances of this interface are invoked by a {@GridPostprocessor} in order to
 * post-process a Grid values.
 * 
 * @author Luciano Fiandesio
 */
public interface ColumnProcessor
{
    List<List<Object>> process( List<GridHeader> headers, List<List<Object>> rows );
}
