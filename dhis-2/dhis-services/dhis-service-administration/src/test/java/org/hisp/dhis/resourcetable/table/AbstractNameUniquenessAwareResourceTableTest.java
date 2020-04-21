package org.hisp.dhis.resourcetable.table;

import static org.apache.commons.lang.StringUtils.countMatches;
import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang.RandomStringUtils;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryOptionGroupSet;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.DataDimensionType;
import org.hisp.dhis.indicator.IndicatorGroupSet;
import org.junit.Test;

public class AbstractNameUniquenessAwareResourceTableTest
{
    @Test
    public void verifyResourceTableColumnNameAreUniqueWhenComputingShortName()
    {
        // Category short name will be shorten to 49 chars, and create 3 identical
        // short-names
        String categoryName = RandomStringUtils.randomAlphabetic( 50 );

        String cogsName = RandomStringUtils.randomAlphabetic( 49 );

        final List<Category> categories = IntStream.of( 1, 2, 3 )
            .mapToObj( i -> new Category( categoryName + i, DataDimensionType.ATTRIBUTE ) )
            .peek( c -> c.setUid( CodeGenerator.generateUid() ) ).collect( Collectors.toList() );

        final List<CategoryOptionGroupSet> categoryOptionGroupSets = IntStream.of( 1, 2, 3 )
            .mapToObj( i -> new CategoryOptionGroupSet( cogsName + i ) )
            .peek( c -> c.setUid( CodeGenerator.generateUid() ) ).collect( Collectors.toList() );

        CategoryResourceTable categoryResourceTable = new CategoryResourceTable( categories, categoryOptionGroupSets );

        final String sql = categoryResourceTable.getCreateTempTableStatement();

        assertEquals( countMatches( sql, "\"" + categories.get( 0 ).getShortName() + "\"" ), 1 );
        assertEquals( countMatches( sql, "\"" + categories.get( 0 ).getShortName() + "1\"" ), 1 );
        assertEquals( countMatches( sql, "\"" + categories.get( 0 ).getShortName() + "2\"" ), 1 );

        assertEquals( countMatches( sql, "\"" + cogsName + "1\"" ), 1 );
        assertEquals( countMatches( sql, "\"" + cogsName + "2\"" ), 1 );
        assertEquals( countMatches( sql, "\"" + cogsName + "3\"" ), 1 );

    }

    @Test
    public void verifyResourceTableColumnNameAreUniqueWhenComputingShortName2()
    {
        // Category short name will be shorten to 49 chars, and create 3 identical
        // short-names
        String indicatorGroupSetName = RandomStringUtils.randomAlphabetic( 50 );

        final List<IndicatorGroupSet> indicatorGroupSets = IntStream.of( 1, 2, 3 )
            .mapToObj( i -> new IndicatorGroupSet( indicatorGroupSetName + 1 ) )
            .peek( c -> c.setUid( CodeGenerator.generateUid() ) ).collect( Collectors.toList() );

        IndicatorGroupSetResourceTable indicatorGroupSetResourceTable = new IndicatorGroupSetResourceTable(
            indicatorGroupSets );

        final String sql = indicatorGroupSetResourceTable.getCreateTempTableStatement();

        assertEquals( countMatches( sql, "\"" + indicatorGroupSets.get( 0 ).getName() + "\"" ), 1 );
        assertEquals( countMatches( sql, "\"" + indicatorGroupSets.get( 0 ).getName() + "1\"" ), 1 );
        assertEquals( countMatches( sql, "\"" + indicatorGroupSets.get( 0 ).getName() + "2\"" ), 1 );

    }
}