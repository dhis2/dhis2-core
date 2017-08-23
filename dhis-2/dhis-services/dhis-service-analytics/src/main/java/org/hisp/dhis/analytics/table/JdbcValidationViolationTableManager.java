package org.hisp.dhis.analytics.table;

import com.google.common.collect.Lists;
import org.hisp.dhis.analytics.AnalyticsTable;
import org.hisp.dhis.analytics.AnalyticsTableColumn;
import org.hisp.dhis.dataelement.DataElementCategory;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;

/**
 * @author Henning Håkonsen
 */
public class JdbcValidationViolationTableManager extends AbstractJdbcTableManager
{
    @Autowired
    PeriodService periodService;

    @Override
    protected void populateTable( AnalyticsTable table )
    {

    }

    @Override
    public AnalyticsTableType getAnalyticsTableType()
    {
        return AnalyticsTableType.VALIDATION_VIOLATION;
    }

    @Override
    public List<AnalyticsTable> getTables( Date earliest )
    {
        return null;
    }

    @Override
    public Set<String> getExistingDatabaseTables()
    {
        return null;
    }

    @Override
    public String validState()
    {
        // TODO verify
        boolean hasData = jdbcTemplate.queryForRowSet( "select datasetid from validationviolation limit 1" ).next();

        if ( !hasData )
        {
            return "No validation violation rules exist, not updating validation violation analytics tables";
        }

        return null;
    }

    @Override
    public void createTable( AnalyticsTable table )
    {

    }

    @Override
    public Future<?> applyAggregationLevels( ConcurrentLinkedQueue<AnalyticsTable> tables,
        Collection<String> dataElements, int aggregationLevel )
    {
        return null;
    }

    @Override
    public Future<?> vacuumTablesAsync( ConcurrentLinkedQueue<AnalyticsTable> tables )
    {
        return null;
    }

    @Override
    protected List<AnalyticsTableColumn> getDimensionColumns( AnalyticsTable table )
    {
        List<AnalyticsTableColumn> columns = new ArrayList<>();

        List<OrganisationUnitGroupSet> orgUnitGroupSets =
            idObjectManager.getDataDimensionsNoAcl( OrganisationUnitGroupSet.class );

        List<DataElementCategory> attributeCategories =
            categoryService.getAttributeDataDimensionCategoriesNoAcl();

        for ( OrganisationUnitGroupSet groupSet : orgUnitGroupSets )
        {
            columns.add( new AnalyticsTableColumn( quote( groupSet.getUid() ), "character(11)", "ougs." + quote( groupSet.getUid() ), groupSet.getCreated() ) );
        }

        for ( DataElementCategory category : attributeCategories )
        {
            columns.add( new AnalyticsTableColumn( quote( category.getUid() ), "character(11)", "acs." + quote( category.getUid() ), category.getCreated() ) );
        }

        for ( PeriodType periodType : PeriodType.getAvailablePeriodTypes() )
        {
            String column = quote( periodType.getName().toLowerCase() );
            columns.add( new AnalyticsTableColumn( column, "character varying(15)", "ps." + column ) );
        }

        String timelyDateDiff = statementBuilder.getDaysBetweenDates( "pe.enddate", statementBuilder.getCastToDate( "cdr.date" ) );
        String timelyAlias = "(select (" + timelyDateDiff + ") <= ds.timelydays) as timely";

        AnalyticsTableColumn tm = new AnalyticsTableColumn( quote( "timely" ), "boolean", timelyAlias );

        AnalyticsTableColumn ds = new AnalyticsTableColumn( quote( "dx" ), "character(11) not null", "ds.uid" );

        columns.addAll( Lists.newArrayList( ds, tm ) );


        return filterDimensionColumns( columns );
    }
}
