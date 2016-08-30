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

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.indicator.IndicatorService;
import org.hisp.dhis.indicator.IndicatorType;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author Lars Helge Overland
 */
public class I18nServiceTest
    extends DhisSpringTest
{
    @Autowired
    private I18nService i18nService;

    @Autowired
    private IndicatorService indicatorService;

    private IndicatorType itA;

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

        i18nService.updateTranslation( className, locale, translationsA, dataElementA.getUid() );

        Map<String, String> actual = i18nService.getTranslations( className, locale, idA );

        assertNotNull( actual );
        assertEquals( 3, actual.size() );
        assertTrue( actual.keySet().contains( "name" ) );
        assertTrue( actual.values().contains( "frenchNameA" ) );
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