/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.expression;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hisp.dhis.antlr.AntlrParserUtils.castDouble;
import static org.hisp.dhis.category.CategoryCombo.DEFAULT_CATEGORY_COMBO_NAME;
import static org.hisp.dhis.common.DimensionItemType.DATA_ELEMENT;
import static org.hisp.dhis.expression.Expression.SEPARATOR;
import static org.hisp.dhis.expression.ExpressionService.SYMBOL_DAYS;
import static org.hisp.dhis.expression.MissingValueStrategy.NEVER_SKIP;
import static org.hisp.dhis.expression.ParseType.INDICATOR_EXPRESSION;
import static org.hisp.dhis.expression.ParseType.PREDICTOR_EXPRESSION;
import static org.hisp.dhis.expression.ParseType.VALIDATION_RULE_EXPRESSION;
import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.utils.Assertions.assertMapEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.util.Precision;
import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.cache.NoOpCache;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.DataDimensionType;
import org.hisp.dhis.common.DimensionItemType;
import org.hisp.dhis.common.DimensionService;
import org.hisp.dhis.common.DimensionalItemId;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.common.ReportingRate;
import org.hisp.dhis.constant.Constant;
import org.hisp.dhis.constant.ConstantService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.hibernate.HibernateGenericStore;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorType;
import org.hisp.dhis.indicator.IndicatorValue;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramDataElementDimensionItem;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramTrackedEntityAttributeDimensionItem;
import org.hisp.dhis.random.BeanRandomizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @author Luciano Fiandesio
 */
@ExtendWith( MockitoExtension.class )
class ExpressionService2Test extends DhisConvenienceTest
{
    @Mock
    private HibernateGenericStore<Expression> hibernateGenericStore;

    @Mock
    private ConstantService constantService;

    @Mock
    private DimensionService dimensionService;

    @Mock
    private IdentifiableObjectManager idObjectManager;

    @Mock
    private StatementBuilder statementBuilder;

    @Mock
    private I18nManager i18nManager;

    @Mock
    private CacheProvider cacheProvider;

    private DefaultExpressionService target;

    private CategoryOption categoryOptionA;

    private CategoryOption categoryOptionB;

    private CategoryOption categoryOptionC;

    private CategoryOption categoryOptionD;

    private Category categoryA;

    private Category categoryB;

    private CategoryCombo categoryCombo;

    private DataElement deA;

    private DataElement deB;

    private DataElement deC;

    private DataElement deD;

    private DataElement deE;

    private DataElementOperand opA;

    private DataElementOperand opB;

    private DataElementOperand opC;

    private DataElementOperand opD;

    private DataElementOperand opE;

    private DataElementOperand opF;

    private ProgramTrackedEntityAttributeDimensionItem pteaA;

    private ProgramDataElementDimensionItem pdeA;

    private ProgramIndicator piA;

    private Period period;

    private OrganisationUnit unitA;

    private OrganisationUnit unitB;

    private OrganisationUnit unitC;

    private CategoryOptionCombo coc;

    private CategoryOptionCombo cocA;

    private CategoryOptionCombo cocB;

    private Constant constantA;

    private Constant constantB;

    private OrganisationUnitGroup groupA;

    private OrganisationUnitGroup groupB;

    private DataSet dataSetA;

    private DataSet dataSetB;

    private Program programA;

    private Program programB;

    private ReportingRate reportingRate;

    private String expressionA;

    private String expressionB;

    private String expressionC;

    private String expressionD;

    private String expressionE;

    private String expressionF;

    private String expressionG;

    private String expressionH;

    private String expressionI;

    private String expressionK;

    private String expressionJ;

    private String expressionL;

    private String expressionM;

    private String expressionN;

    private String expressionO;

    private String expressionP;

    private String expressionR;

    private static final double DELTA = 0.01;

    private final BeanRandomizer rnd = BeanRandomizer.create();

