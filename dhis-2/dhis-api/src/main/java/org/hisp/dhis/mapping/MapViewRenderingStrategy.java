package org.hisp.dhis.mapping;

/**
 * Enumeration defining how to render a map layer, i.e. whether to
 * render one map (default), one map per period, or render the map
 * as a time line where each period is rendered sequentially.
 *
 * @author Lars Helge Overland
 */
public enum MapViewRenderingStrategy
{
    SINGLE,
    SPLIT_BY_PERIOD,
    TIMELINE
}
