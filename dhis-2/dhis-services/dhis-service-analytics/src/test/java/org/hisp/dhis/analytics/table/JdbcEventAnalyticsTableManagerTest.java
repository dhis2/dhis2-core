/*
 * Copyright (c) 2004-2019, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.hisp.dhis.analytics.table;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.hisp.dhis.analytics.*;
import org.hisp.dhis.analytics.partition.PartitionManager;
import org.hisp.dhis.analytics.util.AnalyticsTableAsserter;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOptionGroupSet;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataapproval.DataApprovalLevelService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.organisationunit.OrganisationUnitLevel;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.resourcetable.ResourceTableService;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.database.DatabaseInfo;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.*;
import static org.hisp.dhis.DhisConvenienceTest.*;
import static org.hisp.dhis.analytics.ColumnDataType.*;
import static org.hisp.dhis.analytics.ColumnDataType.TEXT;
import static org.hisp.dhis.analytics.table.AbstractJdbcTableManager.PREFIX_ORGUNITLEVEL;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.quote;
import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Luciano Fiandesio
 */
public class JdbcEventAnalyticsTableManagerTest
{
    @Mock
    private IdentifiableObjectManager idObjectManager;

    @Mock
    private OrganisationUnitService organisationUnitService;

    @Mock
    private CategoryService categoryService;

    @Mock
    private SystemSettingManager systemSettingManager;

    @Mock
    private DataApprovalLevelService dataApprovalLevelService;

    @Mock
    private ResourceTableService resourceTableService;

    @Mock
    private AnalyticsTableHookService tableHookService;

    @Mock
    private StatementBuilder statementBuilder;

    @Mock
    private PartitionManager partitionManager;

    @Mock
    private DatabaseInfo databaseInfo;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @InjectMocks
    private JdbcEventAnalyticsTableManager subject;

    private final static String FROM_CLAUSE = "from programstageinstance where programstageinstanceid=psi.programstageinstanceid";

    private List<AnalyticsTableColumn> periodColumns = PeriodType.getAvailablePeriodTypes().stream().map( pt -> {
        String column = quote( pt.getName().toLowerCase() );
        return new AnalyticsTableColumn( column, TEXT, "dps" + "." + column );
    }).collect(Collectors.toList());

    @Before
    public void setUp()
    {
        when( jdbcTemplate.queryForList(
            "select distinct(extract(year from psi.executiondate)) from programstageinstance psi inner join programinstance pi on psi.programinstanceid = pi.programinstanceid where pi.programid = 0 and psi.executiondate is not null and psi.deleted is false and psi.executiondate >= '2018-01-01'",
            Integer.class ) ).thenReturn( Lists.newArrayList( 2018, 2019 ) );
    }

    @Test
    public void verifyTableType()
    {
        assertThat( subject.getAnalyticsTableType(), is( AnalyticsTableType.EVENT ) );
    }

    @Test
    public void verifyGetTableWithCategoryCombo()
    {
        Program p1 = createProgram( 'A' );

        Category categoryA = createCategory( 'A' );
        categoryA.setCreated( getDate( 2019, 12, 3 ) );
        Category categoryB = createCategory( 'B' );
        categoryA.setCreated( getDate( 2018, 8, 5 ) );
        CategoryCombo categoryCombo = createCategoryCombo( 'B', categoryA, categoryB );

        addCategoryCombo( p1, categoryCombo );

        when( idObjectManager.getAllNoAcl( Program.class ) ).thenReturn( Lists.newArrayList( p1 ) );

        AnalyticsTableUpdateParams params = AnalyticsTableUpdateParams.newBuilder().withLastYears( 2 ).build();
        List<AnalyticsTable> tables = subject.getAnalyticsTables( params );

        assertThat( tables, hasSize( 1 ) );

        new AnalyticsTableAsserter.Builder( tables.get( 0 ) ).withTableType( AnalyticsTableType.EVENT )
            .withColumnSize( 41 ).withDefaultColumns( subject.getDefaultColumns() )
            .addColumns(periodColumns)
            .addColumn( categoryA.getUid(), CHARACTER_11, "acs.", categoryA.getCreated() )
            .addColumn( categoryB.getUid(), CHARACTER_11, "acs.", categoryB.getCreated() ).build()
            .verify();

    }

    @Test
    public void verifyGetTableWithOuLevel()
    {
        Program p1 = createProgram( 'A' );

        when( idObjectManager.getAllNoAcl( Program.class ) ).thenReturn( Lists.newArrayList( p1 ) );

        OrganisationUnitLevel ouLevel = new OrganisationUnitLevel( 1, "C" );
        ouLevel.setCreated( getDate( 2018, 8, 5 ) );
        when( organisationUnitService.getFilledOrganisationUnitLevels() ).thenReturn( Lists.newArrayList( ouLevel ) );

        AnalyticsTableUpdateParams params = AnalyticsTableUpdateParams.newBuilder().withLastYears( 2 ).build();

        List<AnalyticsTable> tables = subject.getAnalyticsTables( params );

        assertThat( tables, hasSize( 1 ) );

        new AnalyticsTableAsserter.Builder( tables.get( 0 ) )
            .withTableType( AnalyticsTableType.EVENT )
            .withColumnSize( 40 )
            .withDefaultColumns( subject.getDefaultColumns() )
            .addColumns(periodColumns)
            .addColumn( PREFIX_ORGUNITLEVEL + ouLevel.getLevel(), CHARACTER_11, "ous.", ouLevel.getCreated() )
            .build().verify();

    }