    @BeforeEach
    public void setUp()
    {
        when( cacheProvider.createAllConstantsCache() ).thenReturn( new NoOpCache<>() );
        target = new DefaultExpressionService( hibernateGenericStore, constantService, dimensionService,
            idObjectManager, statementBuilder, i18nManager, cacheProvider );

        categoryOptionA = new CategoryOption( "Under 5" );
        categoryOptionB = new CategoryOption( "Over 5" );
        categoryOptionC = new CategoryOption( "Male" );
        categoryOptionD = new CategoryOption( "Female" );

        categoryA = new Category( "Age", DataDimensionType.DISAGGREGATION );
        categoryB = new Category( "Gender", DataDimensionType.DISAGGREGATION );

        categoryA.getCategoryOptions().add( categoryOptionA );
        categoryA.getCategoryOptions().add( categoryOptionB );
        categoryB.getCategoryOptions().add( categoryOptionC );
        categoryB.getCategoryOptions().add( categoryOptionD );

        categoryCombo = new CategoryCombo( "Age and gender", DataDimensionType.DISAGGREGATION );
        categoryCombo.getCategories().add( categoryA );
        categoryCombo.getCategories().add( categoryB );
        categoryCombo.generateOptionCombos();

        List<CategoryOptionCombo> optionCombos = Lists.newArrayList( categoryCombo.getOptionCombos() );

        cocA = optionCombos.get( 0 );
        cocA.setUid( CodeGenerator.generateUid() );
        cocB = optionCombos.get( 1 );
        cocB.setUid( CodeGenerator.generateUid() );

        deA = createDataElement( 'A' );
        deB = createDataElement( 'B' );
        deC = createDataElement( 'C' );
        deD = createDataElement( 'D' );
        deE = createDataElement( 'E', categoryCombo );

        coc = rnd.nextObject( CategoryOptionCombo.class );
        coc.setName( DEFAULT_CATEGORY_COMBO_NAME );

        optionCombos.add( coc );

        opA = new DataElementOperand( deA, coc );
        opB = new DataElementOperand( deB, coc );
        opC = new DataElementOperand( deC, coc );
        opD = new DataElementOperand( deD, coc );
        opE = new DataElementOperand( deB, cocA );
        opF = new DataElementOperand( deA, cocA, cocB );

        period = createPeriod( getDate( 2000, 1, 1 ), getDate( 2000, 1, 31 ) );

        pteaA = rnd.nextObject( ProgramTrackedEntityAttributeDimensionItem.class );
        pdeA = rnd.nextObject( ProgramDataElementDimensionItem.class );
        piA = rnd.nextObject( ProgramIndicator.class );

        unitA = createOrganisationUnit( 'A' );
        unitB = createOrganisationUnit( 'B' );
        unitC = createOrganisationUnit( 'C' );

        constantA = rnd.nextObject( Constant.class );
        constantA.setName( "ConstantA" );
        constantA.setValue( 2.0 );

        constantB = rnd.nextObject( Constant.class );
        constantB.setName( "ConstantB" );
        constantB.setValue( 5.0 );

        groupA = createOrganisationUnitGroup( 'A' );
        groupA.addOrganisationUnit( unitA );
        groupA.addOrganisationUnit( unitB );
        groupA.addOrganisationUnit( unitC );

        groupB = createOrganisationUnitGroup( 'B' );
        groupB.addOrganisationUnit( unitB );

        dataSetA = createDataSet( 'A' );
        dataSetA.setUid( "a23dataSetA" );
        dataSetA.addOrganisationUnit( unitA );

        dataSetB = createDataSet( 'B' );
        dataSetB.setUid( "a23dataSetB" );
        dataSetB.addOrganisationUnit( unitA );

        programA = createProgram( 'A' );
        programA.setUid( "a23programA" );
        programA.addOrganisationUnit( unitA );

        programB = createProgram( 'B' );
        programB.setUid( "a23programB" );
        dataSetB.addOrganisationUnit( unitB );

        reportingRate = new ReportingRate( dataSetA );

        expressionA = "#{" + opA.getDimensionItem() + "}+#{" + opB.getDimensionItem() + "}";
        expressionB = "#{" + deC.getUid() + SEPARATOR + coc.getUid() + "}-#{" + deD.getUid() + SEPARATOR + coc.getUid()
            + "}";
        expressionC = "#{" + deA.getUid() + SEPARATOR + coc.getUid() + "}+#{" + deE.getUid() + "}-10";
        expressionD = "#{" + deA.getUid() + SEPARATOR + coc.getUid() + "}+" + SYMBOL_DAYS;
        expressionE = "#{" + deA.getUid() + SEPARATOR + coc.getUid() + "}*C{" + constantA.getUid() + "}";
        expressionF = "#{" + deA.getUid() + SEPARATOR + coc.getUid() + "}";
        expressionG = expressionF + "+#{" + deB.getUid() + "}-#{" + deC.getUid() + "}";
        expressionH = "#{" + deA.getUid() + SEPARATOR + coc.getUid() + "}*OUG{" + groupA.getUid() + "}";
        expressionI = "#{" + opA.getDimensionItem() + "}*" + "#{" + deB.getDimensionItem() + "}+" +
            "C{" + constantA.getUid() + "}+5-" + "D{" + pdeA.getDimensionItem() + "}+" +
            "A{" + pteaA.getDimensionItem() + "}-10+" + "I{" + piA.getDimensionItem() + "}";
        expressionJ = "#{" + opA.getDimensionItem() + "}+#{" + opB.getDimensionItem() + "}";
        expressionK = "1.5*avg(" + expressionJ + ")";
        expressionL = expressionA + "+avg(" + expressionJ + ")+1.5*stddev(" + expressionJ + ")+" + expressionB;
        expressionM = "#{" + deA.getUid() + "}-#{" + deB.getUid() + SEPARATOR
            + coc.getUid() + "}";
        expressionN = "#{" + deA.getUid() + SEPARATOR + cocA.getUid() + SEPARATOR + cocB.getUid() + "}-#{"
            + deB.getUid() + SEPARATOR + cocA.getUid() + "}";
        expressionO = "#{" + opA.getDimensionItem() + "}+sum(#{" + opB.getDimensionItem() + "})";
        expressionP = "#{" + deB.getUid() + SEPARATOR + coc.getUid() + "}";
        expressionR = "#{" + deB.getUid() + SEPARATOR + coc.getUid() + "}" + " + R{" + reportingRate.getUid() +
            ".REPORTING_RATE}";
    }

    private DimensionalItemId getId( DimensionalItemObject o )
    {
        DimensionItemType type = o.getDimensionItemType();

        switch ( type )
        {
        case DATA_ELEMENT:
            String deItem = "#{" + o.getUid() + "}";
            return new DimensionalItemId( type, o.getUid(), null, null, deItem );

        case DATA_ELEMENT_OPERAND:
            DataElementOperand deo = (DataElementOperand) o;

            String deoItem = "#{" + deo.getDataElement().getUid() +
                ((deo.getCategoryOptionCombo() != null)
                    ? "." + deo.getCategoryOptionCombo().getUid()
                    : "")
                +
                ((deo.getCategoryOptionCombo() == null && deo.getAttributeOptionCombo() != null)
                    ? ".*"
                    : "")
                +
                ((deo.getAttributeOptionCombo() != null)
                    ? "." + deo.getAttributeOptionCombo().getUid()
                    : "")
                +
                "}";

            return new DimensionalItemId( type,
                deo.getDataElement().getUid(),
                deo.getCategoryOptionCombo() == null ? null : deo.getCategoryOptionCombo().getUid(),
                deo.getAttributeOptionCombo() == null ? null : deo.getAttributeOptionCombo().getUid(),
                deoItem );

        case REPORTING_RATE:
            ReportingRate rr = (ReportingRate) o;

            return new DimensionalItemId( type,
                rr.getDataSet().getUid(),
                rr.getMetric().name() );

        case PROGRAM_DATA_ELEMENT:
            ProgramDataElementDimensionItem pde = (ProgramDataElementDimensionItem) o;

            return new DimensionalItemId( type,
                pde.getProgram().getUid(),
                pde.getDataElement().getUid() );

        case PROGRAM_ATTRIBUTE:
            ProgramTrackedEntityAttributeDimensionItem pa = (ProgramTrackedEntityAttributeDimensionItem) o;

            return new DimensionalItemId( type,
                pa.getProgram().getUid(),
                pa.getAttribute().getUid() );

        case PROGRAM_INDICATOR:
            return new DimensionalItemId( type, o.getUid() );

        default:
            return null;
        }
    }

