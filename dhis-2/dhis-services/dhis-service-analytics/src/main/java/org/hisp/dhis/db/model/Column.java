package org.hisp.dhis.db.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class Column
{
    private final String name;

    private final DataType dataType;

    private final boolean notNull = false;

}
