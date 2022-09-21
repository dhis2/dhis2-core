<<<<<<< HEAD
package org.hisp.dhis.setting;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import com.google.common.collect.ImmutableSet;
import org.hisp.dhis.DhisSpringTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.hisp.dhis.setting.SettingKey.*;
import static org.junit.Assert.*;

/**
 * @author Stian Strandli
 * @author Lars Helge Overland
 */
public class SystemSettingManagerTest
    extends DhisSpringTest
{
    @Autowired
    private SystemSettingManager systemSettingManager;

    @Override
    public void setUpTest()
    {
        systemSettingManager.invalidateCache();
    }

    @Test
    public void testSaveGetSetting()
    {
        systemSettingManager.saveSystemSetting( APPLICATION_INTRO, "valueA" );
        systemSettingManager.saveSystemSetting( APPLICATION_NOTIFICATION, "valueB" );

        assertEquals( "valueA", systemSettingManager.getSystemSetting( APPLICATION_INTRO ) );
        assertEquals( "valueB", systemSettingManager.getSystemSetting( APPLICATION_NOTIFICATION ) );
    }

    @Test
    public void testSaveGetSettingWithDefault()
    {
        assertEquals( EMAIL_PORT.getDefaultValue(), systemSettingManager.getSystemSetting( EMAIL_PORT ) );
    }

    @Test
    public void testSaveGetDeleteSetting()
    {
        assertNull( systemSettingManager.getSystemSetting( APPLICATION_INTRO ) );
        assertEquals( HELP_PAGE_LINK.getDefaultValue(), systemSettingManager.getSystemSetting( HELP_PAGE_LINK ) );

        systemSettingManager.saveSystemSetting( APPLICATION_INTRO, "valueA" );
        systemSettingManager.saveSystemSetting( HELP_PAGE_LINK, "valueB" );

        assertEquals( "valueA", systemSettingManager.getSystemSetting( APPLICATION_INTRO ) );
        assertEquals( "valueB", systemSettingManager.getSystemSetting( HELP_PAGE_LINK ) );

        systemSettingManager.deleteSystemSetting( APPLICATION_INTRO );

        assertNull( systemSettingManager.getSystemSetting( APPLICATION_INTRO ) );
        assertEquals( "valueB", systemSettingManager.getSystemSetting( HELP_PAGE_LINK ) );

        systemSettingManager.deleteSystemSetting( HELP_PAGE_LINK );

        assertNull( systemSettingManager.getSystemSetting( APPLICATION_INTRO ) );
        assertEquals( HELP_PAGE_LINK.getDefaultValue(), systemSettingManager.getSystemSetting( HELP_PAGE_LINK ) );
    }

    @Test
    public void testGetAllSystemSettings()
    {
        systemSettingManager.saveSystemSetting( APPLICATION_INTRO, "valueA" );
        systemSettingManager.saveSystemSetting( APPLICATION_NOTIFICATION, "valueB" );

        List<SystemSetting> settings = systemSettingManager.getAllSystemSettings();

        assertNotNull( settings );
        assertEquals( 2, settings.size() );
    }

    @Test
    public void testGetSystemSettingsAsMap()
    {
        systemSettingManager.saveSystemSetting( SettingKey.APPLICATION_TITLE, "valueA" );
        systemSettingManager.saveSystemSetting( SettingKey.APPLICATION_NOTIFICATION, "valueB" );

        Map<String, Serializable> settingsMap = systemSettingManager.getSystemSettingsAsMap();

        assertTrue( settingsMap.containsKey( SettingKey.APPLICATION_TITLE.getName() ) );
        assertTrue( settingsMap.containsKey( SettingKey.APPLICATION_NOTIFICATION.getName() ) );

        assertEquals( "valueA", settingsMap.get( SettingKey.APPLICATION_TITLE.getName() ) );
        assertEquals( "valueB", settingsMap.get( SettingKey.APPLICATION_NOTIFICATION.getName() ) );
        assertEquals( SettingKey.CACHE_STRATEGY.getDefaultValue(), settingsMap.get( SettingKey.CACHE_STRATEGY.getName() ) );
        assertEquals( SettingKey.CREDENTIALS_EXPIRES.getDefaultValue(), settingsMap.get( SettingKey.CREDENTIALS_EXPIRES.getName() ) );
    }

    @Test
    public void testGetSystemSettingsByCollection()
    {
        Collection<SettingKey> keys = ImmutableSet
            .of( SettingKey.APPLICATION_TITLE, SettingKey.APPLICATION_INTRO );

        systemSettingManager.saveSystemSetting( APPLICATION_TITLE, "valueA" );
        systemSettingManager.saveSystemSetting( APPLICATION_INTRO, "valueB" );

        assertEquals( systemSettingManager.getSystemSettings( keys ).size(), 2 );
    }

    @Test
    public void testIsConfidential()
    {
        assertEquals( SettingKey.EMAIL_PASSWORD.isConfidential(), true );
        assertEquals( systemSettingManager.isConfidential( SettingKey.EMAIL_PASSWORD.getName() ), true );

        assertEquals( SettingKey.EMAIL_HOST_NAME.isConfidential(), false );
        assertEquals( systemSettingManager.isConfidential( SettingKey.EMAIL_HOST_NAME.getName() ), false);
=======
/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.setting;

import static org.hisp.dhis.setting.SettingKey.*;
import static org.junit.Assert.*;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.hisp.dhis.DhisSpringTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.ImmutableSet;

/**
 * @author Stian Strandli
 * @author Lars Helge Overland
 */
public class SystemSettingManagerTest
    extends DhisSpringTest
{
    @Autowired
    private SystemSettingManager systemSettingManager;

    @Override
    public void setUpTest()
    {
        systemSettingManager.invalidateCache();
    }

    @Test
    public void testSaveGetSetting()
    {
        systemSettingManager.saveSystemSetting( APPLICATION_INTRO, "valueA" );
        systemSettingManager.saveSystemSetting( APPLICATION_NOTIFICATION, "valueB" );

        assertEquals( "valueA", systemSettingManager.getSystemSetting( APPLICATION_INTRO ) );
        assertEquals( "valueB", systemSettingManager.getSystemSetting( APPLICATION_NOTIFICATION ) );
    }

    @Test
    public void testSaveGetSettingWithDefault()
    {
        assertEquals( EMAIL_PORT.getDefaultValue(), systemSettingManager.getSystemSetting( EMAIL_PORT ) );
    }

    @Test
    public void testSaveGetDeleteSetting()
    {
        assertNull( systemSettingManager.getSystemSetting( APPLICATION_INTRO ) );
        assertEquals( HELP_PAGE_LINK.getDefaultValue(), systemSettingManager.getSystemSetting( HELP_PAGE_LINK ) );

        systemSettingManager.saveSystemSetting( APPLICATION_INTRO, "valueA" );
        systemSettingManager.saveSystemSetting( HELP_PAGE_LINK, "valueB" );

        assertEquals( "valueA", systemSettingManager.getSystemSetting( APPLICATION_INTRO ) );
        assertEquals( "valueB", systemSettingManager.getSystemSetting( HELP_PAGE_LINK ) );

        systemSettingManager.deleteSystemSetting( APPLICATION_INTRO );

        assertNull( systemSettingManager.getSystemSetting( APPLICATION_INTRO ) );
        assertEquals( "valueB", systemSettingManager.getSystemSetting( HELP_PAGE_LINK ) );

        systemSettingManager.deleteSystemSetting( HELP_PAGE_LINK );

        assertNull( systemSettingManager.getSystemSetting( APPLICATION_INTRO ) );
        assertEquals( HELP_PAGE_LINK.getDefaultValue(), systemSettingManager.getSystemSetting( HELP_PAGE_LINK ) );
    }

    @Test
    public void testGetAllSystemSettings()
    {
        systemSettingManager.saveSystemSetting( APPLICATION_INTRO, "valueA" );
        systemSettingManager.saveSystemSetting( APPLICATION_NOTIFICATION, "valueB" );

        List<SystemSetting> settings = systemSettingManager.getAllSystemSettings();

        assertNotNull( settings );
        assertEquals( 2, settings.size() );
    }

    @Test
    public void testGetSystemSettingsAsMap()
    {
        systemSettingManager.saveSystemSetting( SettingKey.APPLICATION_TITLE, "valueA" );
        systemSettingManager.saveSystemSetting( SettingKey.APPLICATION_NOTIFICATION, "valueB" );

        Map<String, Serializable> settingsMap = systemSettingManager.getSystemSettingsAsMap();

        assertTrue( settingsMap.containsKey( SettingKey.APPLICATION_TITLE.getName() ) );
        assertTrue( settingsMap.containsKey( SettingKey.APPLICATION_NOTIFICATION.getName() ) );

        assertEquals( "valueA", settingsMap.get( SettingKey.APPLICATION_TITLE.getName() ) );
        assertEquals( "valueB", settingsMap.get( SettingKey.APPLICATION_NOTIFICATION.getName() ) );
        assertEquals( SettingKey.CACHE_STRATEGY.getDefaultValue(),
            settingsMap.get( SettingKey.CACHE_STRATEGY.getName() ) );
        assertEquals( SettingKey.CREDENTIALS_EXPIRES.getDefaultValue(),
            settingsMap.get( SettingKey.CREDENTIALS_EXPIRES.getName() ) );
    }

    @Test
    public void testGetSystemSettingsByCollection()
    {
        Collection<SettingKey> keys = ImmutableSet
            .of( SettingKey.APPLICATION_TITLE, SettingKey.APPLICATION_INTRO );

        systemSettingManager.saveSystemSetting( APPLICATION_TITLE, "valueA" );
        systemSettingManager.saveSystemSetting( APPLICATION_INTRO, "valueB" );

        assertEquals( systemSettingManager.getSystemSettings( keys ).size(), 2 );
    }

    @Test
    public void testIsConfidential()
    {
        assertEquals( SettingKey.EMAIL_PASSWORD.isConfidential(), true );
        assertEquals( systemSettingManager.isConfidential( SettingKey.EMAIL_PASSWORD.getName() ), true );

        assertEquals( SettingKey.EMAIL_HOST_NAME.isConfidential(), false );
        assertEquals( systemSettingManager.isConfidential( SettingKey.EMAIL_HOST_NAME.getName() ), false );
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
    }
}
