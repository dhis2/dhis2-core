package org.hisp.dhis.db.model;

import java.util.List;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Represents a database index.
 * 
 * @author Lars Helge Overland
 */
@Getter
@RequiredArgsConstructor
public class Index
{
    /**
     * Index name.
     */
    @NonNull
    private final String name;

    /**
     * Index type, defaults to {@link IndexType.BTREE}.
     */
    @NonNull
    private final IndexType indexType = IndexType.BTREE;

    /**
     * The name of the table to apply the index to.
     */
    @NonNull
    private final String tableName;

    /**
     * Index uniqueness constraint.
     */
    private final boolean unique;

    /**
     * Index column names.
     */
    @NonNull
    private final List<String> columns;
}
