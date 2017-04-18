package org.hisp.dhis.option;

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
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;

import static org.junit.Assert.*;


/**
 * @author Viet Nguyen <viet@dhis2.org>
 */
public class OptionGroupStoreTest
    extends DhisSpringTest
{
    @Autowired
    private OptionGroupStore store;

    private OptionGroup optionGroupA;

    private OptionGroup optionGroupB;

    private OptionGroup optionGroupC;

    public void setUpTest()
    {
        optionGroupA = new OptionGroup( "OptionGroupA" );
        optionGroupA.setShortName( "ShortNameA" );

        optionGroupB = new OptionGroup( "OptionGroupB" );
        optionGroupB.setShortName( "ShortNameB" );

        optionGroupC = new OptionGroup( "OptionGroupC" );
        optionGroupC.setShortName( "ShortNameC" );
    }

    @Test
    public void tetAddOptionGroup()
    {
        store.save( optionGroupA );
        int idA = optionGroupA.getId();
        store.save( optionGroupB );
        int idB = optionGroupB.getId();
        store.save( optionGroupC );
        int idC = optionGroupC.getId();

        assertEquals( optionGroupA, store.get( idA ));
        assertEquals( optionGroupB, store.get( idB ));
        assertEquals( optionGroupC, store.get( idC ));
    }

    @Test
    public void testDeleteOptionGroup()
    {
        store.save( optionGroupA );
        int idA = optionGroupA.getId();
        store.save( optionGroupB );
        int idB = optionGroupB.getId();

        store.delete( optionGroupA );

        assertNull( store.get( idA ) );
        assertNotNull( store.get( idB ) );
    }

    @Test
    public void genericGetAll()
    {
        store.save( optionGroupA );
        store.save( optionGroupB );
        store.save( optionGroupC );

        Collection<OptionGroup> objects = store.getAll();

        assertNotNull( objects );
        assertEquals( 3, objects.size() );
        assertTrue( objects.contains( optionGroupA ) );
        assertTrue( objects.contains( optionGroupB ) );
        assertTrue( objects.contains( optionGroupC ) );
    }
}
