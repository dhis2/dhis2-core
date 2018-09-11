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

import com.google.common.collect.ImmutableMap;
import org.hisp.dhis.DhisTest;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.MapMap;
import org.hisp.dhis.common.MapMapMap;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.expression.Expression;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

import static java.util.AbstractMap.SimpleEntry;
import static org.junit.Assert.*;

/**
 * @author Jim Grace
 */
public class ParsingServiceTest
    extends DhisTest
{
    @Autowired
    private ParsingService parsingService;

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

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

    private MapMapMap<OrganisationUnit, Period, DimensionalItemObject, Double> valueMap;

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

        dataElementService.addDataElement( dataElementA );
        dataElementService.addDataElement( dataElementB );

        //
        // Organisation unit hierarchy:
        //
        // Level 1        A
        //              / | \
        // Level 2     B  C  D
        //            /  /|\  \
        // Level 3   E  F G H  I
        //

        orgUnitA = createOrganisationUnit( 'A' );
        orgUnitB = createOrganisationUnit( 'B', orgUnitA );
        orgUnitC = createOrganisationUnit( 'C', orgUnitA );
        orgUnitD = createOrganisationUnit( 'D', orgUnitA );
        orgUnitE = createOrganisationUnit( 'E', orgUnitB );
        orgUnitF = createOrganisationUnit( 'F', orgUnitC );
        orgUnitG = createOrganisationUnit( 'G', orgUnitC );
        orgUnitH = createOrganisationUnit( 'H', orgUnitC );
        orgUnitI = createOrganisationUnit( 'I', orgUnitD );

        orgUnitA.setUid( "OrgUnitUidA" );
        orgUnitB.setUid( "OrgUnitUidB" );
        orgUnitC.setUid( "OrgUnitUidC" );
        orgUnitD.setUid( "OrgUnitUidD" );
        orgUnitE.setUid( "OrgUnitUidE" );
        orgUnitF.setUid( "OrgUnitUidF" );
        orgUnitG.setUid( "OrgUnitUidG" );
        orgUnitH.setUid( "OrgUnitUidH" );
        orgUnitI.setUid( "OrgUnitUidI" );

        organisationUnitService.addOrganisationUnit( orgUnitA );
        organisationUnitService.addOrganisationUnit( orgUnitB );
        organisationUnitService.addOrganisationUnit( orgUnitC );
        organisationUnitService.addOrganisationUnit( orgUnitD );
        organisationUnitService.addOrganisationUnit( orgUnitE );
        organisationUnitService.addOrganisationUnit( orgUnitF );
        organisationUnitService.addOrganisationUnit( orgUnitG );
        organisationUnitService.addOrganisationUnit( orgUnitH );
        organisationUnitService.addOrganisationUnit( orgUnitI );

        //
        // Periods:
        //             Month
        //              Jan   Feb   Mar   Apr   May   Jun   Jul
        //  Year 2001                            A     B     C
        //       2002                            D     E     F
        //       2003    G     H     I     J     K     L     M
        //       2004                            N     O     P
        //

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

        //
        // Data Element A:
        //              Month
        //               Jan    Feb    Mar    Apr    May    Jun    Jul
        //  Year 2001                             100,000 40,000 20,000
        //       2002                              10,000  4,000  2,000
        //       2003   1,000   400    200    100      40     20     10
        //       2004                                   4      2      1
        //


        valueMap = MapMapMap.ofEntries(
            entry( orgUnitG, MapMap.ofEntries(
                entry( periodA, ImmutableMap.of( dataElementA, 100000.0 ) ),
                entry( periodB, ImmutableMap.of( dataElementA, 40000.0 ) ),
                entry( periodC, ImmutableMap.of( dataElementA, 20000.0 ) ),
                entry( periodD, ImmutableMap.of( dataElementA, 10000.0 ) ),
                entry( periodE, ImmutableMap.of( dataElementA, 4000.0 ) ),
                entry( periodF, ImmutableMap.of( dataElementA, 2000.0 ) ),
                entry( periodG, ImmutableMap.of( dataElementA, 1000.0 ) ),
                entry( periodH, ImmutableMap.of( dataElementA, 400.0 ) ),
                entry( periodI, ImmutableMap.of( dataElementA, 200.0 ) ),
                entry( periodJ, ImmutableMap.of( dataElementA, 100.0 ) ),
                entry( periodK, ImmutableMap.of( dataElementA, 40.0 ) ),
                entry( periodL, ImmutableMap.of( dataElementA, 20.0 ) ),
                entry( periodM, ImmutableMap.of( dataElementA, 10.0 ) ),
                entry( periodN, ImmutableMap.of( dataElementA, 4.0 ) ),
                entry( periodO, ImmutableMap.of( dataElementA, 2.0 ) ),
                entry( periodP, ImmutableMap.of( dataElementA, 1.0 ) )
                ) )
        );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private <K, V> AbstractMap.SimpleEntry<K, V> entry(K k, V v)
    {
        return new SimpleEntry<K, V>( k, v );
    }

    Object eval( String expr )
    {
        Expression expression = new Expression( expr, expr );

        return parsingService.getExpressionValue( expression, orgUnitG, periodL,
            valueMap, CONSTANT_MAP, ORG_UNIT_COUNT_MAP, DAYS );
    }

    // -------------------------------------------------------------------------
    // ParsingService
    // -------------------------------------------------------------------------

    @Test
    public void testGetExpressionValue()
    {
        // Numeric constants

        assertEquals( 2.0, eval("2" ) );
        assertEquals( 2.0, eval("2." ) );
        assertEquals( 2.0, eval("2.0" ) );
        assertEquals( 2.1, eval("2.1" ) );
        assertEquals( 0.2, eval("0.2" ) );
        assertEquals( 0.2, eval(".2" ) );
        assertEquals( 2.0, eval("2E0" ) );
        assertEquals( 2.0, eval("2e0" ) );
        assertEquals( 2.0, eval("2.E0" ) );
        assertEquals( 2.0, eval("2.0E0" ) );
        assertEquals( 2.1, eval("2.1E0" ) );
        assertEquals( 2.1, eval("2.1E+0" ) );
        assertEquals( 2.1, eval("2.1E-0" ) );
        assertEquals( 0.21, eval("2.1E-1" ) );
        assertEquals( 0.021, eval("2.1E-2" ) );
        assertEquals( 20.0, eval("2E1" ) );
        assertEquals( 20.0, eval("2E+1" ) );
        assertEquals( 20.0, eval("2E01" ) );
        assertEquals( 200.0, eval("2E2" ) );
        assertEquals( 2.0, eval("+2" ) );
        assertEquals( -2.0, eval("-2" ) );

        // Numeric comparison

        assertEquals( 1.0, eval("if(1 < 2, 1, 0)" ) );
        assertEquals( 0.0, eval("if(1 < 1, 1, 0)" ) );
        assertEquals( 0.0, eval("if(2 < 1, 1, 0)" ) );

        assertEquals( 0.0, eval("if(1 > 2, 1, 0)" ) );
        assertEquals( 0.0, eval("if(1 > 1, 1, 0)" ) );
        assertEquals( 1.0, eval("if(2 > 1, 1, 0)" ) );

        assertEquals( 1.0, eval("if(1 <= 2, 1, 0)" ) );
        assertEquals( 1.0, eval("if(1 <= 1, 1, 0)" ) );
        assertEquals( 0.0, eval("if(2 <= 1, 1, 0)" ) );

        assertEquals( 0.0, eval("if(1 >= 2, 1, 0)" ) );
        assertEquals( 1.0, eval("if(1 >= 1, 1, 0)" ) );
        assertEquals( 1.0, eval("if(2 >= 1, 1, 0)" ) );

        assertEquals( 1.0, eval("if(1 == 1, 1, 0)" ) );
        assertEquals( 0.0, eval("if(1 == 2, 1, 0)" ) );

        assertEquals( 0.0, eval("if(1 != 1, 1, 0)" ) );
        assertEquals( 1.0, eval("if(1 != 2, 1, 0)" ) );

        // Arithmetic

        assertEquals( 9.0, eval("2 + 3 + 4" ) );
        assertEquals( 24.0, eval("2 * 3 * 4" ) );
        assertEquals( 2.0, eval("12 / 3 / 2" ) );
        assertEquals( 8.0, eval("12 / ( 3 / 2 )" ) );
        assertEquals( 5.0, eval("11 % 6" ) );
        assertEquals( 2.0, eval("11 % 6 % 3" ) );
        assertEquals( 512.0, eval("2 ^ 3 ^ 2" ) );
        assertEquals( 64.0, eval("( 2 ^ 3 ) ^ 2" ) );

        // String processing

        assertEquals( 1.0, eval("if( \"a\" < \"b\", 1, 0)" ) );
        assertEquals( 0.0, eval("if( \"a\" < \"a\", 1, 0)" ) );
        assertEquals( 0.0, eval("if( \"b\" < \"a\", 1, 0)" ) );

        assertEquals( 1.0, eval("if( \"a\" == \"a\", 1, 0)" ) );
        assertEquals( 0.0, eval("if( \"a\" == \"b\", 1, 0)" ) );

        assertEquals( 0.0, eval("if( \"a\" != \"a\", 1, 0)" ) );
        assertEquals( 1.0, eval("if( \"a\" != \"b\", 1, 0)" ) );

        assertEquals( 1.0, eval("if( \"abc123\" == \"abc\" + \"123\", 1, 0)" ) );

        // Boolean processing

        assertEquals( 1.0, eval("if( true, 1, 0)" ) );
        assertEquals( 0.0, eval("if( false, 1, 0)" ) );

        assertEquals( 0.0, eval("if( ! true, 1, 0)" ) );
        assertEquals( 1.0, eval("if( ! false, 1, 0)" ) );

        assertEquals( 0.0, eval("if( true < false, 1, 0)" ) );
        assertEquals( 1.0, eval("if( true > false, 1, 0)" ) );

        assertEquals( 1.0, eval("if( true == true, 1, 0)" ) );
        assertEquals( 0.0, eval("if( true == false, 1, 0)" ) );

        assertEquals( 0.0, eval("if( true != true, 1, 0)" ) );
        assertEquals( 1.0, eval("if( true != false, 1, 0)" ) );

        assertEquals( 1.0, eval("if( true && true, 1, 0)" ) );
        assertEquals( 0.0, eval("if( true && false, 1, 0)" ) );
        assertEquals( 0.0, eval("if( false && true, 1, 0)" ) );
        assertEquals( 0.0, eval("if( false && false, 1, 0)" ) );

        assertEquals( 1.0, eval("if( true || true, 1, 0)" ) );
        assertEquals( 1.0, eval("if( true || false, 1, 0)" ) );
        assertEquals( 1.0, eval("if( false || true, 1, 0)" ) );
        assertEquals( 0.0, eval("if( false || false, 1, 0)" ) );

        assertEquals( 1.0, eval("if( true || false && false, 1, 0)" ) );
        assertEquals( 0.0, eval("if( ( true || false ) && false, 1, 0)" ) );

        // Periods

        assertEquals( 10.0, eval( "#{dataElemenA}.period( 1 )") );
        assertEquals( 20.0, eval( "#{dataElemenA}.period( 0 )") );
        assertEquals( 40.0, eval( "#{dataElemenA}.period( -1 )") );
        assertEquals( 4000.0, eval( "#{dataElemenA}.period( -12 )") );

        assertEquals( 10.0, eval( "#{dataElemenA}.period( 1, 1 ).sum()") );
        assertEquals( 30.0, eval( "#{dataElemenA}.period( 0, 1 ).sum()") );
        assertEquals( 70.0, eval( "#{dataElemenA}.period( -1, 1 ).sum()") );
        assertEquals( 7770.0, eval( "#{dataElemenA}.period( -12, 1 ).sum()") );

        assertEquals( 2000.0, eval( "#{dataElemenA}.period( 1, 1, -1 ).sum()") );
        assertEquals( 6000.0, eval( "#{dataElemenA}.period( 0, 1, -1 ).sum()") );
        assertEquals( 160000.0, eval( "#{dataElemenA}.period( -1, 1, -2 ).sum()") );
        assertEquals( 76000.0, eval( "#{dataElemenA}.period( -12, 1, -1 ).sum()") );

        assertEquals( 76000.0, eval( "#{dataElemenA}.period( -12, 1, -1, -1 ).sum()") );
        assertEquals( 16077.0, eval( "#{dataElemenA}.period( -1, 1, -1, 1 ).sum()") );
        assertEquals( 44022.0, eval( "#{dataElemenA}.period( 0, 0, -3, 3 ).sum()") );

        assertEquals( 76020.0, eval( "#{dataElemenA}.period( -12, 1, -1, -1, 0 ).sum()") );
        assertEquals( 76010.0, eval( "#{dataElemenA}.period( -12, 1, -1, -1, 1 ).sum()") );
        assertEquals( 10040.0, eval( "#{dataElemenA}.period( -1, -1, -1, -1, -1 ).sum()") );

        assertEquals( 10040.0, eval( "#{dataElemenA}.period( -1, -1, -1, -1, -1, -1 ).sum()") );

    }
}
