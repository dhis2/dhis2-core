package org.hisp.dhis.setting;

/*
 * Copyright (c) 2004-2015, University of Oslo
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
import static org.junit.Assert.assertNull;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.hisp.dhis.DhisSpringTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

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
    public void testSaveGetSystemSetting()
    {
        systemSettingManager.saveSystemSetting( "settingA", "valueA" );
        systemSettingManager.saveSystemSetting( "settingB", "valueB" );

        assertEquals( "valueA", systemSettingManager.getSystemSetting( "settingA" ) );
        assertEquals( "valueB", systemSettingManager.getSystemSetting( "settingB" ) );
    }

    @Test
    public void testSaveGetSetting()
    {
        systemSettingManager.saveSystemSetting( Setting.APPLICATION_INTRO, "valueA" );
        systemSettingManager.saveSystemSetting( Setting.APPLICATION_NOTIFICATION, "valueB" );

        assertEquals( "valueA", systemSettingManager.getSystemSetting( Setting.APPLICATION_INTRO ) );
        assertEquals( "valueB", systemSettingManager.getSystemSetting( Setting.APPLICATION_NOTIFICATION ) );
    }

    @Test
    public void testSaveGetSettingWithDefault()
    {
        assertEquals( Setting.APP_STORE_URL.getDefaultValue(), systemSettingManager.getSystemSetting( Setting.APP_STORE_URL ) );
        assertEquals( Setting.EMAIL_PORT.getDefaultValue(), systemSettingManager.getSystemSetting( Setting.EMAIL_PORT ) );
    }

    @Test
    public void testSaveGetDeleteSetting()
    {
        assertNull( systemSettingManager.getSystemSetting( Setting.APPLICATION_INTRO ) );
        assertEquals( Setting.HELP_PAGE_LINK.getDefaultValue(), systemSettingManager.getSystemSetting( Setting.HELP_PAGE_LINK ) );
        
        systemSettingManager.saveSystemSetting( Setting.APPLICATION_INTRO, "valueA" );
        systemSettingManager.saveSystemSetting( Setting.HELP_PAGE_LINK, "valueB" );

        assertEquals( "valueA", systemSettingManager.getSystemSetting( Setting.APPLICATION_INTRO ) );
        assertEquals( "valueB", systemSettingManager.getSystemSetting( Setting.HELP_PAGE_LINK ) );
        
        systemSettingManager.deleteSystemSetting( Setting.APPLICATION_INTRO );

        assertNull( systemSettingManager.getSystemSetting( Setting.APPLICATION_INTRO ) );
        assertEquals( "valueB", systemSettingManager.getSystemSetting( Setting.HELP_PAGE_LINK ) );

        systemSettingManager.deleteSystemSetting( Setting.HELP_PAGE_LINK );
        
        assertNull( systemSettingManager.getSystemSetting( Setting.APPLICATION_INTRO ) );
        assertEquals( Setting.HELP_PAGE_LINK.getDefaultValue(), systemSettingManager.getSystemSetting( Setting.HELP_PAGE_LINK ) );        
    }

    @Test
    public void testGetAllSystemSettings()
    {
        systemSettingManager.saveSystemSetting( "settingA", "valueA" );
        systemSettingManager.saveSystemSetting( "settingB", "valueB" );
        systemSettingManager.saveSystemSetting( "settingC", "valueC" );
        
        List<SystemSetting> settings = systemSettingManager.getAllSystemSettings();
        
        assertNotNull( settings );
        assertEquals( 3, settings.size() );
    }

    @Test
    public void testGetSystemSettingsAsMap()
    {
        systemSettingManager.saveSystemSetting( "settingA", "valueA" );
        systemSettingManager.saveSystemSetting( "settingB", "valueB" );
        systemSettingManager.saveSystemSetting( "settingC", "valueC" );

        Map<String, Serializable> settingsMap = systemSettingManager.getSystemSettingsAsMap();

        assertTrue( settingsMap.containsKey( "settingA" ) );
        assertTrue( settingsMap.containsKey( "settingB" ) );
        assertTrue( settingsMap.containsKey( "settingC" ) );
        assertEquals( "valueA", settingsMap.get( "settingA" ) );
        assertEquals( "valueB", settingsMap.get( "settingB" ) );
        assertEquals( "valueC", settingsMap.get( "settingC" ) );
    }
}
