package org.hisp.dhis.i18n;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorService;
import org.hisp.dhis.indicator.IndicatorType;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.PeriodType;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Lars Helge Overland
 */
public class I18nServiceTest
    extends DhisSpringTest
{
    @Autowired
    private I18nService i18nService;
    
    @Autowired
    private DataElementService dataElementService;
    
    @Autowired
    private IndicatorService indicatorService;
    
    @Autowired
    private DataSetService dataSetService;
    
    private IndicatorType itA;
    
    private PeriodType ptA = new MonthlyPeriodType();

    @Override
    public void setUpTest()
    {
        itA = createIndicatorType( 'A' );
        indicatorService.addIndicatorType( itA );
    }
    
    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------
    
    @Test
    public void testUpdateTranslation()
    {
        Locale locale = Locale.FRANCE;
        String className = DataElement.class.getSimpleName();
        
        DataElement dataElementA = createDataElement( 'A' );
        String idA = dataElementA.getUid();
        
        Map<String, String> translationsA = new HashMap<>();
        translationsA.put( "name", "frenchNameA" );
        translationsA.put( "shortName", "frenchShortNameA" );
        translationsA.put( "description", "frenchDescriptionA" );        
        
        i18nService.updateTranslation( className, locale, translationsA, dataElementA.getUid());
        
        Map<String, String> actual = i18nService.getTranslations( className, locale, idA );
        
        assertNotNull( actual );
        assertEquals( 3, actual.size() );
        assertTrue( actual.keySet().contains( "name" ) );
        assertTrue( actual.values().contains( "frenchNameA" ) );
    }

    @Test
    public void testInternationaliseObject()
    {
        Locale locale = Locale.FRANCE;
        String className = DataElement.class.getSimpleName();
        
        DataElement dataElementA = createDataElement( 'A' );
        dataElementService.addDataElement( dataElementA );
        
        Map<String, String> translationsA = new HashMap<>();
        translationsA.put( "name", "frenchNameA" );
        translationsA.put( "shortName", "frenchShortNameA" );
        translationsA.put( "description", "frenchDescriptionA" );        
        
        i18nService.updateTranslation( className, locale, translationsA,dataElementA.getUid() );

        assertEquals( "DataElementA", dataElementA.getDisplayName() );
        assertEquals( "DataElementShortA", dataElementA.getDisplayShortName() );
        assertEquals( "DataElementDescriptionA", dataElementA.getDisplayDescription() );
        
        i18nService.internationalise( dataElementA, locale );
        
        assertEquals( "frenchNameA", dataElementA.getDisplayName() );
        assertEquals( "frenchShortNameA", dataElementA.getDisplayShortName() );
        assertEquals( "frenchDescriptionA", dataElementA.getDisplayDescription() );
    }    

    @Test
    public void testInternationaliseCollection()
    {
        Locale locale = Locale.FRANCE;
        String className = DataElement.class.getSimpleName();
        
        DataElement dataElementA = createDataElement( 'A' );
        dataElementService.addDataElement( dataElementA );

        DataElement dataElementB = createDataElement( 'B' );
        dataElementService.addDataElement( dataElementB );

        DataElement dataElementC = createDataElement( 'C' );
        dataElementService.addDataElement( dataElementC );
        
        List<DataElement> elements = new ArrayList<>();
        elements.add( dataElementA );
        elements.add( dataElementB );
        elements.add( dataElementC );        
        
        Map<String, String> translationsA = new HashMap<>();
        translationsA.put( "name", "frenchNameA" );
        translationsA.put( "shortName", "frenchShortNameA" );
        translationsA.put( "description", "frenchDescriptionA" );
        
        Map<String, String> translationsB = new HashMap<>();
        translationsB.put( "name", "frenchNameB" );
        translationsB.put( "shortName", "frenchShortNameB" );
        translationsB.put( "description", "frenchDescriptionB" );
        
        Map<String, String> translationsC = new HashMap<>();
        translationsC.put( "name", "frenchNameC" );
        translationsC.put( "shortName", "frenchShortNameC" );
        translationsC.put( "description", "frenchDescriptionC" );        

        i18nService.updateTranslation( className, locale, translationsA, dataElementA.getUid() );
        i18nService.updateTranslation( className, locale, translationsB, dataElementB.getUid() );
        i18nService.updateTranslation( className, locale, translationsC, dataElementC.getUid());
        
        i18nService.internationalise( elements, locale );
        
        Iterator<DataElement> elementIter = elements.iterator();
        
        assertEquals( "frenchNameA", elementIter.next().getDisplayName() );
        assertEquals( "frenchNameB", elementIter.next().getDisplayName() );
        assertEquals( "frenchNameC", elementIter.next().getDisplayName() );
    }

    @Test
    public void testInternationaliseCollectionMultiObjectTypes()
    {
        Locale locale = Locale.FRANCE;
        
        DataElement dataElementA = createDataElement( 'A' );
        dataElementService.addDataElement( dataElementA );

        DataElement dataElementB = createDataElement( 'B' );
        dataElementService.addDataElement( dataElementB );

        Indicator indicatorA = createIndicator( 'A', itA );
        indicatorService.addIndicator( indicatorA );

        Indicator indicatorB = createIndicator( 'B', itA );
        indicatorService.addIndicator( indicatorB );
        
        DataSet dataSetA = createDataSet( 'A', ptA );
        dataSetService.addDataSet( dataSetA );

        DataSet dataSetB = createDataSet( 'B', ptA );
        dataSetService.addDataSet( dataSetB );
                
        List<? extends IdentifiableObject> elements = Arrays.asList( dataElementA, dataElementB, indicatorA, indicatorB, dataSetA, dataSetB );
        
        Map<String, String> translationsA = new HashMap<>();
        translationsA.put( "name", "frenchNameDeA" );
        translationsA.put( "shortName", "frenchShortNameDeA" );
        
        Map<String, String> translationsB = new HashMap<>();
        translationsB.put( "name", "frenchNameDeB" );
        translationsB.put( "shortName", "frenchShortNameDeB" );
        
        Map<String, String> translationsC = new HashMap<>();
        translationsC.put( "name", "frenchNameInA" );
        translationsC.put( "shortName", "frenchShortNameInA" );  

        Map<String, String> translationsD = new HashMap<>();
        translationsD.put( "name", "frenchNameInB" );
        translationsD.put( "shortName", "frenchShortNameInB" );  

        Map<String, String> translationsE = new HashMap<>();
        translationsE.put( "name", "frenchNameDsA" );
        translationsE.put( "shortName", "frenchShortNameDsA" );  

        Map<String, String> translationsF = new HashMap<>();
        translationsF.put( "name", "frenchNameDsB" );
        translationsF.put( "shortName", "frenchShortNameDsB" );  

        i18nService.updateTranslation( DataElement.class.getSimpleName(), locale, translationsA, dataElementA.getUid() );
        i18nService.updateTranslation( DataElement.class.getSimpleName(), locale, translationsB, dataElementB.getUid() );
        i18nService.updateTranslation( Indicator.class.getSimpleName(), locale, translationsC, indicatorA.getUid() );
        i18nService.updateTranslation( Indicator.class.getSimpleName(), locale, translationsD, indicatorB.getUid() );
        i18nService.updateTranslation( DataSet.class.getSimpleName(), locale, translationsE, dataSetA.getUid() );
        i18nService.updateTranslation( DataSet.class.getSimpleName(), locale, translationsF, dataSetB.getUid() );
        
        i18nService.internationalise( elements, locale );
        
        Iterator<? extends IdentifiableObject> iter = elements.iterator();
        
        assertEquals( "frenchNameDeA", iter.next().getDisplayName() );
        assertEquals( "frenchNameDeB", iter.next().getDisplayName() );
        assertEquals( "frenchNameInA", iter.next().getDisplayName() );
        assertEquals( "frenchNameInB", iter.next().getDisplayName() );
        assertEquals( "frenchNameDsA", iter.next().getDisplayName() );
        assertEquals( "frenchNameDsB", iter.next().getDisplayName() );
    }
    
    @Test
    public void testGetObjectPropertyValues()
    {
        DataElement dataElementA = createDataElement( 'A' );
        
        Map<String, String> values = i18nService.getObjectPropertyValues( dataElementA );
        
        assertNotNull( values );
        assertEquals( 4, values.size() );
        assertTrue( values.keySet().contains( "name" ) );
        assertTrue( values.keySet().contains( "shortName" ) );
        assertTrue( values.keySet().contains( "description" ) );
        assertTrue( values.values().contains( "DataElementA" ) );
        assertTrue( values.values().contains( "DataElementShortA" ) );
        assertTrue( values.values().contains( "DataElementDescriptionA" ) );
    }
}