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
package org.hisp.dhis.indicator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.test.integration.SingleSetupIntegrationTestBase;
import org.hisp.dhis.translation.Translation;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.user.UserSettingKey;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Lars Helge Overland
 * @version $Id$
 */
class IndicatorServiceTest extends SingleSetupIntegrationTestBase
{
    @Autowired
    private IndicatorService indicatorService;

    @Autowired
    private UserService injectUserService;

    @Autowired
    private IdentifiableObjectManager identifiableObjectManager;

    private User user;

    @Override
    public void setUpTest()
    {
        this.userService = injectUserService;
        user = createUserAndInjectSecurityContext( true );
    }

    // -------------------------------------------------------------------------
    // Support methods
    // -------------------------------------------------------------------------
    private void assertEq( char uniqueCharacter, Indicator indicator )
    {
        assertEquals( "Indicator" + uniqueCharacter, indicator.getName() );
        assertEquals( "IndicatorShort" + uniqueCharacter, indicator.getShortName() );
        assertEquals( "IndicatorCode" + uniqueCharacter, indicator.getCode() );
        assertEquals( "IndicatorDescription" + uniqueCharacter, indicator.getDescription() );
    }

    // -------------------------------------------------------------------------
    // IndicatorType
    // -------------------------------------------------------------------------
    @Test
    void testAddIndicatorType()
    {
        IndicatorType typeA = new IndicatorType( "IndicatorTypeA", 100, false );
        IndicatorType typeB = new IndicatorType( "IndicatorTypeB", 1, false );
        long idA = indicatorService.addIndicatorType( typeA );
        long idB = indicatorService.addIndicatorType( typeB );
        typeA = indicatorService.getIndicatorType( idA );
        assertNotNull( typeA );
        assertEquals( idA, typeA.getId() );
        typeB = indicatorService.getIndicatorType( idB );
        assertNotNull( typeB );
        assertEquals( idB, typeB.getId() );
    }

    @Test
    void testUpdateIndicatorType()
        throws Exception
    {
        IndicatorType typeA = new IndicatorType( "IndicatorTypeA", 100, false );
        long idA = indicatorService.addIndicatorType( typeA );
        typeA = indicatorService.getIndicatorType( idA );
        assertEquals( typeA.getName(), "IndicatorTypeA" );
        typeA.setName( "IndicatorTypeB" );
        indicatorService.updateIndicatorType( typeA );
        typeA = indicatorService.getIndicatorType( idA );
        assertNotNull( typeA );
        assertEquals( typeA.getName(), "IndicatorTypeB" );
    }

    @Test
    void testGetAndDeleteIndicatorType()
    {
        IndicatorType typeA = new IndicatorType( "IndicatorTypeA", 100, false );
        IndicatorType typeB = new IndicatorType( "IndicatorTypeB", 1, false );
        long idA = indicatorService.addIndicatorType( typeA );
        long idB = indicatorService.addIndicatorType( typeB );
        assertNotNull( indicatorService.getIndicatorType( idA ) );
        assertNotNull( indicatorService.getIndicatorType( idB ) );
        indicatorService.deleteIndicatorType( typeA );
        assertNull( indicatorService.getIndicatorType( idA ) );
        assertNotNull( indicatorService.getIndicatorType( idB ) );
        indicatorService.deleteIndicatorType( typeB );
        assertNull( indicatorService.getIndicatorType( idA ) );
        assertNull( indicatorService.getIndicatorType( idB ) );
    }

    @Test
    void testGetAllIndicatorTypes()
    {
        IndicatorType typeA = new IndicatorType( "IndicatorTypeA", 100, false );
        IndicatorType typeB = new IndicatorType( "IndicatorTypeB", 1, false );
        indicatorService.addIndicatorType( typeA );
        indicatorService.addIndicatorType( typeB );
        List<IndicatorType> types = indicatorService.getAllIndicatorTypes();
        assertEquals( types.size(), 2 );
        assertTrue( types.contains( typeA ) );
        assertTrue( types.contains( typeB ) );
    }

    // -------------------------------------------------------------------------
    // IndicatorGroup
    // -------------------------------------------------------------------------
    @Test
    void testAddIndicatorGroup()
    {
        IndicatorGroup groupA = new IndicatorGroup( "IndicatorGroupA" );
        IndicatorGroup groupB = new IndicatorGroup( "IndicatorGroupB" );
        long idA = indicatorService.addIndicatorGroup( groupA );
        long idB = indicatorService.addIndicatorGroup( groupB );
        groupA = indicatorService.getIndicatorGroup( idA );
        assertNotNull( groupA );
        assertEquals( idA, groupA.getId() );
        groupB = indicatorService.getIndicatorGroup( idB );
        assertNotNull( groupB );
        assertEquals( idB, groupB.getId() );
    }

    @Test
    void testUpdateIndicatorGroup()
    {
        IndicatorGroup groupA = new IndicatorGroup( "IndicatorGroupA" );
        long idA = indicatorService.addIndicatorGroup( groupA );
        groupA = indicatorService.getIndicatorGroup( idA );
        assertEquals( groupA.getName(), "IndicatorGroupA" );
        groupA.setName( "IndicatorGroupB" );
        indicatorService.updateIndicatorGroup( groupA );
        groupA = indicatorService.getIndicatorGroup( idA );
        assertNotNull( groupA );
        assertEquals( groupA.getName(), "IndicatorGroupB" );
    }

