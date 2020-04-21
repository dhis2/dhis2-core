package org.hisp.dhis.resourcetable.table;

import static org.hisp.dhis.system.util.SqlUtils.quote;

import java.util.ArrayList;
import java.util.List;

import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.resourcetable.ResourceTable;

/**
 * This class can be extended by Resource Table generation classes that need to
 * generate unique colum names
 *
 */
public abstract class AbstractNameUniquenessAwareResourceTable<T>
    extends
    ResourceTable<T>
{
    protected List<String> uniqueColumnNames = new ArrayList<>();

    public AbstractNameUniquenessAwareResourceTable( List<T> objects )
    {
        super( objects );
    }

    /**
     * Returns the short name in quotes for the given {@see BaseDimensionalObject}, ensuring
     * that the short name is unique across the list of BaseDimensionalObject this
     * class operates on
     *
     * @param baseDimensionalObject a {@see BaseDimensionalObject}
     * @return a unique, quoted short name
     */
    protected String ensureUniqueShortName( BaseDimensionalObject baseDimensionalObject )
    {
        String columnName = quote( baseDimensionalObject.getShortName()
            + (uniqueColumnNames.contains( baseDimensionalObject.getShortName() ) ? uniqueColumnNames.size() : "") );

        this.uniqueColumnNames.add( baseDimensionalObject.getShortName() );

        return columnName;
    }

    /**
     * Returns the name in quotes, ensuring
     * that the name is unique across the list of objects this class operates on
     *
     * @param name a String
     * @return a unique, quoted name
     */
    protected String ensureUniqueName( String name )
    {
        String columnName = quote( name + (uniqueColumnNames.contains( name ) ? uniqueColumnNames.size() : "") );

        this.uniqueColumnNames.add( name );

        return columnName;
    }
}
