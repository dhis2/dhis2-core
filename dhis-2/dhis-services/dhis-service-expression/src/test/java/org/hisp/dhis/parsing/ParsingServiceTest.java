package org.hisp.dhis.parsing;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.expression.Expression;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupService;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.AbstractMap.SimpleEntry;
import static org.junit.Assert.*;

/**
 * @author Jim Grace
 */
public class ParsingServiceTest
    extends DhisSpringTest
{
    @Autowired
    private ParsingService parsingService;

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private OrganisationUnitGroupService organisationUnitGroupService;

    @Autowired
    private DataSetService dataSetService;

    @Autowired
    private PeriodService periodService;

    private OrganisationUnit orgUnitA;
    private OrganisationUnit orgUnitB;
    private OrganisationUnit orgUnitC;
    private OrganisationUnit orgUnitD;
    private OrganisationUnit orgUnitE;
    private OrganisationUnit orgUnitF;
    private OrganisationUnit orgUnitG;
    private OrganisationUnit orgUnitH;
    private OrganisationUnit orgUnitI;
    private OrganisationUnit orgUnitJ;
    private OrganisationUnit orgUnitK;
    private OrganisationUnit orgUnitL;

    private OrganisationUnitGroup orgUnitGroupA;
    private OrganisationUnitGroup orgUnitGroupB;
    private OrganisationUnitGroup orgUnitGroupC;

    private DataSet dataSetA;
    private DataSet dataSetB;

    private Period periodA;
    private Period periodB;
    private Period periodC;
    private Period periodD;
    private Period periodE;
    private Period periodF;
    private Period periodG;
    private Period periodH;
    private Period periodI;
    private Period periodJ;
    private Period periodK;
    private Period periodL;
    private Period periodM;
    private Period periodN;
    private Period periodO;
    private Period periodP;

    private static DataElement dataElementA;
    private static DataElement dataElementB;

    private Map<ExpressionItem, Double> valueMap;

    private static final Map<String, Double> CONSTANT_MAP = new HashMap<String, Double>()
    {{
        put( "xxxxxxxxx05", 0.5 );
        put( "xxxxxxxx025", 0.25 );
    }};

    private static final Map<String, Integer> ORG_UNIT_COUNT_MAP = new HashMap<String, Integer>()
    {{
        put( "dataSetAUid", 1000000 );
        put( "dataSetBUid", 2000000 );
    }};

    private final static int DAYS = 30;

    // -------------------------------------------------------------------------
    // Fixture
    // -------------------------------------------------------------------------

    @Override
    public void setUpTest()
        throws Exception
    {
        dataElementA = createDataElement( 'A' );
        dataElementB = createDataElement( 'B' );

        dataElementA.setUid( "dataElemenA" );
        dataElementB.setUid( "dataElemenB" );

        dataElementA.setAggregationType( AggregationType.SUM );

        dataElementService.addDataElement( dataElementA );
        dataElementService.addDataElement( dataElementB );

        orgUnitA = createOrganisationUnit( 'A' );
        orgUnitB = createOrganisationUnit( 'B', orgUnitA );
        orgUnitC = createOrganisationUnit( 'C', orgUnitA );
        orgUnitD = createOrganisationUnit( 'D', orgUnitA );
        orgUnitE = createOrganisationUnit( 'E', orgUnitB );
        orgUnitF = createOrganisationUnit( 'F', orgUnitC );
        orgUnitG = createOrganisationUnit( 'G', orgUnitC );
        orgUnitH = createOrganisationUnit( 'H', orgUnitC );
        orgUnitI = createOrganisationUnit( 'I', orgUnitD );
        orgUnitJ = createOrganisationUnit( 'J', orgUnitG );
        orgUnitK = createOrganisationUnit( 'K', orgUnitG );
        orgUnitL = createOrganisationUnit( 'L', orgUnitJ );

        orgUnitA.setUid( "OrgUnitUidA" );
        orgUnitB.setUid( "OrgUnitUidB" );
        orgUnitC.setUid( "OrgUnitUidC" );
        orgUnitD.setUid( "OrgUnitUidD" );
        orgUnitE.setUid( "OrgUnitUidE" );
        orgUnitF.setUid( "OrgUnitUidF" );
        orgUnitG.setUid( "OrgUnitUidG" );
        orgUnitH.setUid( "OrgUnitUidH" );
        orgUnitI.setUid( "OrgUnitUidI" );
        orgUnitJ.setUid( "OrgUnitUidJ" );
        orgUnitK.setUid( "OrgUnitUidK" );
        orgUnitL.setUid( "OrgUnitUidL" );

        organisationUnitService.addOrganisationUnit( orgUnitA );
        organisationUnitService.addOrganisationUnit( orgUnitB );
        organisationUnitService.addOrganisationUnit( orgUnitC );
        organisationUnitService.addOrganisationUnit( orgUnitD );
        organisationUnitService.addOrganisationUnit( orgUnitE );
        organisationUnitService.addOrganisationUnit( orgUnitF );
        organisationUnitService.addOrganisationUnit( orgUnitG );
        organisationUnitService.addOrganisationUnit( orgUnitH );
        organisationUnitService.addOrganisationUnit( orgUnitI );
        organisationUnitService.addOrganisationUnit( orgUnitJ );
        organisationUnitService.addOrganisationUnit( orgUnitK );
        organisationUnitService.addOrganisationUnit( orgUnitL );

        orgUnitGroupA = createOrganisationUnitGroup( 'A' );
        orgUnitGroupB = createOrganisationUnitGroup( 'B' );
        orgUnitGroupC = createOrganisationUnitGroup( 'C' );

        orgUnitGroupA.setUid( "OrgUnitGrpA" );
        orgUnitGroupB.setUid( "OrgUnitGrpB" );
        orgUnitGroupC.setUid( "OrgUnitGrpC" );

        orgUnitGroupA.addOrganisationUnit( orgUnitB );
        orgUnitGroupA.addOrganisationUnit( orgUnitC );
        orgUnitGroupA.addOrganisationUnit( orgUnitE );
        orgUnitGroupA.addOrganisationUnit( orgUnitF );
        orgUnitGroupA.addOrganisationUnit( orgUnitG );

        orgUnitGroupB.addOrganisationUnit( orgUnitF );
        orgUnitGroupB.addOrganisationUnit( orgUnitG );
        orgUnitGroupB.addOrganisationUnit( orgUnitH );

        orgUnitGroupC.addOrganisationUnit( orgUnitC );
        orgUnitGroupC.addOrganisationUnit( orgUnitD );
        orgUnitGroupC.addOrganisationUnit( orgUnitG );
        orgUnitGroupC.addOrganisationUnit( orgUnitH );
        orgUnitGroupC.addOrganisationUnit( orgUnitI );

        organisationUnitGroupService.addOrganisationUnitGroup( orgUnitGroupA );
        organisationUnitGroupService.addOrganisationUnitGroup( orgUnitGroupB );
        organisationUnitGroupService.addOrganisationUnitGroup( orgUnitGroupC );

        dataSetA = createDataSet( 'A' );
        dataSetB = createDataSet( 'B' );

        dataSetA.setUid( "DataSetUidA" );
        dataSetB.setUid( "DataSetUidB" );

        dataSetA.addOrganisationUnit( orgUnitE );
        dataSetA.addOrganisationUnit( orgUnitH );
        dataSetA.addOrganisationUnit( orgUnitI );

        dataSetB.addOrganisationUnit( orgUnitF );
        dataSetB.addOrganisationUnit( orgUnitG );
        dataSetB.addOrganisationUnit( orgUnitI );

        dataSetService.addDataSet( dataSetA );
        dataSetService.addDataSet( dataSetB );

        periodA = createPeriod( "200105" );
        periodB = createPeriod( "200106" );
        periodC = createPeriod( "200107" );

        periodD = createPeriod( "200205" );
        periodE = createPeriod( "200206" );
        periodF = createPeriod( "200207" );

        periodG = createPeriod( "200301" );
        periodH = createPeriod( "200302" );
        periodI = createPeriod( "200303" );
        periodJ = createPeriod( "200304" );
        periodK = createPeriod( "200305" );
        periodL = createPeriod( "200306" );
        periodM = createPeriod( "200307" );

        periodN = createPeriod( "200405" );
        periodO = createPeriod( "200406" );
        periodP = createPeriod( "200407" );

        periodService.addPeriod( periodA );
        periodService.addPeriod( periodB );
        periodService.addPeriod( periodC );
        periodService.addPeriod( periodD );
        periodService.addPeriod( periodE );
        periodService.addPeriod( periodF );
        periodService.addPeriod( periodG );
        periodService.addPeriod( periodH );
        periodService.addPeriod( periodI );
        periodService.addPeriod( periodJ );
        periodService.addPeriod( periodK );
        periodService.addPeriod( periodL );
        periodService.addPeriod( periodM );
        periodService.addPeriod( periodN );
        periodService.addPeriod( periodO );
        periodService.addPeriod( periodP );

        valueMap = entries(
            entrySum( orgUnitA, periodK, dataElementA, 0.5 ),
            entrySum( orgUnitA, periodL, dataElementA, 0.25 ),
            entrySum( orgUnitA, periodM, dataElementA, 0.125 ),

            entrySum( orgUnitB, periodK, dataElementA, 1.5 ),
            entrySum( orgUnitB, periodL, dataElementA, 2.25 ),
            entrySum( orgUnitB, periodM, dataElementA, 4.125 ),

            entrySum( orgUnitC, periodK, dataElementA, 10.5 ),
            entrySum( orgUnitC, periodL, dataElementA, 20.25 ),
            entrySum( orgUnitC, periodM, dataElementA, 40.125 ),

            entrySum( orgUnitD, periodK, dataElementA, 100.5 ),
            entrySum( orgUnitD, periodL, dataElementA, 200.25 ),
            entrySum( orgUnitD, periodM, dataElementA, 400.125 ),

            entrySum( orgUnitE, periodK, dataElementA, 1.0 ),
            entrySum( orgUnitE, periodL, dataElementA, 2.0 ),
            entrySum( orgUnitE, periodM, dataElementA, 4.0 ),

            entrySum( orgUnitF, periodK, dataElementA, 100.0 ),
            entrySum( orgUnitF, periodL, dataElementA, 200.0 ),
            entrySum( orgUnitF, periodM, dataElementA, 400.0 ),

            entrySum( orgUnitG, periodA, dataElementA, 100000.0 ),
            entrySum( orgUnitG, periodB, dataElementA, 40000.0 ),
            entrySum( orgUnitG, periodC, dataElementA, 20000.0 ),
            entrySum( orgUnitG, periodD, dataElementA, 10000.0 ),
            entrySum( orgUnitG, periodE, dataElementA, 4000.0 ),
            entrySum( orgUnitG, periodF, dataElementA, 2000.0 ),

            entryAgg( orgUnitG, periodG, dataElementA, 1000.0 ),
            entryAgg( orgUnitG, periodH, dataElementA, 400.0 ),
            entryAgg( orgUnitG, periodI, dataElementA, 200.0 ),
            entryAgg( orgUnitG, periodJ, dataElementA, 100.0 ),
            entryAgg( orgUnitG, periodK, dataElementA, 40.0 ),
            entryAgg( orgUnitG, periodL, dataElementA, 20.0 ),
            entryAgg( orgUnitG, periodM, dataElementA, 10.0 ),

            entrySum( orgUnitG, periodN, dataElementA, 4.0 ),
            entrySum( orgUnitG, periodO, dataElementA, 2.0 ),
            entrySum( orgUnitG, periodP, dataElementA, 1.0 ),

            entrySum( orgUnitH, periodK, dataElementA, 1000.0 ),
            entrySum( orgUnitH, periodL, dataElementA, 2000.0 ),
            entrySum( orgUnitH, periodM, dataElementA, 4000.0 ),

            entrySum( orgUnitI, periodK, dataElementA, 10000.0 ),
            entrySum( orgUnitI, periodL, dataElementA, 20000.0 ),
            entrySum( orgUnitI, periodM, dataElementA, 40000.0 ),

            entrySum( orgUnitJ, periodL, dataElementA, 11.0 ),

            entrySum( orgUnitK, periodL, dataElementA, 22.0 ),

            entrySum( orgUnitL, periodL, dataElementA, 55.0 ),

            entrySum( orgUnitG, periodK, dataElementB, 1.5 ),
            entrySum( orgUnitG, periodL, dataElementB, 2.25 ),
            entrySum( orgUnitG, periodM, dataElementB, 4.125 )

        );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private <K, V> Map<K, V> entries( List<Map.Entry<K, V>>... entries )
    {
        Map<K, V> map = new HashMap<K, V>();

        for ( List<Map.Entry<K, V>> list : entries )
        {
            for ( Map.Entry<K, V> entry : list )
            {
                map.put( entry.getKey(), entry.getValue() );
            }
        }

        return map;
    }

    private <K, V> Map.Entry<K, V> singleEntry( K k, V v )
    {
        return new SimpleEntry<K, V>( k, v );
    }

    private List<Map.Entry<ExpressionItem, Double>> entrySum( OrganisationUnit orgUnit,
        Period period, DimensionalItemObject item, Double value )
    {
        return ImmutableList.of(
            singleEntry( new ExpressionItem( orgUnit, period, item, AggregationType.SUM ), value )
        );
    }

    private List<Map.Entry<ExpressionItem, Double>> entryAgg( OrganisationUnit orgUnit,
        Period period, DimensionalItemObject item, Double value )
    {
        return ImmutableList.of(
            singleEntry( new ExpressionItem( orgUnit, period, item, AggregationType.SUM ), value ),
            singleEntry( new ExpressionItem( orgUnit, period, item, AggregationType.MAX ), value + 1 ),
            singleEntry( new ExpressionItem( orgUnit, period, item, AggregationType.MIN ), value + 2 ),
            singleEntry( new ExpressionItem( orgUnit, period, item, AggregationType.AVERAGE ), value + 3 ),
            singleEntry( new ExpressionItem( orgUnit, period, item, AggregationType.STDDEV ), value + 4 ),
            singleEntry( new ExpressionItem( orgUnit, period, item, AggregationType.VARIANCE ), value + 5 ),
            singleEntry( new ExpressionItem( orgUnit, period, item, AggregationType.LAST ), value + 6 )
        );
    }

    /**
     * Evaluates a test expression, both against getItemsInExpression and
     * getExpressionValue. Returns a string containing first the returned
     * value from getExpressionValue, and then the items returned from
     * getItemsInExpression, if any, separated by spaces. For example,
     * "10.2 F306deB G307deB" means that the value returned from
     * getExpressionValue was 10.0, and getItemsInExpression requested
     * the values from (orgUnitF, period 200306, dataElementB)
     * and (orgUnitG, period 200307, dataElementB).
     * <p/>
     * The value from getExpressionValue is always a Double. If it is null,
     * "null" is returned. If it represents an integer value, just the
     * integer is returned. (This improves readabiliy for test cases not
     * having to add ".0" to each integral value.)
     * <p/>
     * The items returned from getItemsInExpression, if any, are returned
     * in abbreviated format for ease of comparing. Each item has the
     * following format:
     * <li>
     * <le>org unit letter</le>
     * <le>last digit of year and 2 digits of month</le>
     * <le>item type abbreviation and identifying letter</le>
     * </li>
     * For example, G306daB means orgUnitG, period 200306,
     * dataElementB. Since dataElementA is used frequently for testing
     * period and org unit functions, it is omitted from the item string --
     * so G306 means orgUnitG, period 200306, dataElementA.
     *
     * @param expr expression to evaluate
     * @return result from getItemsInExpression and getExpressionValue
     */
    private String eval( String expr )
    {
        Expression expression = new Expression( expr, expr );

        Set<ExpressionItem> items = parsingService.getExpressionItems(
            Arrays.asList( expression ), Arrays.asList( orgUnitG ), Arrays.asList( periodL ),
            CONSTANT_MAP, ORG_UNIT_COUNT_MAP );

        Double value = parsingService.getExpressionValue( expression, orgUnitG, periodL,
            valueMap, CONSTANT_MAP, ORG_UNIT_COUNT_MAP, DAYS );

        return result( value, items );
    }

    /**
     * Formats the result from getItemsInExpression and getExpressionValue
     *
     * @param value the value retuned from getExpressionValue
     * @param items the items returned from getExpressionItems
     * @return the result string
     */
    private String result( Double value, Set<ExpressionItem> items )
    {
        String valueString;

        if ( value == null )
        {
            valueString = "null";
        }
        else if ( value == (double) value.intValue() )
        {
            valueString = Integer.toString( value.intValue() );
        }
        else
        {
            valueString = value.toString();
        }

        List<String> itemAbbreviations = getItemAbbreviations( items );

        String itemsString = String.join( " ", itemAbbreviations );

        if ( itemsString.length() != 0 )
        {
            itemsString = " " + itemsString;
        }

        return valueString + itemsString;
    }

    /**
     * Gets a list of item abbrevivations returned by getExpressionItems
     *
     * @param items the items returned by getItemsInExpression
     * @return list of abbreviations to display in results
     */
    private List<String> getItemAbbreviations( Set<ExpressionItem> items )
    {
        List<String> itemAbbreviations = new ArrayList<>();

        for ( ExpressionItem item : items )
        {
            String ou = item.getOrgUnit().getUid().substring( 10 );
            String pe = item.getPeriod().getIsoDate().substring( 3 );
            String it = getTypeAbbreviation( item.getDimensionalItemObject() )
                + item.getDimensionalItemObject().getUid().substring( 10 );

            String agg = item.getAggregationType() == null ? "" : "-" + item.getAggregationType().name();

            if ( agg.equals( "-SUM" ) )
            {
                agg = "-"; // Shorthand for -SUM.
            }

            if ( it.equals( "deA" ) )
            {
                it = "";
            }

            itemAbbreviations.add( ou + pe + it + agg );
        }

        Collections.sort( itemAbbreviations );

        return itemAbbreviations;
    }

    /**
     * Gets the DimensionItemType abbreviation for display in results
     *
     * @param object the DimensionalItemObject
     * @return abbreviation based on the DimensionalItemObject's type
     */
    private String getTypeAbbreviation( DimensionalItemObject object )
    {
        switch ( object.getDimensionItemType() )
        {
            case DATA_ELEMENT:
                return "de";

            case DATA_ELEMENT_OPERAND:
                return "deo";

            default:
                return "???";
        }
    }

    /**
     * Make sure the expression causes an error
     *
     * @param expr The expression to test
     * @return null if error, otherwise expression description
     */
    private String error( String expr )
    {
        String description;

        try
        {
            parsingService.getExpressionDescription( expr );
        }
        catch ( ParsingException ex )
        {
            return null;
        }

        return "Unexpected success: " + expr;
    }

    // -------------------------------------------------------------------------
    // Test getExpressionValue
    // -------------------------------------------------------------------------

    @Test
    public void testExpressionNumeric()
    {
        // Numeric constants

        assertEquals( "2", eval( "2" ) );
        assertEquals( "2", eval( "2." ) );
        assertEquals( "2", eval( "2.0" ) );
        assertEquals( "2.1", eval( "2.1" ) );
        assertEquals( "0.2", eval( "0.2" ) );
        assertEquals( "0.2", eval( ".2" ) );
        assertEquals( "2", eval( "2E0" ) );
        assertEquals( "2", eval( "2e0" ) );
        assertEquals( "2", eval( "2.E0" ) );
        assertEquals( "2", eval( "2.0E0" ) );
        assertEquals( "2.1", eval( "2.1E0" ) );
        assertEquals( "2.1", eval( "2.1E+0" ) );
        assertEquals( "2.1", eval( "2.1E-0" ) );
        assertEquals( "0.21", eval( "2.1E-1" ) );
        assertEquals( "0.021", eval( "2.1E-2" ) );
        assertEquals( "20", eval( "2E1" ) );
        assertEquals( "20", eval( "2E+1" ) );
        assertEquals( "20", eval( "2E01" ) );
        assertEquals( "200", eval( "2E2" ) );
        assertEquals( "2", eval( "+2" ) );
        assertEquals( "-2", eval( "-2" ) );

        // Numeric operators in precedence order:

        // Exponentiation (right-to-left)

        assertEquals( "512", eval( "2 ^ 3 ^ 2" ) );
        assertEquals( "64", eval( "( 2 ^ 3 ) ^ 2" ) );
        assertEquals( "0.25", eval( "2 ^ -2" ) );

        // Unary +, -

        assertEquals( "5", eval( "+ (2 + 3)" ) );
        assertEquals( "-5", eval( "- (2 + 3)" ) );

        // Unary +, - after Exponentiation

        assertEquals( "-4", eval( "-(2) ^ 2" ) );
        assertEquals( "4", eval( "(-(2)) ^ 2" ) );
        assertEquals( "4", eval( "+(2) ^ 2" ) );

        // Multiply, divide, modulus (left-to-right)

        assertEquals( "24", eval( "2 * 3 * 4" ) );
        assertEquals( "2", eval( "12 / 3 / 2" ) );
        assertEquals( "8", eval( "12 / ( 3 / 2 )" ) );
        assertEquals( "2", eval( "12 % 5 % 3" ) );
        assertEquals( "0", eval( "12 % ( 5 % 3 )" ) );
        assertEquals( "8", eval( "12 / 3 * 2" ) );
        assertEquals( "2", eval( "12 / ( 3 * 2 )" ) );
        assertEquals( "3", eval( "5 % 2 * 3" ) );
        assertEquals( "1", eval( "3 * 5 % 2" ) );
        assertEquals( "1.5", eval( "7 % 4 / 2" ) );
        assertEquals( "1", eval( "9 / 3 % 2" ) );

        // Multiply, divide, modulus after Unary +, -

        assertEquals( "-6", eval( "-(3) * 2" ) );
        assertEquals( "-6", eval( "-(3 * 2)" ) );
        assertEquals( "-1.5", eval( "-(3) / 2" ) );
        assertEquals( "-1.5", eval( "-(3 / 2)" ) );
        assertEquals( "-1", eval( "-(7) % 3" ) );
        assertEquals( "-1", eval( "-(7 % 3)" ) );

        // Add, subtract (left-to-right)

        assertEquals( "9", eval( "2 + 3 + 4" ) );
        assertEquals( "9", eval( "2 + ( 3 + 4 )" ) );
        assertEquals( "-5", eval( "2 - 3 - 4" ) );
        assertEquals( "3", eval( "2 - ( 3 - 4 )" ) );
        assertEquals( "3", eval( "2 - 3 + 4" ) );
        assertEquals( "-5", eval( "2 - ( 3 + 4 )" ) );

        // Add, subtract after Multiply, divide, modulus

        assertEquals( "10", eval( "4 + 3 * 2" ) );
        assertEquals( "14", eval( "( 4 + 3 ) * 2" ) );
        assertEquals( "5.5", eval( "4 + 3 / 2" ) );
        assertEquals( "3.5", eval( "( 4 + 3 ) / 2" ) );
        assertEquals( "5", eval( "4 + 3 % 2" ) );
        assertEquals( "1", eval( "( 4 + 3 ) % 2" ) );

        assertEquals( "-2", eval( "4 - 3 * 2" ) );
        assertEquals( "2", eval( "( 4 - 3 ) * 2" ) );
        assertEquals( "2.5", eval( "4 - 3 / 2" ) );
        assertEquals( "0.5", eval( "( 4 - 3 ) / 2" ) );
        assertEquals( "3", eval( "4 - 3 % 2" ) );
        assertEquals( "1", eval( "( 4 - 3 ) % 2" ) );

        // Comparisons (left-to-right)

        assertEquals( "1", eval( "if(1 < 2, 1, 0)" ) );
        assertEquals( "0", eval( "if(1 < 1, 1, 0)" ) );
        assertEquals( "0", eval( "if(2 < 1, 1, 0)" ) );

        assertEquals( "0", eval( "if(1 > 2, 1, 0)" ) );
        assertEquals( "0", eval( "if(1 > 1, 1, 0)" ) );
        assertEquals( "1", eval( "if(2 > 1, 1, 0)" ) );

        assertEquals( "1", eval( "if(1 <= 2, 1, 0)" ) );
        assertEquals( "1", eval( "if(1 <= 1, 1, 0)" ) );
        assertEquals( "0", eval( "if(2 <= 1, 1, 0)" ) );

        assertEquals( "0", eval( "if(1 >= 2, 1, 0)" ) );
        assertEquals( "1", eval( "if(1 >= 1, 1, 0)" ) );
        assertEquals( "1", eval( "if(2 >= 1, 1, 0)" ) );

        // Comparisons after Add, subtract

        assertEquals( "0", eval( "if(5 < 2 + 3, 1, 0)" ) );
        assertEquals( "0", eval( "if(5 > 2 + 3, 1, 0)" ) );
        assertEquals( "1", eval( "if(5 <= 2 + 3, 1, 0)" ) );
        assertEquals( "1", eval( "if(5 >= 2 + 3, 1, 0)" ) );

        assertEquals( "0", eval( "if(5 < 8 - 3, 1, 0)" ) );
        assertEquals( "0", eval( "if(5 > 8 - 3, 1, 0)" ) );
        assertEquals( "1", eval( "if(5 <= 8 - 3, 1, 0)" ) );
        assertEquals( "1", eval( "if(5 >= 8 - 3, 1, 0)" ) );

        assertNull( error( "if((5 < 2) + 3, 1, 0)" ) );
        assertNull( error( "if((5 > 2) + 3, 1, 0)" ) );
        assertNull( error( "if((5 <= 2) + 3, 1, 0)" ) );
        assertNull( error( "if((5 >= 2) + 3, 1, 0)" ) );

        assertNull( error( "if((5 < 8) - 3, 1, 0)" ) );
        assertNull( error( "if((5 > 8) - 3, 1, 0)" ) );
        assertNull( error( "if((5 <= 8) - 3, 1, 0)" ) );
        assertNull( error( "if((5 >= 8) - 3, 1, 0)" ) );

        // Equality

        assertEquals( "1", eval( "if(1 == 1, 1, 0)" ) );
        assertEquals( "0", eval( "if(1 == 2, 1, 0)" ) );

        assertEquals( "0", eval( "if(1 != 1, 1, 0)" ) );
        assertEquals( "1", eval( "if(1 != 2, 1, 0)" ) );

        // Equality after Comparisons

        assertEquals( "1", eval( "if(1 + 2 == 3, 1, 0)" ) );
        assertEquals( "0", eval( "if(1 + 2 != 3, 1, 0)" ) );

        assertNull( error( "if(1 + (2 == 3), 1, 0)" ) );
        assertNull( error( "if(1 + (2 != 3), 1, 0)" ) );
    }

    @Test
    public void testExpressionString()
    {
        // Concatenation (and constants)

        assertEquals( "1", eval( "if( \"abc123\" == \"abc\" + \"123\", 1, 0)" ) );

        // Comparisons

        assertEquals( "0", eval( "if( \"a\" < \"a\", 1, 0)" ) );
        assertEquals( "1", eval( "if( \"a\" < \"b\", 1, 0)" ) );
        assertEquals( "0", eval( "if( \"b\" < \"a\", 1, 0)" ) );

        assertEquals( "0", eval( "if( \"a\" > \"a\", 1, 0)" ) );
        assertEquals( "0", eval( "if( \"a\" > \"b\", 1, 0)" ) );
        assertEquals( "1", eval( "if( \"b\" > \"a\", 1, 0)" ) );

        assertEquals( "1", eval( "if( \"a\" <= \"a\", 1, 0)" ) );
        assertEquals( "1", eval( "if( \"a\" <= \"b\", 1, 0)" ) );
        assertEquals( "0", eval( "if( \"b\" <= \"a\", 1, 0)" ) );

        assertEquals( "1", eval( "if( \"a\" >= \"a\", 1, 0)" ) );
        assertEquals( "0", eval( "if( \"a\" >= \"b\", 1, 0)" ) );
        assertEquals( "1", eval( "if( \"b\" >= \"a\", 1, 0)" ) );

        // Comparisons after Concatenation

        assertEquals( "0", eval( "if( \"ab\" < \"a\" + \"b\", 1, 0)" ) );
        assertEquals( "0", eval( "if( \"ab\" > \"a\" + \"b\", 1, 0)" ) );
        assertEquals( "1", eval( "if( \"ab\" <= \"a\" + \"b\", 1, 0)" ) );
        assertEquals( "1", eval( "if( \"ab\" >= \"a\" + \"b\", 1, 0)" ) );

        assertNull( error( "if( (\"a\" < \"a\") + \"b\", 1, 0)" ) );
        assertNull( error( "if( (\"a\" > \"a\") + \"b\", 1, 0)" ) );
        assertNull( error( "if( (\"a\" <= \"a\") + \"b\", 1, 0)" ) );
        assertNull( error( "if( (\"a\" >= \"a\") + \"b\", 1, 0)" ) );

        // Equality

        assertEquals( "1", eval( "if( \"a\" == \"a\", 1, 0)" ) );
        assertEquals( "0", eval( "if( \"a\" == \"b\", 1, 0)" ) );

        assertEquals( "0", eval( "if( \"a\" != \"a\", 1, 0)" ) );
        assertEquals( "1", eval( "if( \"a\" != \"b\", 1, 0)" ) );
    }

    @Test
    public void testExpressionBoolean()
    {
        // Boolean constants

        assertEquals( "1", eval( "if( true, 1, 0)" ) );
        assertEquals( "0", eval( "if( false, 1, 0)" ) );

        // Unary not

        assertEquals( "0", eval( "if( ! true, 1, 0)" ) );
        assertEquals( "1", eval( "if( ! false, 1, 0)" ) );

        // Comparison

        assertEquals( "0", eval( "if( true < true, 1, 0)" ) );
        assertEquals( "0", eval( "if( true < false, 1, 0)" ) );
        assertEquals( "1", eval( "if( false < true, 1, 0)" ) );

        assertEquals( "0", eval( "if( true > true, 1, 0)" ) );
        assertEquals( "1", eval( "if( true > false, 1, 0)" ) );
        assertEquals( "0", eval( "if( false > true, 1, 0)" ) );

        assertEquals( "1", eval( "if( true <= true, 1, 0)" ) );
        assertEquals( "0", eval( "if( true <= false, 1, 0)" ) );
        assertEquals( "1", eval( "if( false <= true, 1, 0)" ) );

        assertEquals( "1", eval( "if( true >= true, 1, 0)" ) );
        assertEquals( "1", eval( "if( true >= false, 1, 0)" ) );
        assertEquals( "0", eval( "if( false >= true, 1, 0)" ) );

        // Comparison after Unary not

        assertEquals( "0", eval( "if( ! true < false, 1, 0)" ) );
        assertEquals( "0", eval( "if( ! true > false, 1, 0)" ) );
        assertEquals( "1", eval( "if( ! true <= false, 1, 0)" ) );
        assertEquals( "1", eval( "if( ! true >= false, 1, 0)" ) );

        assertEquals( "0", eval( "if( ! ( true >= false ), 1, 0)" ) );
        assertEquals( "0", eval( "if( ! ( true > false ), 1, 0)" ) );
        assertEquals( "1", eval( "if( ! ( true <= false ), 1, 0)" ) );
        assertEquals( "1", eval( "if( ! ( true < false ), 1, 0)" ) );

        // Equality (associative, left/right parsing direction doesn't matter)

        assertEquals( "1", eval( "if( true == true, 1, 0)" ) );
        assertEquals( "0", eval( "if( true == false, 1, 0)" ) );

        assertEquals( "0", eval( "if( true != true, 1, 0)" ) );
        assertEquals( "1", eval( "if( true != false, 1, 0)" ) );

        assertEquals( "1", eval( "if( true == false == false, 1, 0)" ) );
    }

    @Test
    public void testExpressionPeriods()
    {
        // Period letters, and values for (dataElementA, orgUnitG):
        //
        //              Month
        //               Jan    Feb    Mar    Apr    May    Jun    Jul
        //  Year 2001                                 A      B      C
        //                                        100,000 40,000 20,000
        //       2002                                 D      E      F
        //                                         10,000  4,000  2,000
        //       2003     G      H      I      J      K      L      M
        //              1,000   400    200    100    40     20     10
        //       2004                                N      O      P
        //                                           4      2      1

        assertEquals( "10 G307-", eval( "#{dataElemenA}.period( 1 )" ) );
        assertEquals( "20 G306-", eval( "#{dataElemenA}.period( 0 )" ) );
        assertEquals( "40 G305-", eval( "#{dataElemenA}.period( -1 )" ) );
        assertEquals( "4000 G206-", eval( "#{dataElemenA}.period( -12 )" ) );

        assertEquals( "10 G307-", eval( "#{dataElemenA}.period( 1, 1 ).sum()" ) );
        assertEquals( "30 G306- G307-", eval( "#{dataElemenA}.period( 0, 1 ).sum()" ) );
        assertEquals( "70 G305- G306- G307-", eval( "#{dataElemenA}.period( -1, 1 ).sum()" ) );
        assertEquals( "7770 G206- G207- G208- G209- G210- G211- G212- G301- G302- G303- G304- G305- G306- G307-", eval( "#{dataElemenA}.period( -12, 1 ).sum()" ) );

        assertEquals( "2000 G207-", eval( "#{dataElemenA}.period( 1, 1, -1 ).sum()" ) );
        assertEquals( "6000 G206- G207-", eval( "#{dataElemenA}.period( 0, 1, -1 ).sum()" ) );
        assertEquals( "160000 G105- G106- G107-", eval( "#{dataElemenA}.period( -1, 1, -2 ).sum()" ) );
        assertEquals( "7770 G206- G207- G208- G209- G210- G211- G212- G301- G302- G303- G304- G305- G306- G307-", eval( "#{dataElemenA}.period( -12, 1, 0 ).sum()" ) );

        assertEquals( "7770 G206- G207- G208- G209- G210- G211- G212- G301- G302- G303- G304- G305- G306- G307-", eval( "#{dataElemenA}.period( -12, 1, 0, 0 ).sum()" ) );
        assertEquals( "16077 G205- G206- G207- G305- G306- G307- G405- G406- G407-", eval( "#{dataElemenA}.period( -1, 1, -1, 1 ).sum()" ) );
        assertEquals( "44022 G106- G206- G306- G406-", eval( "#{dataElemenA}.period( 0, 0, -2, 1 ).sum()" ) );

        assertEquals( "7770 G206- G207- G208- G209- G210- G211- G212- G301- G302- G303- G304- G305- G306- G307-", eval( "#{dataElemenA}.period( -12, 0, 0, 0,   1 ).sum()" ) );
        assertEquals( "10040 G205- G305-", eval( "#{dataElemenA}.period( -1, -1, -1, -1,   -1 ).sum()" ) );

        assertEquals( "10040 G205- G305-", eval( "#{dataElemenA}.period( -1, -1, -1, -1,   -1, -1 ).sum()" ) );
        assertEquals( "66666 G106- G107- G206- G207- G302- G303- G305- G306- G405- G406-", eval( "#{dataElemenA}.period( -4, -3, 0, 0,   0, 1, -2, -1,   -1, 0, 0, 1).sum()" ) );

        assertEquals( "null G308-", eval( "#{dataElemenA}.period( 2 )" ) );
        assertEquals( "null G308- G309- G310-", eval( "#{dataElemenA}.period( 2, 4 ).sum()" ) );
        assertEquals( "null G201- G202- G203-", eval( "#{dataElemenA}.period( -17, -15 ).sum()" ) );
        assertEquals( "null G201- G202- G203- G204-", eval( "#{dataElemenA}.period( -5, -2, -1 ).sum()" ) );
        assertEquals( "null G101- G102- G103- G104- G201- G202- G203- G204-", eval( "#{dataElemenA}.period( -5, -2, -2, -1 ).sum()" ) );
        assertEquals( "null G101- G102- G103- G104- G201- G202- G203- G204- G308-", eval( "#{dataElemenA}.period( -5, -2, -2, -1,   2 ).sum()" ) );
        assertEquals( "null G101- G102- G103- G104- G201- G202- G203- G204- G308- G309- G310-", eval( "#{dataElemenA}.period( -5, -2, -2, -1,   2, 4 ).sum()" ) );
        assertEquals( "null G101- G102- G103- G104- G201- G202- G203- G204- G408- G409- G410-", eval( "#{dataElemenA}.period( -5, -2, -2, -1,   2, 4, 1 ).sum()" ) );
        assertEquals( "null G101- G102- G103- G104- G201- G202- G203- G204- G408- G409- G410- G508- G509- G510-", eval( "#{dataElemenA}.period( -5, -2, -2, -1,   2, 4, 1, 2 ).sum()" ) );
    }

    @Test
    public void testExpressionOrganisationUnits()
    {
        // Org unit letters, and values for (dataElementA, June 2003):
        //
        // Level 1             A
        //                    0.25
        //                  /  |   \
        // Level 2        B    C    D
        //             2.25  20.25  200.25
        //            /      / |  \     \
        // Level 3   E     F   G   H     I
        //           2   200  20  2000  20,000
        //                    /\
        // Level 4           J  K
        //                  11 22
        //                   |
        // Level 5           L
        //                  55

        // ouAncestor

        assertEquals( "20 G306-", eval( "#{dataElemenA}.ouAncestor( 0 )" ) );
        assertEquals( "20.25 C306-", eval( "#{dataElemenA}.ouAncestor( 1 )" ) );
        assertEquals( "0.25 A306-", eval( "#{dataElemenA}.ouAncestor( 2 )" ) );

        // ouDescendant

        assertEquals( "20 G306-", eval( "#{dataElemenA}.ouDescendant( 0 ).sum()" ) );
        assertEquals( "33 J306- K306-", eval( "#{dataElemenA}.ouDescendant( 1 ).sum()" ) );
        assertEquals( "55 L306-", eval( "#{dataElemenA}.ouDescendant( 2 ).sum()" ) );
        assertEquals( "53 G306- J306- K306-", eval( "#{dataElemenA}.ouDescendant( 0, 1 ).sum()" ) );
        assertEquals( "75 G306- L306-", eval( "#{dataElemenA}.ouDescendant( 0, 2 ).sum()" ) );
        assertEquals( "88 J306- K306- L306-", eval( "#{dataElemenA}.ouDescendant( 1, 2 ).sum()" ) );
        assertEquals( "108 G306- J306- K306- L306-", eval( "#{dataElemenA}.ouDescendant( 0, 1, 2 ).sum()" ) );
        assertEquals( "108 G306- J306- K306- L306-", eval( "#{dataElemenA}.ouDescendant( 0, 1, 2, 3 ).sum()" ) );

        assertEquals( "2220 F306- G306- H306-", eval( "#{dataElemenA}.ouDescendant( 1 ).ouAncestor( 1 ).sum()" ) );
        assertEquals( "22222 E306- F306- G306- H306- I306-", eval( "#{dataElemenA}.ouDescendant( 2 ).ouAncestor( 2 ).sum()" ) );
        assertEquals( "222.75 B306- C306- D306-", eval( "#{dataElemenA}.ouDescendant( 1 ).ouAncestor( 2 ).sum()" ) );
        assertEquals( "22255 E306- F306- G306- H306- I306- J306- K306-", eval( "#{dataElemenA}.ouDescendant( 2, 3 ).ouAncestor( 2 ).sum()" ) );
        assertEquals( "22310 E306- F306- G306- H306- I306- J306- K306- L306-", eval( "#{dataElemenA}.ouDescendant( 2, 3, 4 ).ouAncestor( 2 ).sum()" ) );

        // ouLevel

        assertEquals( "0.25 A306-", eval( "#{dataElemenA}.ouLevel( 1 ).sum()" ) );
        assertEquals( "222.75 B306- C306- D306-", eval( "#{dataElemenA}.ouLevel( 2 ).sum()" ) );
        assertEquals( "22222 E306- F306- G306- H306- I306-", eval( "#{dataElemenA}.ouLevel( 3 ).sum()" ) );
        assertEquals( "22444.75 B306- C306- D306- E306- F306- G306- H306- I306-", eval( "#{dataElemenA}.ouLevel( 2, 3 ).sum()" ) );

        assertEquals( "53 G306- J306- K306-", eval( "#{dataElemenA}.ouLevel( 3, 4 ).ouDescendant( 0, 1, 2 ).sum()" ) );
        assertEquals( "75 G306- L306-", eval( "#{dataElemenA}.ouLevel( 3, 5 ).ouDescendant( 0, 1, 2 ).sum()" ) );

        // ouPeer

        assertEquals( "20 G306-", eval( "#{dataElemenA}.ouPeer( 0 ).sum()" ) );
        assertEquals( "2220 F306- G306- H306-", eval( "#{dataElemenA}.ouPeer( 1 ).sum()" ) );
        assertEquals( "22222 E306- F306- G306- H306- I306-", eval( "#{dataElemenA}.ouPeer( 2 ).sum()" ) );
        assertEquals( "22222 E306- F306- G306- H306- I306-", eval( "#{dataElemenA}.ouPeer( 3 ).sum()" ) );

        // ouGroup

        assertEquals( "244.5 B306- C306- E306- F306- G306-", eval( "#{dataElemenA}.ouGroup( \"OrgUnitGrpA\" ).sum()" ) );
        assertEquals( "244.5 B306- C306- E306- F306- G306-", eval( "#{dataElemenA}.ouGroup( \"OrganisationUnitGroupCodeA\" ).sum()" ) );
        assertEquals( "244.5 B306- C306- E306- F306- G306-", eval( "#{dataElemenA}.ouGroup( \"OrganisationUnitGroupA\" ).sum()" ) );
        assertEquals( "2220 F306- G306- H306-", eval( "#{dataElemenA}.ouGroup( \"OrgUnitGrpB\" ).sum()" ) );
        assertEquals( "22240.5 C306- D306- G306- H306- I306-", eval( "#{dataElemenA}.ouGroup( \"OrgUnitGrpC\" ).sum()" ) );

        assertEquals( "2244.5 B306- C306- E306- F306- G306- H306-", eval( "#{dataElemenA}.ouGroup( \"OrgUnitGrpA\", \"OrgUnitGrpB\" ).sum()" ) );
        assertEquals( "22444.75 B306- C306- D306- E306- F306- G306- H306- I306-", eval( "#{dataElemenA}.ouGroup( \"OrgUnitGrpA\", \"OrgUnitGrpB\", \"OrgUnitGrpC\" ).sum()" ) );

        assertEquals( "220 F306- G306-", eval( "#{dataElemenA}.ouGroup( \"OrgUnitGrpA\" ).ouGroup( \"OrgUnitGrpB\" ).sum()" ) );
        assertEquals( "2040.25 C306- G306- H306-", eval( "#{dataElemenA}.ouGroup( \"OrgUnitGrpA\", \"OrgUnitGrpB\").ouGroup( \"OrgUnitGrpC\" ).sum()" ) );

        // ouDataSet

        assertEquals( "22002 E306- H306- I306-", eval( "#{dataElemenA}.ouDataSet( \"DataSetUidA\" ).sum()" ) );
        assertEquals( "22002 E306- H306- I306-", eval( "#{dataElemenA}.ouDataSet( \"DataSetCodeA\" ).sum()" ) );
        assertEquals( "22002 E306- H306- I306-", eval( "#{dataElemenA}.ouDataSet( \"DataSetA\" ).sum()" ) );
        assertEquals( "20220 F306- G306- I306-", eval( "#{dataElemenA}.ouDataSet( \"DataSetUidB\" ).sum()" ) );

        assertEquals( "22222 E306- F306- G306- H306- I306-", eval( "#{dataElemenA}.ouDataSet( \"DataSetUidA\", \"DataSetUidB\" ).sum()" ) );
        assertEquals( "20000 I306-", eval( "#{dataElemenA}.ouDataSet( \"DataSetUidA\").ouDataSet( \"DataSetUidB\" ).sum()" ) );
    }

    @Test
    public void testAggregations()
    {
        // Period letters, and values for (dataElementA, orgUnitG):
        //
        // Note that for testing purposes, the values supplied depend
        // on the aggregation type override for the data element.
        //
        //              Month
        //               Jan    Feb    Mar    Apr    May    Jun    Jul
        // Year  2003     G      H      I      J      K      L      M
        // - (SUM)      1,000   400    200    100    40     20     10
        // -MAX         1,001   401    201    101    41     21     11
        // -MIN         1,002   402    202    102    42     22     12
        // -AVERAGE     1,003   403    203    103    43     23     13
        // -STDDEV      1,004   404    204    104    44     24     14
        // -VARIANCE    1,005   405    205    105    45     25     15
        // -LAST        1,006   406    206    106    46     26     16

        assertEquals( "1770 G301- G302- G303- G304- G305- G306- G307-", eval( "#{dataElemenA}.period( -5, 1 ).sum()" ) );

        assertEquals( "1001 G301-MAX G302-MAX G303-MAX G304-MAX G305-MAX G306-MAX G307-MAX", eval( "#{dataElemenA}.period( -5, 1 ).max()" ) );

        assertEquals( "12 G301-MIN G302-MIN G303-MIN G304-MIN G305-MIN G306-MIN G307-MIN", eval( "#{dataElemenA}.period( -5, 1 ).min()" ) );

        assertEquals( "45.5 G304-AVERAGE G305-AVERAGE G306-AVERAGE G307-AVERAGE", eval( "#{dataElemenA}.period( -2, 1 ).average()" ) );

        assertEquals( "15.275252316519467 G305-STDDEV G306-STDDEV G307-STDDEV", eval( "#{dataElemenA}.period( -1, 1 ).stddev()" ) );

        assertEquals( "200 G305-VARIANCE G306-VARIANCE", eval( "#{dataElemenA}.period( -1, 0 ).variance()" ) );

        assertEquals( "100 G301- G302- G303- G304- G305- G306- G307-", eval( "#{dataElemenA}.period( -5, 1 ).median()" ) );

        assertEquals( "70 G302- G303- G304- G305- G306- G307-", eval( "#{dataElemenA}.period( -4, 1 ).median()" ) );

        assertEquals( "1000 G301- G302- G303- G304- G305- G306- G307-", eval( "#{dataElemenA}.period( -5, 1 ).first()" ) );

        assertEquals( "16 G301-LAST G302-LAST G303-LAST G304-LAST G305-LAST G306-LAST G307-LAST", eval( "#{dataElemenA}.period( -5, 1 ).last()" ) );

        assertEquals( "20 G301- G302- G303- G304- G305- G306- G307-", eval( "#{dataElemenA}.period( -5, 1 ).percentile( 25 )" ) );

        assertEquals( "5 G301- G302- G303- G304- G305- G306- G307-", eval( "#{dataElemenA}.period( -5, 1 ).rankHigh( 200 )" ) );

        assertEquals( "3 G301- G302- G303- G304- G305- G306- G307-", eval( "#{dataElemenA}.period( -5, 1 ).rankLow( 200 )" ) );

        assertEquals( "71 G301- G302- G303- G304- G305- G306- G307-", eval( "#{dataElemenA}.period( -5, 1 ).rankPercentile( 200 )" ) );
    }
}