    @Test
    void testGetAndDeleteIndicatorGroup()
    {
        IndicatorGroup groupA = new IndicatorGroup( "IndicatorGroupA" );
        IndicatorGroup groupB = new IndicatorGroup( "IndicatorGroupB" );
        long idA = indicatorService.addIndicatorGroup( groupA );
        long idB = indicatorService.addIndicatorGroup( groupB );
        assertNotNull( indicatorService.getIndicatorGroup( idA ) );
        assertNotNull( indicatorService.getIndicatorGroup( idB ) );
        indicatorService.deleteIndicatorGroup( groupA );
        assertNull( indicatorService.getIndicatorGroup( idA ) );
        assertNotNull( indicatorService.getIndicatorGroup( idB ) );
        indicatorService.deleteIndicatorGroup( groupB );
        assertNull( indicatorService.getIndicatorGroup( idA ) );
        assertNull( indicatorService.getIndicatorGroup( idB ) );
    }

    @Test
    void testGetAllIndicatorGroups()
    {
        IndicatorGroup groupA = new IndicatorGroup( "IndicatorGroupA" );
        IndicatorGroup groupB = new IndicatorGroup( "IndicatorGroupB" );
        indicatorService.addIndicatorGroup( groupA );
        indicatorService.addIndicatorGroup( groupB );
        List<IndicatorGroup> groups = indicatorService.getAllIndicatorGroups();
        assertEquals( groups.size(), 2 );
        assertTrue( groups.contains( groupA ) );
        assertTrue( groups.contains( groupB ) );
    }

    // -------------------------------------------------------------------------
    // Indicator
    // -------------------------------------------------------------------------
    @Test
    void testAddIndicator()
    {
        IndicatorType type = new IndicatorType( "IndicatorType", 100, false );
        indicatorService.addIndicatorType( type );
        Indicator indicatorA = createIndicator( 'A', type );
        Indicator indicatorB = createIndicator( 'B', type );
        long idA = indicatorService.addIndicator( indicatorA );
        long idB = indicatorService.addIndicator( indicatorB );
        indicatorA = indicatorService.getIndicator( idA );
        assertNotNull( indicatorA );
        assertEq( 'A', indicatorA );
        indicatorB = indicatorService.getIndicator( idB );
        assertNotNull( indicatorB );
        assertEq( 'B', indicatorB );
    }

    @Test
    void testUpdateIndicator()
    {
        IndicatorType type = new IndicatorType( "IndicatorType", 100, false );
        indicatorService.addIndicatorType( type );
        Indicator indicatorA = createIndicator( 'A', type );
        long idA = indicatorService.addIndicator( indicatorA );
        indicatorA = indicatorService.getIndicator( idA );
        assertEq( 'A', indicatorA );
        indicatorA.setName( "IndicatorB" );
        indicatorService.updateIndicator( indicatorA );
        indicatorA = indicatorService.getIndicator( idA );
        assertNotNull( indicatorA );
        assertEquals( indicatorA.getName(), "IndicatorB" );
    }

    @Test
    void testGetAndDeleteIndicator()
    {
        IndicatorType type = new IndicatorType( "IndicatorType", 100, false );
        indicatorService.addIndicatorType( type );
        Indicator indicatorA = createIndicator( 'A', type );
        Indicator indicatorB = createIndicator( 'B', type );
        long idA = indicatorService.addIndicator( indicatorA );
        long idB = indicatorService.addIndicator( indicatorB );
        assertNotNull( indicatorService.getIndicator( idA ) );
        assertNotNull( indicatorService.getIndicator( idB ) );
        indicatorService.deleteIndicator( indicatorA );
        assertNull( indicatorService.getIndicator( idA ) );
        assertNotNull( indicatorService.getIndicator( idB ) );
        indicatorService.deleteIndicator( indicatorB );
        assertNull( indicatorService.getIndicator( idA ) );
        assertNull( indicatorService.getIndicator( idB ) );
    }

    @Test
    void testGetAllIndicators()
    {
        IndicatorType type = new IndicatorType( "IndicatorType", 100, false );
        indicatorService.addIndicatorType( type );
        Indicator indicatorA = createIndicator( 'A', type );
        Indicator indicatorB = createIndicator( 'B', type );
        indicatorService.addIndicator( indicatorA );
        indicatorService.addIndicator( indicatorB );
        List<Indicator> indicators = indicatorService.getAllIndicators();
        assertEquals( indicators.size(), 2 );
        assertTrue( indicators.contains( indicatorA ) );
        assertTrue( indicators.contains( indicatorB ) );
    }

    @Test
    void testNumeratorTranslation()
    {
        Locale locale = Locale.FRENCH;
        CurrentUserUtil.setUserSetting( UserSettingKey.DB_LOCALE, locale );
        IndicatorType type = new IndicatorType( "IndicatorType", 100, false );
        indicatorService.addIndicatorType( type );
        Indicator indicatorA = createIndicator( 'A', type );
        indicatorA.setNumeratorDescription( "Numerator description" );
        indicatorA.setDenominatorDescription( "Denominator description" );
        long idA = indicatorService.addIndicator( indicatorA );
        indicatorA = indicatorService.getIndicator( idA );
        String numeratorTranslated = "Numerator description translated";
        String denominatorTranslated = "Denominator description translated";
        Set<Translation> listObjectTranslation = new HashSet<>( indicatorA.getTranslations() );
        listObjectTranslation
            .add( new Translation( locale.getLanguage(), "NUMERATOR_DESCRIPTION", numeratorTranslated ) );
        listObjectTranslation
            .add( new Translation( locale.getLanguage(), "DENOMINATOR_DESCRIPTION", denominatorTranslated ) );
        identifiableObjectManager.updateTranslations( indicatorA, listObjectTranslation );
        assertEquals( numeratorTranslated, indicatorA.getDisplayNumeratorDescription() );
        assertEquals( denominatorTranslated, indicatorA.getDisplayDenominatorDescription() );
    }
}
