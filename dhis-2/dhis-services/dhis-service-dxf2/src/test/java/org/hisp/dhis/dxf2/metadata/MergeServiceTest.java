package org.hisp.dhis.dxf2.metadata;

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
import org.hisp.dhis.common.MergeMode;
import org.hisp.dhis.dxf2.metadata.merge.Simple;
import org.hisp.dhis.dxf2.metadata.merge.SimpleCollection;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class MergeServiceTest
    extends DhisSpringTest
{
    @Autowired
    private MergeService mergeService;

    @Override
    public void setUpTest()
    {

    }

    @Test
    public void simpleReplace()
    {
        Date date = new Date();
        Simple source = new Simple( "string", 10, date, false, 123, 2.5f );
        Simple target = new Simple();

        mergeService.merge( source, target, MergeMode.REPLACE );

        assertEquals( "string", target.getString() );
        assertEquals( 10, (int) target.getInteger() );
        assertEquals( date, target.getDate() );
        assertEquals( false, target.getBool() );
        assertEquals( 123, target.getAnInt() );
    }

    @Test
    public void simpleMerge()
    {
        Date date = new Date();
        Simple source = new Simple( null, 10, date, null, 123, 2.5f );
        Simple target = new Simple( "hello", 20, date, true, 123, 2.5f );

        mergeService.merge( source, target, MergeMode.MERGE );

        assertEquals( "hello", target.getString() );
        assertEquals( 10, (int) target.getInteger() );
        assertEquals( date, target.getDate() );
        assertEquals( true, target.getBool() );
    }

    @Test
    public void simpleCollection()
    {
        Date date = new Date();

        SimpleCollection source = new SimpleCollection( "name" );
        source.getSimples().add( new Simple( "simple", 10, date, false, 123, 2.5f ) );
        source.getSimples().add( new Simple( "simple", 20, date, false, 123, 2.5f ) );
        source.getSimples().add( new Simple( "simple", 30, date, false, 123, 2.5f ) );

        SimpleCollection target = new SimpleCollection( "target" );

        mergeService.merge( source, target, MergeMode.MERGE );

        assertEquals( "name", target.getName() );
        assertEquals( 3, target.getSimples().size() );

        assertTrue( target.getSimples().contains( source.getSimples().get( 0 ) ) );
        assertTrue( target.getSimples().contains( source.getSimples().get( 1 ) ) );
        assertTrue( target.getSimples().contains( source.getSimples().get( 2 ) ) );
    }
}