    @Test
    public void verifyGetTableWithOuGroupSet()
    {
        Program p1 = createProgram( 'A' );

        when( idObjectManager.getAllNoAcl( Program.class ) ).thenReturn( Lists.newArrayList( p1 ) );

        OrganisationUnitGroupSet ouGroupSetA = createOrganisationUnitGroupSet( 'A' );
        ouGroupSetA.setCreated( getDate( 2018, 8, 5 ) );
        OrganisationUnitGroupSet ouGroupSetB = createOrganisationUnitGroupSet( 'A' );
        ouGroupSetB.setCreated( getDate( 2017, 8, 5 ) );

        when( idObjectManager.getDataDimensionsNoAcl( OrganisationUnitGroupSet.class ) )
            .thenReturn( Lists.newArrayList( ouGroupSetA, ouGroupSetB ) );

        AnalyticsTableUpdateParams params = AnalyticsTableUpdateParams.newBuilder().withLastYears( 2 ).build();

        List<AnalyticsTable> tables = subject.getAnalyticsTables( params );

        assertThat( tables, hasSize( 1 ) );

        new AnalyticsTableAsserter.Builder( tables.get( 0 ) )
            .withTableType( AnalyticsTableType.EVENT )
            .withColumnSize( 41 )
            .withDefaultColumns( subject.getDefaultColumns() )
            .addColumns(periodColumns)
            .addColumn( ouGroupSetA.getUid(), CHARACTER_11, "ougs.", ouGroupSetA.getCreated() )
            .addColumn( ouGroupSetB.getUid(), CHARACTER_11, "ougs.", ouGroupSetB.getCreated() )
            .build().verify();

    }

    @Test
    public void verifyGetTableWithCategoryOptionGroupSet()
    {


        Program p1 = createProgram( 'A' );

        when( idObjectManager.getAllNoAcl( Program.class ) ).thenReturn( Lists.newArrayList( p1 ) );

        CategoryOptionGroupSet categoryOptionGroupSetA = createCategoryOptionGroupSet( 'A' );
        categoryOptionGroupSetA.setCreated( getDate( 2018, 8, 5 ) );
        CategoryOptionGroupSet categoryOptionGroupSetB = createCategoryOptionGroupSet( 'B' );
        categoryOptionGroupSetB.setCreated( getDate( 2017, 8, 5 ) );

        when( categoryService.getAttributeCategoryOptionGroupSetsNoAcl( ) )
                .thenReturn( Lists.newArrayList( categoryOptionGroupSetA, categoryOptionGroupSetB ) );

        AnalyticsTableUpdateParams params = AnalyticsTableUpdateParams.newBuilder().withLastYears( 2 ).build();

        List<AnalyticsTable> tables = subject.getAnalyticsTables( params );

        assertThat( tables, hasSize( 1 ) );

        new AnalyticsTableAsserter.Builder( tables.get( 0 ) )
            .withTableType( AnalyticsTableType.EVENT )
            .withColumnSize( 41 )
            .addColumns(periodColumns)
            .withDefaultColumns( subject.getDefaultColumns() )
            .addColumn( categoryOptionGroupSetA.getUid(), CHARACTER_11, "acs.", categoryOptionGroupSetA.getCreated() )
            .addColumn( categoryOptionGroupSetB.getUid(), CHARACTER_11, "acs.", categoryOptionGroupSetB.getCreated() )
            .build().verify();
    }

