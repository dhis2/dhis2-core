package org.hisp.dhis.db.model;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
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
    private final String name;

    /**
     * Index type, defaults to {@link IndexType.BTREE}.
     */
    private final IndexType indexType = IndexType.BTREE;

    /**
     * The name of the table to apply the index to.
     */
    private final String tableName;

    /**
     * Index uniqueness constraint.
     */
    private final boolean unique;

    /**
     * Index column names.
     */
    private final List<String> columns = new ArrayList<>();
}
