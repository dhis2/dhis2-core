package org.hisp.dhis.dxf2.events.trackedentity.store;

/**
 * @author Luciano Fiandesio
 */
public interface QueryElement
{
    String useInSelect();

    String getResultsetValue();
}
