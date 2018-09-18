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
import org.apache.commons.lang.builder.CompareToBuilder;
import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.ListMap;
import org.hisp.dhis.common.ListMapMap;
import org.hisp.dhis.common.MapMap;
import org.hisp.dhis.common.MapMapMap;
import org.hisp.dhis.common.SetMap;
import org.hisp.dhis.common.SetMapMap;
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

import java.util.AbstractMap;
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

    private DimItem gaa;
    private DimItem gba;
    private DimItem gca;
    private DimItem gda;
    private DimItem gea;
    private DimItem gfa;
    private DimItem gga;
    private DimItem gha;
    private DimItem gia;
    private DimItem gja;
    private DimItem gka;
    private DimItem gla;
    private DimItem gma;
    private DimItem gna;
    private DimItem goa;
    private DimItem gpa;

    private DimItem g208a;
    private DimItem g209a;
    private DimItem g210a;
    private DimItem g211a;
    private DimItem g212a;

    private DimItem ala;
    private DimItem bla;
    private DimItem cla;
    private DimItem dla;
    private DimItem ela;
    private DimItem fla;
    private DimItem hla;
    private DimItem ila;
    private DimItem jla;
    private DimItem kla;
    private DimItem lla;



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

    private class DimItem implements Comparable<DimItem>
    {
        private OrganisationUnit orgUnit;
        private Period period;
        private DimensionalItemObject itemObject;

        DimItem( OrganisationUnit orgUnit, Period period, DimensionalItemObject itemObject )
        {
            this.orgUnit = orgUnit;
            this.period = period;
            this.itemObject = itemObject;
        }

        @Override
        public int compareTo( DimItem di )
        {
            return new CompareToBuilder()
                .append( this.orgUnit.getUid(), di.orgUnit.getUid() )
                .append( this.period.getIsoDate(), di.period.getIsoDate() )
                .append( this.itemObject.getClass().getName(), di.itemObject.getClass().getName() )
                .append( this.itemObject.getDimensionItem(), di.itemObject.getDimensionItem() )
                .toComparison();
        }
    }

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
        //               /\
        // Level 4      J  K
        //              |
        // Level 5      L
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
        orgUnitI.setUid( "OrgUnitUidJ" );
        orgUnitI.setUid( "OrgUnitUidK" );
        orgUnitI.setUid( "OrgUnitUidL" );

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
        dataSetA.addOrganisationUnit( orgUnitG );
        dataSetA.addOrganisationUnit( orgUnitI );

        dataSetB.addOrganisationUnit( orgUnitF );
        dataSetB.addOrganisationUnit( orgUnitG );
        dataSetB.addOrganisationUnit( orgUnitH );

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

        gaa = dimItem( orgUnitG, periodA, dataElementA );
        gba = dimItem( orgUnitG, periodB, dataElementA );
        gca = dimItem( orgUnitG, periodC, dataElementA );
        gda = dimItem( orgUnitG, periodD, dataElementA );
        gea = dimItem( orgUnitG, periodE, dataElementA );
        gfa = dimItem( orgUnitG, periodF, dataElementA );
        gga = dimItem( orgUnitG, periodG, dataElementA );
        gha = dimItem( orgUnitG, periodH, dataElementA );
        gia = dimItem( orgUnitG, periodI, dataElementA );
        gja = dimItem( orgUnitG, periodJ, dataElementA );
        gka = dimItem( orgUnitG, periodK, dataElementA );
        gla = dimItem( orgUnitG, periodL, dataElementA );
        gma = dimItem( orgUnitG, periodM, dataElementA );
        gna = dimItem( orgUnitG, periodN, dataElementA );
        goa = dimItem( orgUnitG, periodO, dataElementA );
        gpa = dimItem( orgUnitG, periodP, dataElementA );

        g208a = dimItem( orgUnitG, createPeriod( "200208" ), dataElementA );
        g209a = dimItem( orgUnitG, createPeriod( "200209" ), dataElementA );
        g210a = dimItem( orgUnitG, createPeriod( "200210" ), dataElementA );
        g211a = dimItem( orgUnitG, createPeriod( "200211" ), dataElementA );
        g212a = dimItem( orgUnitG, createPeriod( "200212" ), dataElementA );


        ala = dimItem( orgUnitA, periodL, dataElementA );
        bla = dimItem( orgUnitB, periodL, dataElementA );
        cla = dimItem( orgUnitC, periodL, dataElementA );
        dla = dimItem( orgUnitD, periodL, dataElementA );
        ela = dimItem( orgUnitE, periodL, dataElementA );
        fla = dimItem( orgUnitF, periodL, dataElementA );
        hla = dimItem( orgUnitH, periodL, dataElementA );
        ila = dimItem( orgUnitI, periodL, dataElementA );
        jla = dimItem( orgUnitJ, periodL, dataElementA );
        kla = dimItem( orgUnitK, periodL, dataElementA );
        lla = dimItem( orgUnitL, periodL, dataElementA );

        valueMap = MapMapMap.ofEntries(
            entry( orgUnitA, MapMap.ofEntries(
                entry( periodK, ImmutableMap.of( dataElementA, 0.5 ) ),
                entry( periodL, ImmutableMap.of( dataElementA, 0.25 ) ),
                entry( periodM, ImmutableMap.of( dataElementA, 0.125 ) )
            ) ),
            entry( orgUnitB, MapMap.ofEntries(
                entry( periodK, ImmutableMap.of( dataElementA, 1.5 ) ),
                entry( periodL, ImmutableMap.of( dataElementA, 2.25 ) ),
                entry( periodM, ImmutableMap.of( dataElementA, 4.125 ) )
            ) ),
            entry( orgUnitC, MapMap.ofEntries(
                entry( periodK, ImmutableMap.of( dataElementA, 10.5 ) ),
                entry( periodL, ImmutableMap.of( dataElementA, 20.25 ) ),
                entry( periodM, ImmutableMap.of( dataElementA, 40.125 ) )
            ) ),
            entry( orgUnitD, MapMap.ofEntries(
                entry( periodK, ImmutableMap.of( dataElementA, 100.5 ) ),
                entry( periodL, ImmutableMap.of( dataElementA, 200.25 ) ),
                entry( periodM, ImmutableMap.of( dataElementA, 400.125 ) )
            ) ),
            entry( orgUnitE, MapMap.ofEntries(
                entry( periodK, ImmutableMap.of( dataElementA, 1.0 ) ),
                entry( periodL, ImmutableMap.of( dataElementA, 2.0 ) ),
                entry( periodM, ImmutableMap.of( dataElementA, 4.0 ) )
            ) ),
            entry( orgUnitF, MapMap.ofEntries(
                entry( periodK, ImmutableMap.of( dataElementA, 100.0 ) ),
                entry( periodL, ImmutableMap.of( dataElementA, 200.0 ) ),
                entry( periodM, ImmutableMap.of( dataElementA, 400.0 ) )
            ) ),
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
                entry( periodK, ImmutableMap.of( dataElementA, 40.0, dataElementB, 1.5 ) ),
                entry( periodL, ImmutableMap.of( dataElementA, 20.0, dataElementB, 2.25 ) ),
                entry( periodM, ImmutableMap.of( dataElementA, 10.0, dataElementB, 4.125 ) ),
                entry( periodN, ImmutableMap.of( dataElementA, 4.0 ) ),
                entry( periodO, ImmutableMap.of( dataElementA, 2.0 ) ),
                entry( periodP, ImmutableMap.of( dataElementA, 1.0 ) )
            ) ),
            entry( orgUnitH, MapMap.ofEntries(
                entry( periodK, ImmutableMap.of( dataElementA, 1000.0 ) ),
                entry( periodL, ImmutableMap.of( dataElementA, 2000.0 ) ),
                entry( periodM, ImmutableMap.of( dataElementA, 4000.0 ) )
           ) ),
            entry( orgUnitI, MapMap.ofEntries(
                entry( periodK, ImmutableMap.of( dataElementA, 10000.0 ) ),
                entry( periodL, ImmutableMap.of( dataElementA, 20000.0 ) ),
                entry( periodM, ImmutableMap.of( dataElementA, 40000.0 ) )
            ) ),
            entry( orgUnitJ, MapMap.ofEntries(
                entry( periodL, ImmutableMap.of( dataElementA, 11.0 ) )
            ) ),
            entry( orgUnitK, MapMap.ofEntries(
                entry( periodL, ImmutableMap.of( dataElementA, 22.0 ) )
            ) ),
            entry( orgUnitL, MapMap.ofEntries(
                entry( periodL, ImmutableMap.of( dataElementA, 55.0 ) )
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

    private DimItem dimItem( OrganisationUnit orgUnit, Period period, DimensionalItemObject itemObject )
    {
        return new DimItem( orgUnit, period, itemObject );
    }

    private String result( int value, DimItem... dimItems )
    {
        return result( (double)value, Arrays.asList( dimItems ) );
    }

    private String result( Double value, DimItem... dimItems )
    {
        return result( value, Arrays.asList( dimItems ) );
    }

    private String result( Double value, List<DimItem> dimItems )
    {
        Collections.sort( dimItems );

        String s = value == null ? "null" : Double.toString( value );

        for (DimItem d : dimItems )
        {
            s += " - " + d.orgUnit.getUid() + "/" + d.period.getUid() + "/" + d.itemObject.getDimensionItem();
        }

        return s;
    }

    private String eval( String expr )
    {
        Expression expression = new Expression( expr, expr );

        SetMapMap<OrganisationUnit, Period, DimensionalItemObject> items = parsingService.getItemsInExpression(
            Arrays.asList( expression ), Arrays.asList( orgUnitG ), Arrays.asList( periodL ),
            CONSTANT_MAP, ORG_UNIT_COUNT_MAP );

        List<DimItem> dimItems = getDimItems( items );

        Double value = parsingService.getExpressionValue( expression, orgUnitG, periodL,
            valueMap, CONSTANT_MAP, ORG_UNIT_COUNT_MAP, DAYS );

        return result( value, dimItems );
    }

    private List<DimItem> getDimItems( SetMapMap<OrganisationUnit, Period, DimensionalItemObject> items )
    {
        List<DimItem> dimItems = new ArrayList<>();

        for ( Map.Entry<OrganisationUnit, SetMap<Period, DimensionalItemObject>> e1 : items.entrySet() )
        {
            for ( Map.Entry<Period, Set<DimensionalItemObject>> e2: e1.getValue().entrySet() )
            {
                for ( DimensionalItemObject item : e2.getValue() )
                {
                    dimItems.add( new DimItem( e1.getKey(), e2.getKey(), item ) );
                }
            }
        }

        return dimItems;
    }

    // -------------------------------------------------------------------------
    // Test getExpressionValue
    // -------------------------------------------------------------------------

    @Test
    public void testExpressionNumeric()
    {
        // Numeric constants

        assertEquals( result( 2 ), eval( "2" ) );
        assertEquals( result( 2 ), eval( "2." ) );
        assertEquals( result( 2 ), eval( "2.0" ) );
        assertEquals( result( 2.1 ), eval( "2.1" ) );
        assertEquals( result( 0.2 ), eval( "0.2" ) );
        assertEquals( result( 0.2 ), eval( ".2" ) );
        assertEquals( result( 2 ), eval( "2E0" ) );
        assertEquals( result( 2 ), eval( "2e0" ) );
        assertEquals( result( 2 ), eval( "2.E0" ) );
        assertEquals( result( 2 ), eval( "2.0E0" ) );
        assertEquals( result( 2.1 ), eval( "2.1E0" ) );
        assertEquals( result( 2.1 ), eval( "2.1E+0" ) );
        assertEquals( result( 2.1 ), eval( "2.1E-0" ) );
        assertEquals( result( 0.21 ), eval( "2.1E-1" ) );
        assertEquals( result( 0.021 ), eval( "2.1E-2" ) );
        assertEquals( result( 20 ), eval( "2E1" ) );
        assertEquals( result( 20 ), eval( "2E+1" ) );
        assertEquals( result( 20 ), eval( "2E01" ) );
        assertEquals( result( 200 ), eval( "2E2" ) );
        assertEquals( result( 2 ), eval( "+2" ) );
        assertEquals( result( -2 ), eval( "-2" ) );

        // Numeric comparisons

        assertEquals( result( 1 ), eval( "if(1 < 2, 1, 0)" ) );
        assertEquals( result( 0 ), eval( "if(1 < 1, 1, 0)" ) );
        assertEquals( result( 0 ), eval( "if(2 < 1, 1, 0)" ) );

        assertEquals( result( 0 ), eval( "if(1 > 2, 1, 0)" ) );
        assertEquals( result( 0 ), eval( "if(1 > 1, 1, 0)" ) );
        assertEquals( result( 1 ), eval( "if(2 > 1, 1, 0)" ) );

        assertEquals( result( 1 ), eval( "if(1 <= 2, 1, 0)" ) );
        assertEquals( result( 1 ), eval( "if(1 <= 1, 1, 0)" ) );
        assertEquals( result( 0 ), eval( "if(2 <= 1, 1, 0)" ) );

        assertEquals( result( 0 ), eval( "if(1 >= 2, 1, 0)" ) );
        assertEquals( result( 1 ), eval( "if(1 >= 1, 1, 0)" ) );
        assertEquals( result( 1 ), eval( "if(2 >= 1, 1, 0)" ) );

        assertEquals( result( 1 ), eval( "if(1 == 1, 1, 0)" ) );
        assertEquals( result( 0 ), eval( "if(1 == 2, 1, 0)" ) );

        assertEquals( result( 0 ), eval( "if(1 != 1, 1, 0)" ) );
        assertEquals( result( 1 ), eval( "if(1 != 2, 1, 0)" ) );

        // Arithmetic

        assertEquals( result( 9 ), eval( "2 + 3 + 4" ) );
        assertEquals( result( 24 ), eval( "2 * 3 * 4" ) );
        assertEquals( result( 2 ), eval( "12 / 3 / 2" ) );
        assertEquals( result( 8 ), eval( "12 / ( 3 / 2 )" ) );
        assertEquals( result( 5 ), eval( "11 % 6" ) );
        assertEquals( result( 2 ), eval( "11 % 6 % 3" ) );
        assertEquals( result( 512 ), eval( "2 ^ 3 ^ 2" ) );
        assertEquals( result( 64 ), eval( "( 2 ^ 3 ) ^ 2" ) );
    }

    @Test
    public void testGetExpressionValueString()
    {
        assertEquals( result( 1 ), eval( "if( \"a\" < \"b\", 1, 0)" ) );
        assertEquals( result( 0 ), eval( "if( \"a\" < \"a\", 1, 0)" ) );
        assertEquals( result( 0 ), eval( "if( \"b\" < \"a\", 1, 0)" ) );

        assertEquals( result( 1 ), eval( "if( \"a\" == \"a\", 1, 0)" ) );
        assertEquals( result( 0 ), eval( "if( \"a\" == \"b\", 1, 0)" ) );

        assertEquals( result( 0 ), eval( "if( \"a\" != \"a\", 1, 0)" ) );
        assertEquals( result( 1 ), eval( "if( \"a\" != \"b\", 1, 0)" ) );

        assertEquals( result( 1 ), eval( "if( \"abc123\" == \"abc\" + \"123\", 1, 0)" ) );
    }

    @Test
    public void testGetExpressionValueBoolean()
    {
        assertEquals( result( 1 ), eval( "if( true, 1, 0)" ) );
        assertEquals( result( 0 ), eval( "if( false, 1, 0)" ) );

        assertEquals( result( 0 ), eval( "if( ! true, 1, 0)" ) );
        assertEquals( result( 1 ), eval( "if( ! false, 1, 0)" ) );

        assertEquals( result( 0 ), eval( "if( true < false, 1, 0)" ) );
        assertEquals( result( 1 ), eval( "if( true > false, 1, 0)" ) );

        assertEquals( result( 1 ), eval( "if( true == true, 1, 0)" ) );
        assertEquals( result( 0 ), eval( "if( true == false, 1, 0)" ) );

        assertEquals( result( 0 ), eval( "if( true != true, 1, 0)" ) );
        assertEquals( result( 1 ), eval( "if( true != false, 1, 0)" ) );

        assertEquals( result( 1 ), eval( "if( true && true, 1, 0)" ) );
        assertEquals( result( 0 ), eval( "if( true && false, 1, 0)" ) );
        assertEquals( result( 0 ), eval( "if( false && true, 1, 0)" ) );
        assertEquals( result( 0 ), eval( "if( false && false, 1, 0)" ) );

        assertEquals( result( 1 ), eval( "if( true || true, 1, 0)" ) );
        assertEquals( result( 1 ), eval( "if( true || false, 1, 0)" ) );
        assertEquals( result( 1 ), eval( "if( false || true, 1, 0)" ) );
        assertEquals( result( 0 ), eval( "if( false || false, 1, 0)" ) );

        assertEquals( result( 1 ), eval( "if( true || false && false, 1, 0)" ) );
        assertEquals( result( 0 ), eval( "if( ( true || false ) && false, 1, 0)" ) );
    }

    @Test
    public void testExpressionPeriods()
    {
        //
        // Periods:
        //              Month
        //               Jan    Feb    Mar    Apr   May    Jun    Jul
        //  Year 2001                                A      B      C
        //       2002                                D      E      F
        //       2003    G      H      I      J      K      L      M
        //       2004                                N      O      P
        //
        // Data Element A, Organisation Unit G:
        //              Month
        //               Jan    Feb    Mar    Apr    May    Jun    Jul
        //  Year 2001                             100,000 40,000 20,000
        //       2002                              10,000  4,000  2,000
        //       2003   1,000   400    200    100      40     20     10
        //       2004                                   4      2      1
        //

        assertEquals( result( 10, gma ), eval( "#{dataElemenA}.period( 1 )" ) );
        assertEquals( result( 20, gla ), eval( "#{dataElemenA}.period( 0 )" ) );
        assertEquals( result( 40, gka ), eval( "#{dataElemenA}.period( -1 )" ) );
        assertEquals( result( 4000, gea ), eval( "#{dataElemenA}.period( -12 )" ) );

        assertEquals( result( 10, gma ), eval( "#{dataElemenA}.period( 1, 1 ).sum()" ) );
        assertEquals( result( 30, gla, gma ), eval( "#{dataElemenA}.period( 0, 1 ).sum()" ) );
        assertEquals( result( 70, gka, gla, gma ), eval( "#{dataElemenA}.period( -1, 1 ).sum()" ) );
        assertEquals( result( 7770, gea, gfa, g208a, g209a, g210a, g211a, g212a, gga, gha, gia, gja, gka, gla, gma ), eval( "#{dataElemenA}.period( -12, 1 ).sum()" ) );

        assertEquals( result( 2000, gfa ), eval( "#{dataElemenA}.period( 1, 1, -1 ).sum()" ) );
        assertEquals( result( 6000, gea, gfa ), eval( "#{dataElemenA}.period( 0, 1, -1 ).sum()" ) );
        assertEquals( result( 160000, gaa, gba, gca ), eval( "#{dataElemenA}.period( -1, 1, -2 ).sum()" ) );
        assertEquals( result( 7770, gea, gfa, g208a, g209a, g210a, g211a, g212a, gga, gha, gia, gja, gka, gla, gma ), eval( "#{dataElemenA}.period( -12, 1, 0 ).sum()" ) );

        assertEquals( result( 7770, gea, gfa, g208a, g209a, g210a, g211a, g212a, gga, gha, gia, gja, gka, gla, gma ), eval( "#{dataElemenA}.period( -12, 1, 0, 0 ).sum()" ) );
        assertEquals( result( 16077, gda, gea, gfa, gka, gla, gma, gna, goa, gpa ), eval( "#{dataElemenA}.period( -1, 1, -1, 1 ).sum()" ) );
        assertEquals( result( 44022, gba, gea, gla, goa ), eval( "#{dataElemenA}.period( 0, 0, -2, 1 ).sum()" ) );

        assertEquals( result( 7780, gea, gfa, g208a, g209a, g210a, g211a, g212a, gga, gha, gia, gja, gka, gma  ), eval( "#{dataElemenA}.period( -12, 1, 0, 0, 1 ).sum()" ) );
        assertEquals( result( 10040, gda ), eval( "#{dataElemenA}.period( -1, -1, -1, -1, -1 ).sum()" ) );

        assertEquals( result( 10040, gda ), eval( "#{dataElemenA}.period( -1, -1, -1, -1, -1, -1 ).sum()" ) );
        assertEquals( result( 66666, gha, gia, gba, gca, gea, gfa, gka, gla, gna, goa ), eval( "#{dataElemenA}.period( -4, -3, 0, 0,   0, 1, -2, -1,   -1, 0, 0, 1).sum()" ) );

//        assertEquals( result( null ), eval("#{dataElemenA}.period( 2 )" ) );
//        assertEquals( result( null ), eval("#{dataElemenA}.period( 2, 4 ).sum()" ) );
//        assertEquals( result( null ), eval("#{dataElemenA}.period( -17, -15 ).sum()" ) );
//        assertEquals( result( null ), eval("#{dataElemenA}.period( -5, -2, -1 ).sum()" ) );
//        assertEquals( result( null ), eval("#{dataElemenA}.period( -5, -2, -2, -1 ).sum()" ) );
//        assertEquals( result( null ), eval("#{dataElemenA}.period( -5, -2, -2, -1, 2 ).sum()" ) );
//        assertEquals( result( null ), eval("#{dataElemenA}.period( -5, -2, -2, -1, 2, 4 ).sum()" ) );
//        assertEquals( result( null ), eval("#{dataElemenA}.period( -5, -2, -2, -1, 2, 4, 1 ).sum()" ) );
//        assertEquals( result( null ), eval("#{dataElemenA}.period( -5, -2, -2, -1, 2, 4, 1, 2 ).sum()" ) );
    }

//    @Test
    public void testGetExpressionValueOrganisationUnits()
    {
        // Data Element A, June 2003
        //
        // Organisation unit hierarchy:
        //
        // Level 1            0.25
        //                  /  |  \
        // Level 2     2.25  20.25  200.25
        //            /      / |  \     \
        // Level 3   2   200  20  2000  20,000
        //                    /\
        // Level 4          11 22
        //                   |
        // Level 5          55
        //

        // ouAncestor

        assertEquals( 20.0, eval( "#{dataElemenA}.ouAncestor( 0 )" ) );
        assertEquals( 20.25, eval( "#{dataElemenA}.ouAncestor( 1 )" ) );
        assertEquals( 0.25, eval( "#{dataElemenA}.ouAncestor( 2 )" ) );

        // ouDescendant

        assertEquals( 20.0, eval( "#{dataElemenA}.ouDescendant( 0 ).sum()" ) );
        assertEquals( 33.0, eval( "#{dataElemenA}.ouDescendant( 1 ).sum()" ) );
        assertEquals( 55.0, eval( "#{dataElemenA}.ouDescendant( 2 ).sum()" ) );
        assertEquals( 53.0, eval( "#{dataElemenA}.ouDescendant( 0, 1 ).sum()" ) );
        assertEquals( 75.0, eval( "#{dataElemenA}.ouDescendant( 0, 2 ).sum()" ) );
        assertEquals( 88.0, eval( "#{dataElemenA}.ouDescendant( 1, 2 ).sum()" ) );
        assertEquals( 108.0, eval( "#{dataElemenA}.ouDescendant( 0, 1, 2 ).sum()" ) );
        assertEquals( 108.0, eval( "#{dataElemenA}.ouDescendant( 0, 1, 2, 3 ).sum()" ) );

        assertEquals( 2220.0, eval( "#{dataElemenA}.ouDescendant( 1 ).ouAncestor( 1 ).sum()" ) );
        assertEquals( 22222.0, eval( "#{dataElemenA}.ouDescendant( 2 ).ouAncestor( 2 ).sum()" ) );
        assertEquals( 222.75, eval( "#{dataElemenA}.ouDescendant( 1 ).ouAncestor( 2 ).sum()" ) );
        assertEquals( 22255.0, eval( "#{dataElemenA}.ouDescendant( 2, 3 ).ouAncestor( 2 ).sum()" ) );
        assertEquals( 22310.0, eval( "#{dataElemenA}.ouDescendant( 2, 3, 4 ).ouAncestor( 2 ).sum()" ) );

        // ouLevel

        assertEquals( 0.25, eval( "#{dataElemenA}.ouLevel( 1 ).sum()" ) );
        assertEquals( 222.75, eval( "#{dataElemenA}.ouLevel( 2 ).sum()" ) );
        assertEquals( 22222.0, eval( "#{dataElemenA}.ouLevel( 3 ).sum()" ) );
        assertEquals( 22444.75, eval( "#{dataElemenA}.ouLevel( 2, 3 ).sum()" ) );

        assertEquals( 53.0, eval( "#{dataElemenA}.ouLevel( 3, 4 ).ouDescendant( 0, 1, 2 ).sum()" ) );
        assertEquals( 75.0, eval( "#{dataElemenA}.ouLevel( 3, 5 ).ouDescendant( 0, 1, 2 ).sum()" ) );

        // ouPeer

        assertEquals( 20.0, eval( "#{dataElemenA}.ouPeer( 0 ).sum()" ) );
        assertEquals( 2220.0, eval( "#{dataElemenA}.ouPeer( 1 ).sum()" ) );
        assertEquals( 22222.0, eval( "#{dataElemenA}.ouPeer( 2 ).sum()" ) );
        assertEquals( 22222.0, eval( "#{dataElemenA}.ouPeer( 3 ).sum()" ) );

        // ouGroup

        assertEquals( 244.5, eval( "#{dataElemenA}.ouGroup( \"OrgUnitGrpA\" ).sum()" ) );
        assertEquals( 244.5, eval( "#{dataElemenA}.ouGroup( \"OrganisationUnitGroupCodeA\" ).sum()" ) );
        assertEquals( 244.5, eval( "#{dataElemenA}.ouGroup( \"OrganisationUnitGroupA\" ).sum()" ) );

        assertEquals( 2220.0, eval( "#{dataElemenA}.ouGroup( \"OrgUnitGrpB\" ).sum()" ) );

        assertEquals( 22240.5, eval( "#{dataElemenA}.ouGroup( \"OrgUnitGrpC\" ).sum()" ) );

        assertEquals( 2244.5, eval( "#{dataElemenA}.ouGroup( \"OrgUnitGrpA\", \"OrgUnitGrpB\" ).sum()" ) );

        assertEquals( 22444.75, eval( "#{dataElemenA}.ouGroup( \"OrgUnitGrpA\", \"OrgUnitGrpB\", \"OrgUnitGrpC\" ).sum()" ) );

        assertEquals( 220.00, eval( "#{dataElemenA}.ouGroup( \"OrgUnitGrpA\" ).ouGroup( \"OrgUnitGrpB\" ).sum()" ) );

        assertEquals( 2040.25, eval( "#{dataElemenA}.ouGroup( \"OrgUnitGrpA\", \"OrgUnitGrpB\").ouGroup( \"OrgUnitGrpC\" ).sum()" ) );

        // ouDataSet

        assertEquals( 20022.0, eval( "#{dataElemenA}.ouDataSet( \"DataSetUidA\" ).sum()" ) );
        assertEquals( 20022.0, eval( "#{dataElemenA}.ouDataSet( \"DataSetCodeA\" ).sum()" ) );
        assertEquals( 20022.0, eval( "#{dataElemenA}.ouDataSet( \"DataSetA\" ).sum()" ) );

    }
}
