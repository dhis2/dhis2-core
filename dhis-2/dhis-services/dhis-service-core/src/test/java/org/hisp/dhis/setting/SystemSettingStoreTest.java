package org.hisp.dhis.setting;

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
import static org.junit.Assert.assertNull;

import java.util.List;

import org.hisp.dhis.DhisSpringTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Stian Strandli
 */
public class SystemSettingStoreTest
    extends DhisSpringTest
{
    @Autowired
    private SystemSettingStore systemSettingStore;

    private SystemSetting settingA;
    private SystemSetting settingB;
    private SystemSetting settingC;

    @Override
    public void setUpTest()
        throws Exception
    {
        settingA = new SystemSetting();
        settingA.setName( "Setting1" );
        settingA.setValue( "Value1" );

        settingB = new SystemSetting();
        settingB.setName( "Setting2" );
        settingB.setValue( "Value2" );

        settingC = new SystemSetting();
        settingC.setName( "Setting3" );
        settingC.setValue( "Value3" );
    }

    @Test
    public void testAddSystemSetting()
    {
        int idA = systemSettingStore.save( settingA );
        systemSettingStore.save( settingB );
        systemSettingStore.save( settingC );

        settingA = systemSettingStore.get( idA );
        assertNotNull( settingA );
        assertEquals( "Setting1", settingA.getName() );
        assertEquals( "Value1", settingA.getValue() );

        settingA.setValue( "Value1.1" );
        systemSettingStore.update( settingA );

        settingA = systemSettingStore.get( idA );
        assertNotNull( settingA );
        assertEquals( "Setting1", settingA.getName() );
        assertEquals( "Value1.1", settingA.getValue() );        
    }

    @Test
    public void testUpdateSystemSetting()
    {
        int id = systemSettingStore.save( settingA );
        
        settingA = systemSettingStore.get( id );
        
        assertEquals( "Value1", settingA.getValue() );
        
        settingA.setValue( "Value2" );
        
        systemSettingStore.update( settingA );

        settingA = systemSettingStore.get( id );
        
        assertEquals( "Value2", settingA.getValue() );
    }

    @Test
    public void testDeleteSystemSetting()
    {
        int idA = systemSettingStore.save( settingA );
        int idB = systemSettingStore.save( settingB );
        systemSettingStore.save( settingC );

        systemSettingStore.delete( settingA );

        assertNull( systemSettingStore.get( idA ) );
        assertNotNull( systemSettingStore.get( idB ) );
    }

    @Test
    public void testGetSystemSetting()
    {
        systemSettingStore.save( settingA );
        systemSettingStore.save( settingB );

        SystemSetting s = systemSettingStore.getByName( "Setting1" );
        assertNotNull( s );
        assertEquals( "Setting1", s.getName() );
        assertEquals( "Value1", s.getValue() );

        s = systemSettingStore.getByName( "Setting3" );
        assertNull( s );
    }

    @Test
    public void testGetAllSystemSettings()
    {
        List<SystemSetting> settings = systemSettingStore.getAll();
        assertNotNull( settings );
        assertEquals( 0, settings.size() );

        systemSettingStore.save( settingA );
        systemSettingStore.save( settingB );

        settings = systemSettingStore.getAll();
        assertNotNull( settings );
        assertEquals( 2, settings.size() );
    }
}