    @Test
    public void verifyGetTableWithDataElements()
    {
        when( databaseInfo.isSpatialSupport() ).thenReturn( true );
        Program p1 = createProgram( 'A' );

        DataElement d1 = createDataElement('Z', ValueType.TEXT, AggregationType.SUM);
        DataElement d2 = createDataElement('P', ValueType.PERCENTAGE, AggregationType.SUM);
        DataElement d3 = createDataElement('Y', ValueType.BOOLEAN, AggregationType.NONE);
        DataElement d4 = createDataElement('W', ValueType.DATE, AggregationType.LAST);
        DataElement d5 = createDataElement('G', ValueType.ORGANISATION_UNIT, AggregationType.NONE);
        DataElement d6 = createDataElement('H', ValueType.INTEGER, AggregationType.SUM);
        DataElement d7 = createDataElement('U', ValueType.COORDINATE, AggregationType.NONE);

        ProgramStage ps1 = createProgramStage( 'A', Sets.newHashSet( d1, d2, d3, d4, d5, d6, d7 ) );

        p1.setProgramStages( Sets.newHashSet( ps1 ) );

        when( idObjectManager.getAllNoAcl( Program.class ) ).thenReturn( Lists.newArrayList( p1 ) );

        String aliasD1 = "(select eventdatavalues #>> '{%s, value}' " + FROM_CLAUSE + " ) as \"%s\"";
        String aliasD2 = "(select cast(eventdatavalues #>> '{%s, value}' as null) "+ FROM_CLAUSE +"  and eventdatavalues #>> '{%s,value}' null '^(-?[0-9]+)(\\.[0-9]+)?$') as \"%s\"";
        String aliasD3 = "(select case when eventdatavalues #>> '{%s, value}' = 'true' then 1 when eventdatavalues #>> '{%s, value}' = 'false' then 0 else null end " + FROM_CLAUSE + " ) as \"%s\"";
        String aliasD4 = "(select cast(eventdatavalues #>> '{%s, value}' as timestamp) " + FROM_CLAUSE + "  and eventdatavalues #>> '{%s,value}' null '^\\d{4}-\\d{2}-\\d{2}(\\s|T)?(\\d{2}:\\d{2}:\\d{2})?$') as \"%s\"";
        String aliasD5 = "(select ou.name from organisationunit ou where ou.uid = " + "(select eventdatavalues #>> '{"
            + d5.getUid() + ", value}' " + FROM_CLAUSE + " )) as \"" + d5.getUid() + "\"";
        String aliasD6 = "(select cast(eventdatavalues #>> '{%s, value}' as bigint) " + FROM_CLAUSE + "  and eventdatavalues #>> '{%s,value}' null '^(-?[0-9]+)(\\.[0-9]+)?$') as \"%s\"";
        String aliasD7 = "(select ST_GeomFromGeoJSON('{\"type\":\"Point\", \"coordinates\":' || (eventdatavalues #>> '{%s, value}') || ', \"crs\":{\"type\":\"name\", \"properties\":{\"name\":\"EPSG:4326\"}}}') from programstageinstance where programstageinstanceid=psi.programstageinstanceid ) as \"%s\"";

        AnalyticsTableUpdateParams params = AnalyticsTableUpdateParams.newBuilder().withLastYears( 2 ).build();

        List<AnalyticsTable> tables = subject.getAnalyticsTables( params );

        assertThat( tables, hasSize( 1 ) );

        new AnalyticsTableAsserter.Builder( tables.get( 0 ) )
            .withTableType( AnalyticsTableType.EVENT )
            .withColumnSize( 46 )
            .addColumns( periodColumns )
            .addColumn( d1.getUid(), TEXT, toAlias( aliasD1, d1.getUid() ) )  // ValueType.TEXT
            .addColumn( d2.getUid(), DOUBLE, toAlias( aliasD2, d2.getUid() ) ) // ValueType.PERCENTAGE
            .addColumn( d3.getUid(), INTEGER, toAlias( aliasD3, d3.getUid() ) ) // ValueType.BOOLEAN
            .addColumn( d4.getUid(), TIMESTAMP, toAlias( aliasD4, d4.getUid() ) ) // ValueType.DATE
            .addColumn( d5.getUid(), TEXT, toAlias( aliasD5, d5.getUid() ) ) // ValueType.ORGANISATION_UNIT
            .addColumn( d6.getUid(), BIGINT, toAlias( aliasD6, d6.getUid() ) ) // ValueType.INTEGER
            .addColumn( d7.getUid(), GEOMETRY_POINT, toAlias( aliasD7, d7.getUid() ) ) // ValueType.COORDINATES
            .withDefaultColumns( subject.getDefaultColumns() )
            .build().verify();
    }
    
    @Test
    public void verifyDataElementTypeOrgUnitFetchesOuNameWhenPopulatingEventAnalyticsTable()
    {
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        when( databaseInfo.isSpatialSupport() ).thenReturn( true );
        Program p1 = createProgram( 'A' );

        DataElement d5 = createDataElement('G', ValueType.ORGANISATION_UNIT, AggregationType.NONE);

        ProgramStage ps1 = createProgramStage( 'A', Sets.newHashSet( d5 ) );

        p1.setProgramStages( Sets.newHashSet( ps1 ) );

        when( idObjectManager.getAllNoAcl( Program.class ) ).thenReturn( Lists.newArrayList( p1 ) );

        AnalyticsTableUpdateParams params = AnalyticsTableUpdateParams.newBuilder().withLastYears( 2 ).build();

        subject.populateTable( params,
            PartitionUtils.getTablePartitions( subject.getAnalyticsTables( params ) ).get( 0 ) );

        verify(jdbcTemplate).execute(sql.capture());
        String ouQuery = "(select ou.name from organisationunit ou where ou.uid = " + "(select eventdatavalues #>> '{"
            + d5.getUid() + ", value}' from programstageinstance where "
            + "programstageinstanceid=psi.programstageinstanceid )) as \"" + d5.getUid() + "\"";

        assertThat(sql.getValue(), containsString(ouQuery));

    }
    
    private String toAlias( String template, String uid )
    {
        return String.format( template, uid, uid, uid );
    }
    
    private void addCategoryCombo( Program p, CategoryCombo categoryCombo )
    {
        p.setCategoryCombo( categoryCombo );
    }
}