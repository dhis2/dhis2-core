package org.hisp.dhis.program;

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
import org.hisp.dhis.common.GenericIdentifiableObjectStore;
import org.junit.Test;

import javax.annotation.Resource;

import static org.junit.Assert.*;

/**
 * @author Viet Nguyen <viet@dhis2.org>
 */
public class ProgramIndicatorGroupStoreTest
    extends DhisSpringTest
{

    // -----------------------------------------------------
    // Dependencies
    // -----------------------------------------------------

    @Resource(name="org.hisp.dhis.program.ProgramIndicatorGroupStore")
    private GenericIdentifiableObjectStore<ProgramIndicatorGroup> programIndicatorGroupStore;

    public void setIndicatorGroupStore( GenericIdentifiableObjectStore<ProgramIndicatorGroup> programIndicatorGroupStore )
    {
        this.programIndicatorGroupStore = programIndicatorGroupStore;
    }

    // -----------------------------------------------------
    // Variables
    // -----------------------------------------------------

    private ProgramIndicatorGroup programIndicatorGroupA;

    // -----------------------------------------------------
    // Set up test
    // -----------------------------------------------------

    @Override
    protected void setUpTest() throws Exception
    {
        programIndicatorGroupA = new ProgramIndicatorGroup( "A" );
    }

    // -----------------------------------------------------
    // Test
    // -----------------------------------------------------

    @Test
    public void testCreateProgramIndicatorGroup()
    {
        programIndicatorGroupStore.save( programIndicatorGroupA );
        assertNotNull( programIndicatorGroupA.getUid() );
    }

    @Test
    public void testUpdateProgramIndicatorGroup()
    {
        programIndicatorGroupStore.save( programIndicatorGroupA );

        programIndicatorGroupA.setName( "B" );
        programIndicatorGroupStore.save( programIndicatorGroupA );

        assertEquals( "B", programIndicatorGroupA.getName() );
    }

    @Test
    public void testDeleteProgramIndicatorGroup()
    {
        programIndicatorGroupStore.save( programIndicatorGroupA );
        int id = programIndicatorGroupA.getId();

        programIndicatorGroupStore.delete( programIndicatorGroupA );

        assertNull( programIndicatorGroupStore.get( id ) );

    }

}

