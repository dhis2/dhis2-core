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
package org.hisp.dhis.constant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.hisp.dhis.DhisSpringTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Jim Grace
 */
public class ConstantServiceTest
    extends DhisSpringTest
{
    @Autowired
    private ConstantService constantService;

    private Constant constantA;

    private Constant constantB;

    private Constant constantB2;

    private Constant constantB3;

    @Override
    protected void setUpTest()
    {
        constantA = createConstant( 'A', 12 );
        constantA.setUid( "ConstantUiA" );

        constantB = createConstant( 'B', 13 );
        constantB.setUid( "ConstantUiB" );

        constantB2 = createConstant( 'C', 14 );
        constantB2.setName( "ConstantB2" );
        constantB2.setUid( "ConstantUi2" );

        constantB3 = createConstant( 'D', 15 );
        constantB3.setName( "ConstantB3" );
        constantB3.setUid( "ConstantUi3" );
    }

    @Test
    public void testSaveConstant()
    {
        long constantId = constantService.saveConstant( constantA );

        Constant constant = constantService.getConstant( constantA.getUid() );

        assertNotNull( constant );
        assertEquals( constantId, constantA.getId() );
        assertEquals( constant, constantA );
    }

    @Test
    public void testUpdateConstant()
    {
        constantService.saveConstant( constantA );

        constantA.setValue( 20 );

        constantService.updateConstant( constantA );

        assertEquals( 20.0, constantService.getConstant( constantA.getUid() ).getValue(), .01 );
    }

    @Test
    public void testDeleteConstant()
    {
        constantService.saveConstant( constantA );

        assertNotNull( constantService.getConstant( constantA.getUid() ) );

        constantService.deleteConstant( constantA );

        assertNull( constantService.getConstant( constantA.getUid() ) );
    }

    @Test
    public void testAllConstants()
    {
        constantService.saveConstant( constantA );
        constantService.saveConstant( constantB );

        List<Constant> allConstants = constantService.getAllConstants();

        assertEquals( 2, allConstants.size() );
        assertTrue( allConstants.contains( constantA ) );
        assertTrue( allConstants.contains( constantB ) );
    }

    @Test
    public void testAllConstantsAfterSave()
    {
        constantService.saveConstant( constantA );

        List<Constant> allConstants = constantService.getAllConstants();

        assertEquals( 1, allConstants.size() );
        assertTrue( allConstants.contains( constantA ) );

        constantService.saveConstant( constantB );

        allConstants = constantService.getAllConstants();

        assertEquals( 2, allConstants.size() );
        assertTrue( allConstants.contains( constantA ) );
        assertTrue( allConstants.contains( constantB ) );
    }

    @Test
    public void testAllConstantsAfterUpdate()
    {
        constantService.saveConstant( constantA );
        constantService.saveConstant( constantB );

        List<Constant> allConstants = constantService.getAllConstants();

        assertEquals( 2, allConstants.size() );
        assertTrue( allConstants.contains( constantA ) );
        assertTrue( allConstants.contains( constantB ) );

        constantA.setValue( 20 );
        constantService.updateConstant( constantA );

        allConstants = constantService.getAllConstants();

        assertEquals( 2, allConstants.size() );
        assertTrue( allConstants.contains( constantA ) );
        assertTrue( allConstants.contains( constantB ) );
    }

    @Test
    public void testAllConstantsAfterDelete()
    {
        constantService.saveConstant( constantA );
        constantService.saveConstant( constantB );

        List<Constant> allConstants = constantService.getAllConstants();

        assertEquals( 2, allConstants.size() );
        assertTrue( allConstants.contains( constantA ) );
        assertTrue( allConstants.contains( constantB ) );

        constantService.deleteConstant( constantB );

        allConstants = constantService.getAllConstants();

        assertEquals( 1, allConstants.size() );
        assertTrue( allConstants.contains( constantA ) );
    }

    @Test
    public void testGetConstantMap()
    {
        constantService.saveConstant( constantA );
        constantService.saveConstant( constantB );

        Map<String, Constant> constantMap = constantService.getConstantMap();

        assertEquals( 2, constantMap.size() );
        assertEquals( constantA, constantMap.get( constantA.getUid() ) );
        assertEquals( constantB, constantMap.get( constantB.getUid() ) );
    }

    @Test
    public void testGetConstantParameterMap()
    {
        constantService.saveConstant( constantA );
        constantService.saveConstant( constantB );

        Map<String, Double> constantParameterMap = constantService.getConstantParameterMap();

        assertEquals( 2, constantParameterMap.size() );
        assertEquals( 12.0, constantParameterMap.get( constantA.getName() ), .01 );
        assertEquals( 13.0, constantParameterMap.get( constantB.getName() ), .01 );
    }

    @Test
    public void testGetConstantCount()
    {
        constantService.saveConstant( constantA );
        constantService.saveConstant( constantB );

        assertEquals( 2, constantService.getConstantCount() );
    }

    @Test
    public void testGetConstantCountByName()
    {
        constantService.saveConstant( constantA );
        constantService.saveConstant( constantB );
        constantService.saveConstant( constantB2 );

        assertEquals( 3, constantService.getConstantCountByName( "A" ) );
        assertEquals( 2, constantService.getConstantCountByName( "B" ) );
        assertEquals( 1, constantService.getConstantCountByName( "2" ) );
    }

    @Test
    public void testGetConstantsBetween()
    {
        constantService.saveConstant( constantA );
        constantService.saveConstant( constantB );
        constantService.saveConstant( constantB2 );

        List<Constant> constants = constantService.getConstantsBetween( 0, 2 );

        assertEquals( 2, constants.size() );
        assertTrue( constants.contains( constantA ) );
        assertTrue( constants.contains( constantB ) );

        constants = constantService.getConstantsBetween( 0, 3 );

        assertEquals( 3, constants.size() );
        assertTrue( constants.contains( constantA ) );
        assertTrue( constants.contains( constantB ) );
        assertTrue( constants.contains( constantB2 ) );

        constants = constantService.getConstantsBetween( 1, 3 );

        assertEquals( 2, constants.size() );
        assertTrue( constants.contains( constantB ) );
        assertTrue( constants.contains( constantB2 ) );
    }

    public void testGetConstantsBetweenByName()
    {
        constantService.saveConstant( constantA );
        constantService.saveConstant( constantB );
        constantService.saveConstant( constantB2 );
        constantService.saveConstant( constantB3 );

        List<Constant> constants = constantService.getConstantsBetweenByName( "A", 0, 3 );

        assertEquals( 4, constants.size() );
        assertTrue( constants.contains( constantA ) );
        assertTrue( constants.contains( constantB ) );
        assertTrue( constants.contains( constantB2 ) );
        assertTrue( constants.contains( constantB3 ) );

        constants = constantService.getConstantsBetweenByName( "A", 1, 2 );

        assertEquals( 2, constants.size() );
        assertTrue( constants.contains( constantB ) );
        assertTrue( constants.contains( constantB2 ) );

        constants = constantService.getConstantsBetweenByName( "B", 1, 2 );

        assertEquals( 2, constants.size() );
        assertTrue( constants.contains( constantB2 ) );
        assertTrue( constants.contains( constantB3 ) );
    }
}