    @SafeVarargs
    private <T extends IdentifiableObject> Map<String, T> getMap( T... idObjects )
    {
        return IdentifiableObjectUtils.getIdMap( List.of( idObjects ), IdScheme.UID );
    }

    private Double exprValue( String expression, Map<DimensionalItemId, DimensionalItemObject> itemMap,
        Map<DimensionalItemObject, Object> valueMap, Map<String, Integer> orgUnitCountMap, Integer days )
    {
        return castDouble( target.getExpressionValue( ExpressionParams.builder()
            .expression( expression )
            .parseType( INDICATOR_EXPRESSION )
            .itemMap( itemMap )
            .valueMap( valueMap )
            .orgUnitCountMap( orgUnitCountMap )
            .days( days )
            .missingValueStrategy( NEVER_SKIP )
            .build() ) );
    }

    private void mockConstantService()
    {
        when( constantService.getConstantMap() ).thenReturn( Map.of(
            constantA.getUid(), constantA,
            constantB.getUid(), constantB ) );
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    void testGetExpressionElementAndOptionComboIds()
    {
        Set<String> ids = target.getExpressionElementAndOptionComboIds( expressionC, VALIDATION_RULE_EXPRESSION );

        assertEquals( 2, ids.size() );
        assertTrue( ids.contains( deA.getUid() + SEPARATOR + coc.getUid() ) );
        assertTrue( ids.contains( deE.getUid() ) );
    }

    @Test
    void testGetExpressionDimensionalItemIdsNullOrEmpty()
    {
        Set<DimensionalItemId> itemIds = target.getExpressionDimensionalItemIds( null, INDICATOR_EXPRESSION );
        assertEquals( 0, itemIds.size() );

        itemIds = target.getExpressionDimensionalItemIds( "", INDICATOR_EXPRESSION );
        assertEquals( 0, itemIds.size() );
    }

    @Test
    void testGetExpressionDimensionalItemIds()
    {
        mockConstantService();

        Set<DimensionalItemId> itemIds = target.getExpressionDimensionalItemIds( expressionI, INDICATOR_EXPRESSION );

        assertEquals( 5, itemIds.size() );
        assertTrue( itemIds.contains( getId( opA ) ) );
        assertTrue( itemIds.contains( getId( deB ) ) );
        assertTrue( itemIds.contains( getId( pdeA ) ) );
        assertTrue( itemIds.contains( getId( pteaA ) ) );
        assertTrue( itemIds.contains( getId( piA ) ) );
    }

    @Test
    void testGetExpressionDimensionalItemIdsWithDeGroup()
    {
        mockConstantService();

        String deGroupUid = "deGroupUidA";
        String expr = "#{deGroup:" + deGroupUid + "}";

        DimensionalItemId itemId = new DimensionalItemId( DATA_ELEMENT, "deGroup:" + deGroupUid, null, null, expr );

        Set<DimensionalItemId> itemIds = target.getExpressionDimensionalItemIds( expr, INDICATOR_EXPRESSION );

        assertEquals( 1, itemIds.size() );
        assertTrue( itemIds.contains( itemId ) );
    }

    @Test
    void testGetExpressionInfoItemIdsNullOrEmpty()
    {
        ExpressionInfo info;

        info = target.getExpressionInfo( ExpressionParams.builder()
            .expression( null )
            .parseType( INDICATOR_EXPRESSION )
            .build() );
        assertEquals( 0, info.getAllItemIds().size() );
        assertEquals( 0, info.getSampleItemIds().size() );

        info = target.getExpressionInfo( ExpressionParams.builder()
            .expression( "" )
            .parseType( INDICATOR_EXPRESSION )
            .build() );
        assertEquals( 0, info.getAllItemIds().size() );
        assertEquals( 0, info.getSampleItemIds().size() );
    }

    @Test
    void testGetExpressionInfo()
    {
        String expression = "#{" + opA.getDimensionItem() + "}" +
            "+sum(#{" + deB.getUid() + "})" +
            "+sum(D{" + pdeA.getDimensionItem() + "})" +
            "+R{" + reportingRate.getUid() + ".REPORTING_RATE}" +
            "+if(orgUnit.ancestor(" + unitA.getUid() + "," + unitB.getUid() + "),1,0)" +
            "+if(orgUnit.group(" + groupA.getUid() + "," + groupB.getUid() + "),1,0)" +
            "+if(orgUnit.dataSet(" + dataSetA.getUid() + "," + dataSetB.getUid() + "),1,0)" +
            "+if(orgUnit.program(" + programA.getUid() + "," + programB.getUid() + "),1,0)";

        ExpressionInfo info = target.getExpressionInfo( ExpressionParams.builder()
            .expression( expression )
            .parseType( PREDICTOR_EXPRESSION )
            .build() );

        assertContainsOnly( Set.of( getId( opA ), getId( reportingRate ) ), info.getItemIds() );
        assertContainsOnly( Set.of( getId( deB ), getId( pdeA ) ), info.getSampleItemIds() );
        assertContainsOnly( Set.of( getId( opA ), getId( reportingRate ), getId( deB ), getId( pdeA ) ),
            info.getAllItemIds() );
        assertContainsOnly( Set.of( groupA.getUid(), groupB.getUid() ), info.getOrgUnitGroupIds() );
        assertContainsOnly( Set.of( dataSetA.getUid(), dataSetB.getUid() ), info.getOrgUnitDataSetIds() );
        assertContainsOnly( Set.of( programA.getUid(), programB.getUid() ), info.getOrgUnitProgramIds() );
        assertTrue( info.getOrgUnitGroupCountIds().isEmpty() );
    }

    @Test
    void testGetExpressionInfoOrgUnitGroupCountIds()
    {
        String expression = "OUG{" + groupA.getUid() + "}" +
            "+OUG{" + groupB.getUid() + "}";

        ExpressionInfo info = target.getExpressionInfo( ExpressionParams.builder()
            .expression( expression )
            .parseType( INDICATOR_EXPRESSION )
            .build() );

        assertContainsOnly( Set.of( groupA.getUid(), groupB.getUid() ), info.getOrgUnitGroupCountIds() );
        assertTrue( info.getItemIds().isEmpty() );
        assertTrue( info.getSampleItemIds().isEmpty() );
        assertTrue( info.getAllItemIds().isEmpty() );
        assertTrue( info.getOrgUnitGroupIds().isEmpty() );
        assertTrue( info.getOrgUnitDataSetIds().isEmpty() );
        assertTrue( info.getOrgUnitProgramIds().isEmpty() );
    }

    @Test
    void testGetExpressionInfoChainedCalls()
    {
        String expression1 = "#{" + opA.getDimensionItem() + "}" +
            " + sum(#{" + deB.getUid() + "})";

        ExpressionInfo info = target.getExpressionInfo( ExpressionParams.builder()
            .expression( expression1 )
            .parseType( PREDICTOR_EXPRESSION )
            .build() );

        String expression2 = "D{" + pdeA.getDimensionItem() + "}" +
            " + sum(R{" + reportingRate.getUid() + ".REPORTING_RATE})";

        target.getExpressionInfo( ExpressionParams.builder()
            .expressionInfo( info )
            .expression( expression2 )
            .parseType( PREDICTOR_EXPRESSION )
            .build() );

        assertContainsOnly( Set.of( getId( opA ), getId( pdeA ) ), info.getItemIds() );
        assertContainsOnly( Set.of( getId( deB ), getId( reportingRate ) ), info.getSampleItemIds() );
    }

    @Test
    void testGetBaseExpressionParams()
    {
        ExpressionInfo info = new ExpressionInfo();

        info.getItemIds().addAll( List.of( getId( opA ), getId( deB ) ) );
        info.getSampleItemIds().addAll( List.of( getId( reportingRate ), getId( pdeA ) ) );
        info.getOrgUnitGroupIds().addAll( List.of( groupA.getUid(), groupB.getUid() ) );
        info.getOrgUnitDataSetIds().addAll( List.of( dataSetA.getUid(), dataSetB.getUid() ) );
        info.getOrgUnitProgramIds().addAll( List.of( programA.getUid(), programB.getUid() ) );

        when( dimensionService.getNoAclDataDimensionalItemObjectMap( (Set<DimensionalItemId>) argThat(
            containsInAnyOrder( getId( opA ), getId( deB ), getId( reportingRate ), getId( pdeA ) ) ) ) )
            .thenReturn( Map.of(
                getId( opA ), opA,
                getId( deB ), deB,
                getId( reportingRate ), reportingRate,
                getId( pdeA ), pdeA ) );

        when( idObjectManager.getNoAcl( eq( OrganisationUnitGroup.class ), (java.util.Collection<String>) argThat(
            containsInAnyOrder( groupA.getUid(), groupB.getUid() ) ) ) )
            .thenReturn( List.of( groupA, groupB ) );

        when( idObjectManager.getNoAcl( eq( DataSet.class ), (java.util.Collection<String>) argThat(
            containsInAnyOrder( dataSetA.getUid(), dataSetB.getUid() ) ) ) )
            .thenReturn( List.of( dataSetA, dataSetB ) );

        when( idObjectManager.getNoAcl( eq( Program.class ), (java.util.Collection<String>) argThat(
            containsInAnyOrder( programA.getUid(), programB.getUid() ) ) ) )
            .thenReturn( List.of( programA, programB ) );

        ExpressionParams baseParams = target.getBaseExpressionParams( info );

        Map<DimensionalItemId, DimensionalItemObject> expectedItemMap = Map.of(
            getId( opA ), opA,
            getId( deB ), deB,
            getId( reportingRate ), reportingRate,
            getId( pdeA ), pdeA );

        assertMapEquals( expectedItemMap, baseParams.getItemMap() );
        assertMapEquals( getMap( groupA, groupB ), baseParams.getOrgUnitGroupMap() );
        assertMapEquals( getMap( dataSetA, dataSetB ), baseParams.getDataSetMap() );
        assertMapEquals( getMap( programA, programB ), baseParams.getProgramMap() );
    }

    @Test
    void testGetIndicatorDimensionalItemMap()
    {
        Set<DimensionalItemId> itemIds = Sets.newHashSet(
            getId( opA ), getId( opB ), getId( deB ), getId( pdeA ), getId( pteaA ), getId( piA ) );

        ImmutableMap<DimensionalItemId, DimensionalItemObject> itemMap = ImmutableMap
            .<DimensionalItemId, DimensionalItemObject> builder()
            .put( getId( opA ), opA )
            .put( getId( opB ), opB )
            .put( getId( deB ), deB )
            .put( getId( pdeA ), pdeA )
            .put( getId( pteaA ), pteaA )
            .put( getId( piA ), piA )
            .build();

        when( dimensionService.getDataDimensionalItemObjectMap( itemIds ) ).thenReturn( itemMap );

        mockConstantService();

        Indicator indicator = createIndicator( 'A', null );
        indicator.setNumerator( expressionI );
        indicator.setDenominator( expressionA );

        Set<Indicator> indicators = Sets.newHashSet( indicator );

        Map<DimensionalItemId, DimensionalItemObject> result = target.getIndicatorDimensionalItemMap( indicators );

        assertMapEquals( itemMap, result );
    }

    @Test
    void testGetExpressionDataElementIds()
    {
        Set<String> dataElementIds = target.getExpressionDataElementIds( expressionA, INDICATOR_EXPRESSION );

        assertThat( dataElementIds, hasSize( 2 ) );
        assertThat( dataElementIds, hasItems( opA.getDataElement().getUid(), opB.getDataElement().getUid() ) );

        dataElementIds = target.getExpressionDataElementIds( expressionG, INDICATOR_EXPRESSION );

        assertThat( dataElementIds, hasSize( 3 ) );
        assertThat( dataElementIds, hasItems( deA.getUid(), deB.getUid(), deC.getUid() ) );

        dataElementIds = target.getExpressionDataElementIds( expressionM, INDICATOR_EXPRESSION );

        assertThat( dataElementIds, hasSize( 2 ) );
        assertThat( dataElementIds, hasItems( deA.getUid(), deB.getUid() ) );
    }

    @Test
    void testGetExpressionOptionComboIds()
    {
        Set<String> comboIds = target.getExpressionOptionComboIds( expressionG, INDICATOR_EXPRESSION );

        assertNotNull( comboIds );
        assertThat( comboIds, hasSize( 1 ) );
        assertThat( comboIds, hasItem( coc.getUid() ) );
    }

    @Test
    void testExpressionIsValid()
    {
        mockConstantService();
        when( dimensionService.getDataDimensionalItemObject( getId( deA ) ) ).thenReturn( deA );
        when( dimensionService.getDataDimensionalItemObject( getId( deE ) ) ).thenReturn( deE );
        when( dimensionService.getDataDimensionalItemObject( getId( opA ) ) ).thenReturn( opA );
        when( dimensionService.getDataDimensionalItemObject( getId( opB ) ) ).thenReturn( opB );
        when( dimensionService.getDataDimensionalItemObject( getId( opC ) ) ).thenReturn( opC );
        when( dimensionService.getDataDimensionalItemObject( getId( opD ) ) ).thenReturn( opD );
        when( dimensionService.getDataDimensionalItemObject( getId( opE ) ) ).thenReturn( opE );
        when( dimensionService.getDataDimensionalItemObject( getId( opF ) ) ).thenReturn( opF );
        when( dimensionService.getDataDimensionalItemObject( getId( reportingRate ) ) ).thenReturn( reportingRate );
        when( idObjectManager.get( OrganisationUnitGroup.class, groupA.getUid() ) ).thenReturn( groupA );

        assertTrue( target.expressionIsValid( expressionA, VALIDATION_RULE_EXPRESSION ).isValid() );
        assertTrue( target.expressionIsValid( expressionB, VALIDATION_RULE_EXPRESSION ).isValid() );
        assertTrue( target.expressionIsValid( expressionC, VALIDATION_RULE_EXPRESSION ).isValid() );
        assertTrue( target.expressionIsValid( expressionD, VALIDATION_RULE_EXPRESSION ).isValid() );
        assertTrue( target.expressionIsValid( expressionE, VALIDATION_RULE_EXPRESSION ).isValid() );
        assertTrue( target.expressionIsValid( expressionH, VALIDATION_RULE_EXPRESSION ).isValid() );
        assertFalse( target.expressionIsValid( expressionK, VALIDATION_RULE_EXPRESSION ).isValid() );
        assertFalse( target.expressionIsValid( expressionL, VALIDATION_RULE_EXPRESSION ).isValid() );
        assertTrue( target.expressionIsValid( expressionM, VALIDATION_RULE_EXPRESSION ).isValid() );
        assertTrue( target.expressionIsValid( expressionN, VALIDATION_RULE_EXPRESSION ).isValid() );
        assertTrue( target.expressionIsValid( expressionR, VALIDATION_RULE_EXPRESSION ).isValid() );

        assertTrue( target.expressionIsValid( expressionK, PREDICTOR_EXPRESSION ).isValid() );
        assertTrue( target.expressionIsValid( expressionL, PREDICTOR_EXPRESSION ).isValid() );

        String expression = "#{nonExisting" + SEPARATOR + coc.getUid() + "} + 12";

        assertEquals( ExpressionValidationOutcome.EXPRESSION_IS_NOT_WELL_FORMED,
            target.expressionIsValid( expression, VALIDATION_RULE_EXPRESSION ) );

        expression = "#{" + deA.getUid() + SEPARATOR + "999} + 12";

        assertEquals( ExpressionValidationOutcome.EXPRESSION_IS_NOT_WELL_FORMED, target
            .expressionIsValid( expression, INDICATOR_EXPRESSION ) );

        expression = "#{" + deA.getUid() + SEPARATOR + coc.getUid() + "} + ( 12";

        assertEquals( ExpressionValidationOutcome.EXPRESSION_IS_NOT_WELL_FORMED,
            target.expressionIsValid( expression, VALIDATION_RULE_EXPRESSION ) );

        expression = "12 x 4";

        assertEquals( ExpressionValidationOutcome.EXPRESSION_IS_NOT_WELL_FORMED,
            target.expressionIsValid( expression, VALIDATION_RULE_EXPRESSION ) );

        expression = "1.5*AVG(" + target;

        assertEquals( ExpressionValidationOutcome.EXPRESSION_IS_NOT_WELL_FORMED,
            target.expressionIsValid( expression, VALIDATION_RULE_EXPRESSION ) );

        expression = "12 + C{nonExisting}";

        assertEquals( ExpressionValidationOutcome.EXPRESSION_IS_NOT_WELL_FORMED,
            target.expressionIsValid( expression, VALIDATION_RULE_EXPRESSION ) );

        expression = "12 + OUG{nonExisting}";

        assertEquals( ExpressionValidationOutcome.EXPRESSION_IS_NOT_WELL_FORMED,
            target.expressionIsValid( expression, VALIDATION_RULE_EXPRESSION ) );
    }

    @Test
    void testGetExpressionDescription()
    {
        mockConstantService();
        when( dimensionService.getDataDimensionalItemObject( getId( opA ) ) ).thenReturn( opA );
        when( dimensionService.getDataDimensionalItemObject( getId( opB ) ) ).thenReturn( opB );

        String description = target.getExpressionDescription( expressionA, INDICATOR_EXPRESSION );
        assertThat( description, is( opA.getDisplayName() + "+" + opB.getDisplayName() ) );

        description = target.getExpressionDescription( expressionD, INDICATOR_EXPRESSION );
        assertThat( description, is( opA.getDisplayName() + "+" + ExpressionService.DAYS_DESCRIPTION ) );

        description = target.getExpressionDescription( expressionE, INDICATOR_EXPRESSION );
        assertThat( description, is( opA.getDisplayName() + "*" + constantA.getDisplayName() ) );

        when( idObjectManager.get( OrganisationUnitGroup.class, groupA.getUid() ) ).thenReturn( groupA );
        description = target.getExpressionDescription( expressionH, INDICATOR_EXPRESSION );
        assertThat( description, is( opA.getDisplayName() + "*" + groupA.getDisplayName() ) );

        when( dimensionService.getDataDimensionalItemObject( getId( deA ) ) ).thenReturn( deA );
        description = target.getExpressionDescription( expressionM, INDICATOR_EXPRESSION );
        assertThat( description, is( deA.getDisplayName() + "-" + deB.getDisplayName() + " " + coc.getDisplayName() ) );

        when( dimensionService.getDataDimensionalItemObject( getId( reportingRate ) ) ).thenReturn( reportingRate );
        description = target.getExpressionDescription( expressionR, INDICATOR_EXPRESSION );
        assertThat( description,
            is( deB.getDisplayName() + " " + coc.getDisplayName() + " + " + reportingRate.getDisplayName() ) );
    }

    @Test
    void testGetExpressionValue()
    {
        Map<DimensionalItemId, DimensionalItemObject> itemMap = ImmutableMap
            .<DimensionalItemId, DimensionalItemObject> builder()
            .put( getId( opA ), opA )
            .put( getId( opB ), opB )
            .put( getId( opE ), opE )
            .put( getId( opF ), opF )
            .put( getId( reportingRate ), reportingRate )
            .build();

        Map<DimensionalItemObject, Object> valueMap = new HashMap<>();
        valueMap.put( opA, 12d );
        valueMap.put( opB, 34d );
        valueMap.put( opE, 16d );
        valueMap.put( opF, 26d );
        valueMap.put( reportingRate, 20d );

        Map<String, Integer> orgUnitCountMap = new HashMap<>();
        orgUnitCountMap.put( groupA.getUid(), groupA.getMembers().size() );

        mockConstantService();

        assertEquals( 46d, exprValue( expressionA, itemMap, valueMap, null, null ), DELTA );
        assertEquals( 17d, exprValue( expressionD, itemMap, valueMap, null, 5 ), DELTA );
        assertEquals( 24d, exprValue( expressionE, itemMap, valueMap, null, null ), DELTA );
        assertEquals( 36d, exprValue( expressionH, itemMap, valueMap, orgUnitCountMap, null ), DELTA );
        assertEquals( 10d, exprValue( expressionN, itemMap, valueMap, orgUnitCountMap, null ), DELTA );
        assertEquals( 54d, exprValue( expressionR, itemMap, valueMap, orgUnitCountMap, null ), DELTA );
    }

    @Test
    void testGetIndicatorDimensionalItemMap2()
    {
        Set<DimensionalItemId> itemIds = Sets.newHashSet( getId( opA ) );

        Map<DimensionalItemId, DimensionalItemObject> expectedItemMap = Map.of(
            getId( opA ), opA );

        when( dimensionService.getDataDimensionalItemObjectMap( itemIds ) ).thenReturn( expectedItemMap );

        mockConstantService();

        IndicatorType indicatorType = new IndicatorType( "A", 100, false );

        Indicator indicatorA = createIndicator( 'A', indicatorType );
        indicatorA.setNumerator( expressionE );
        indicatorA.setDenominator( expressionF );

        List<Indicator> indicators = Arrays.asList( indicatorA );

        Map<DimensionalItemId, DimensionalItemObject> itemMap = target.getIndicatorDimensionalItemMap( indicators );

        Map<DimensionalItemObject, Object> valueMap = new HashMap<>();

        valueMap.put( new DataElementOperand( deA, coc ), 12d );

        IndicatorValue value = target.getIndicatorValueObject( indicatorA, Collections.singletonList( period ),
            itemMap, valueMap, null );

        assertNotNull( value );
        assertEquals( 24d, value.getNumeratorValue(), DELTA );
        assertEquals( 12d, value.getDenominatorValue(), DELTA );
        assertEquals( 100, value.getMultiplier() );
        assertEquals( 1, value.getDivisor() );
        assertEquals( 100d, value.getFactor(), DELTA );
        assertEquals( 200d, value.getValue(), DELTA );
    }

    @Test
    void testGetIndicatorDimensionalItemMap3()
    {
        Set<DimensionalItemId> itemIds = Sets.newHashSet( getId( opA ), getId( opE ), getId( opF ) );

        Map<DimensionalItemId, DimensionalItemObject> expectedItemMap = Map.of(
            getId( opA ), opA,
            getId( opE ), opE,
            getId( opF ), opF );

        when( dimensionService.getDataDimensionalItemObjectMap( itemIds ) ).thenReturn( expectedItemMap );

        mockConstantService();

        IndicatorType indicatorType = new IndicatorType( "A", 100, false );

        Indicator indicatorB = createIndicator( 'B', indicatorType );
        indicatorB.setNumerator( expressionN );
        indicatorB.setDenominator( expressionF );
        indicatorB.setAnnualized( true );

        List<Indicator> indicators = Arrays.asList( indicatorB );

        Map<DimensionalItemId, DimensionalItemObject> itemMap = target.getIndicatorDimensionalItemMap( indicators );

        Map<DimensionalItemObject, Object> valueMap = new HashMap<>();

        valueMap.put( new DataElementOperand( deA, coc ), 12d );
        valueMap.put( new DataElementOperand( deB, coc ), 34d );
        valueMap.put( new DataElementOperand( deA, cocA, cocB ), 46d );
        valueMap.put( new DataElementOperand( deB, cocA ), 10d );

        IndicatorValue value = target.getIndicatorValueObject( indicatorB, Collections.singletonList( period ),
            itemMap, valueMap, null );

        assertNotNull( value );
        assertEquals( 36d, value.getNumeratorValue(), DELTA );
        assertEquals( 12d, value.getDenominatorValue(), DELTA );
        assertEquals( 36500, value.getMultiplier() );
        assertEquals( 31, value.getDivisor() );
        assertEquals( 1177.419, value.getFactor(), DELTA );
        assertEquals( 3532.258, value.getValue(), DELTA );
    }

    @Test
    void testSubstituteIndicatorExpressions()
    {
        String expressionZ = "if(\"A\" < 'B' and true,0,0)";

        IndicatorType indicatorType = new IndicatorType( "A", 100, false );

        Indicator indicatorA = createIndicator( 'A', indicatorType );
        indicatorA.setNumerator( expressionD );
        indicatorA.setDenominator( expressionE );

        Indicator indicatorB = createIndicator( 'B', indicatorType );
        indicatorB.setNumerator( expressionH );
        indicatorB.setDenominator( expressionZ );

        List<Indicator> indicators = Lists.newArrayList( indicatorA, indicatorB );

        List<Constant> constants = ImmutableList.<Constant> builder()
            .add( constantA )
            .add( constantB )
            .build();

        List<OrganisationUnitGroup> orgUnitGroups = ImmutableList.<OrganisationUnitGroup> builder()
            .add( groupA )
            .build();

        when( idObjectManager.getAllNoAcl( Constant.class ) ).thenReturn( constants );
        when( idObjectManager.getAllNoAcl( OrganisationUnitGroup.class ) ).thenReturn( orgUnitGroups );

        target.substituteIndicatorExpressions( indicators );

        assertEquals( "#{deabcdefghA." + coc.getUid() + "}+[days]", indicatorA.getExplodedNumerator() );
        assertEquals( "#{deabcdefghA." + coc.getUid() + "}*2.0", indicatorA.getExplodedDenominator() );
        assertEquals( "#{deabcdefghA." + coc.getUid() + "}*3", indicatorB.getExplodedNumerator() );
        assertEquals( expressionZ, indicatorB.getExplodedDenominator() );
    }

    // -------------------------------------------------------------------------
    // CRUD tests
    // -------------------------------------------------------------------------

    // -------------------------------------------------------------------------

    @Test
    void verifyExpressionIsUpdated()
    {
        Expression expression = rnd.nextObject( Expression.class );
        target.updateExpression( expression );
        verify( hibernateGenericStore ).update( expression );
    }

    @Test
    void verifyExpressionIsDeleted()
    {
        Expression expression = rnd.nextObject( Expression.class );
        target.deleteExpression( expression );
        verify( hibernateGenericStore ).delete( expression );
    }

    @Test
    void verifyExpressionIsAdded()
    {
        Expression expression = rnd.nextObject( Expression.class );
        long id = target.addExpression( expression );
        assertThat( id, is( expression.getId() ) );
        verify( hibernateGenericStore ).save( expression );
    }

    @Test
    void verifyAllExpressionsCanBeFetched()
    {
        when( hibernateGenericStore.getAll() ).thenReturn( Lists.newArrayList( rnd.nextObject( Expression.class ) ) );
        List<Expression> expressions = target.getAllExpressions();
        assertThat( expressions, hasSize( 1 ) );
        verify( hibernateGenericStore ).getAll();
    }

    @Test
    void testGetExpressionOrgUnitGroupCountGroups()
    {
        IndicatorType indicatorType = new IndicatorType( "A", 100, false );

        Indicator indicatorA = createIndicator( 'A', indicatorType );
        indicatorA.setAnnualized( true );
        indicatorA.setNumerator( expressionG );
        indicatorA.setDenominator( expressionH );

        when( idObjectManager.getByUid( OrganisationUnitGroup.class, Set.of( groupA.getUid() ) ) )
            .thenReturn( List.of( groupA ) );

        List<OrganisationUnitGroup> groups = target.getOrgUnitGroupCountGroups( List.of( indicatorA ) );
        assertThat( groups, hasSize( 1 ) );
        assertThat( groups, hasItem( groupA ) );

        groups = target.getOrgUnitGroupCountGroups( null );
        assertNotNull( groups );
        assertThat( groups, hasSize( 0 ) );
    }

    @Test
    void testGetExpressionOrgUnitGroupIds()
    {
        String expression = "if( orgUnit.group(groupUidABC,groupUidDEF), 1, 0)";
        Set<String> ids = target.getExpressionOrgUnitGroupIds( expression, PREDICTOR_EXPRESSION );
        assertThat( ids, hasSize( 2 ) );
        assertThat( ids, hasItem( "groupUidABC" ) );
        assertThat( ids, hasItem( "groupUidDEF" ) );

        ids = target.getExpressionOrgUnitGroupIds( null, PREDICTOR_EXPRESSION );
        assertNotNull( ids );
        assertThat( ids, hasSize( 0 ) );
    }

    @Test
    void testAnnualizedIndicatorValueWhenHavingMultiplePeriods()
    {
        Set<DimensionalItemId> itemIds = Sets.newHashSet( getId( opA ) );

        Map<DimensionalItemId, DimensionalItemObject> expectedItemMap = Map.of(
            getId( opA ), opA );

        when( dimensionService.getDataDimensionalItemObjectMap( itemIds ) ).thenReturn( expectedItemMap );

        mockConstantService();

        List<Period> periods = new ArrayList<>( 6 );

        periods.add( createPeriod( "200001" ) );
        periods.add( createPeriod( "200002" ) );
        periods.add( createPeriod( "200003" ) );
        periods.add( createPeriod( "200004" ) );
        periods.add( createPeriod( "200005" ) );
        periods.add( createPeriod( "200006" ) );

        IndicatorType indicatorType = new IndicatorType( "A", 100, false );

        Indicator indicatorA = createIndicator( 'A', indicatorType );
        indicatorA.setAnnualized( true );
        indicatorA.setNumerator( expressionE );
        indicatorA.setDenominator( expressionF );

        Map<DimensionalItemObject, Object> valueMap = new HashMap<>();

        valueMap.put( new DataElementOperand( deA, coc ), 12d );
        valueMap.put( new DataElementOperand( deB, coc ), 34d );
        valueMap.put( new DataElementOperand( deA, cocA, cocB ), 46d );
        valueMap.put( new DataElementOperand( deB, cocA ), 10d );

        Map<DimensionalItemId, DimensionalItemObject> itemMap = target
            .getIndicatorDimensionalItemMap( Arrays.asList( indicatorA ) );

        IndicatorValue value = target.getIndicatorValueObject(
            indicatorA, periods, itemMap, valueMap, null );

        assertNotNull( value );
        assertEquals( 24d, value.getNumeratorValue(), DELTA );
        assertEquals( 12d, value.getDenominatorValue(), DELTA );
        assertEquals( 36500, value.getMultiplier() );
        assertEquals( 182, value.getDivisor() );
        assertEquals( 200.55d, Precision.round( value.getFactor(), 2 ), DELTA );
        assertEquals( 401.1d, Precision.round( value.getValue(), 2 ), DELTA );
    }

    @Test
    void testAnnualizedIndicatorValueWhenHavingNullPeriods()
    {
        Set<DimensionalItemId> itemIds = Sets.newHashSet( getId( opA ) );

        Map<DimensionalItemId, DimensionalItemObject> expectedItemMap = Map.of(
            getId( opA ), opA );

        when( dimensionService.getDataDimensionalItemObjectMap( itemIds ) ).thenReturn( expectedItemMap );

        mockConstantService();

        IndicatorType indicatorType = new IndicatorType( "A", 100, false );

        Indicator indicatorA = createIndicator( 'A', indicatorType );
        indicatorA.setAnnualized( true );
        indicatorA.setNumerator( expressionE );
        indicatorA.setDenominator( expressionF );

        Map<DimensionalItemObject, Object> valueMap = new HashMap<>();

        valueMap.put( new DataElementOperand( deA, coc ), 12d );
        valueMap.put( new DataElementOperand( deB, coc ), 34d );
        valueMap.put( new DataElementOperand( deA, cocA, cocB ), 46d );
        valueMap.put( new DataElementOperand( deB, cocA ), 10d );

        Map<DimensionalItemId, DimensionalItemObject> itemMap = target
            .getIndicatorDimensionalItemMap( Arrays.asList( indicatorA ) );

        IndicatorValue value = target.getIndicatorValueObject(
            indicatorA, null, itemMap, valueMap, null );

        assertNotNull( value );
        assertEquals( 24d, value.getNumeratorValue(), DELTA );
        assertEquals( 12d, value.getDenominatorValue(), DELTA );
        assertEquals( 100, value.getMultiplier() );
        assertEquals( 1, value.getDivisor() );
        assertEquals( 100.0d, Precision.round( value.getFactor(), 2 ), DELTA );
        assertEquals( 200.0d, Precision.round( value.getValue(), 2 ), DELTA );
    }

    @Test
    void testGetNullWithoutNumeratorDataWithDenominatorData()
    {
        IndicatorType indicatorType = new IndicatorType( "A", 100, false );

        Indicator indicatorA = createIndicator( 'A', indicatorType );
        indicatorA.setNumerator( expressionF );
        indicatorA.setDenominator( expressionP );

        Map<DimensionalItemObject, Object> valueMap = new HashMap<>();

        valueMap.put( new DataElementOperand( deB, coc ), 12d );
        valueMap.put( new DataElementOperand( deC, coc ), 18d );

        Map<DimensionalItemId, DimensionalItemObject> itemMap = target
            .getIndicatorDimensionalItemMap( Arrays.asList( indicatorA ) );

        IndicatorValue value = target.getIndicatorValueObject(
            indicatorA, null, itemMap, valueMap, null );

        assertNull( value );
    }

    @Test
    void testGetNullWithNumeratorDataWithZeroDenominatorData()
    {
        IndicatorType indicatorType = new IndicatorType( "A", 100, false );

        Indicator indicatorA = createIndicator( 'A', indicatorType );
        indicatorA.setNumerator( expressionF );
        indicatorA.setDenominator( expressionP );

        Map<DimensionalItemObject, Object> valueMap = new HashMap<>();

        valueMap.put( new DataElementOperand( deA, coc ), 12d );
        valueMap.put( new DataElementOperand( deB, coc ), 0d );

        Map<DimensionalItemId, DimensionalItemObject> itemMap = target
            .getIndicatorDimensionalItemMap( Arrays.asList( indicatorA ) );

        IndicatorValue value = target.getIndicatorValueObject(
            indicatorA, null, itemMap, valueMap, null );

        assertNull( value );
    }
}
