package org.hisp.dhis.dxf2.datavalueset;

import lombok.AllArgsConstructor;

/**
 * Simple implementation of {@link DataValueSetReader} which wraps an in-memory
 * {@link DataValueSet}.
 *
 * @author Lars Helge Overland
 */
@AllArgsConstructor
public class SimpleDataValueSetReader implements DataValueSetReader
{
    private DataValueSet dataValueSet;

    @Override
    public DataValueSet readHeader()
    {
        return dataValueSet;
    }

    @Override
    public DataValueEntry readNext()
    {
        return null;
    }

    @Override
    public void close()
    {
    }
}
