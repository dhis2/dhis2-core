package org.hisp.dhis.trackedentityattributevalue;
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
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeStore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class TrackedEntityAttributeReservedValueStoreTest
    extends DhisSpringTest
{
    private TrackedEntityAttribute trackedEntityAttributeA;
    private TrackedEntityAttribute trackedEntityAttributeB;
    
    @Autowired
    private TrackedEntityAttributeReservedValueStore trackedEntityAttributeReservedValueStore;
    
    @Autowired
    private TrackedEntityAttributeStore trackedEntityAttributeStore;
    
    @Override
    public void setUpTest()
    {
        trackedEntityAttributeA = createTrackedEntityAttribute( 'A' );
        trackedEntityAttributeB = createTrackedEntityAttribute( 'B' );
        
        trackedEntityAttributeStore.save( trackedEntityAttributeA );
        trackedEntityAttributeStore.save( trackedEntityAttributeB );
    }
    
    @Test
    public void testAddGet()
    {
        TrackedEntityAttributeReservedValue reservedValue = new TrackedEntityAttributeReservedValue(
            trackedEntityAttributeA, "value");
        trackedEntityAttributeReservedValueStore.saveTrackedEntityAttributeReservedValue( reservedValue );
        
        List<TrackedEntityAttributeReservedValue> result = 
            trackedEntityAttributeReservedValueStore.getTrackedEntityReservedValues( trackedEntityAttributeA, "a" );
        
        assertTrue( result.size() == 0 );
        
        result = trackedEntityAttributeReservedValueStore.getTrackedEntityReservedValues( trackedEntityAttributeA, "value" );
        
        assertTrue( result.size() == 1 );
        assertEquals( result.get( 0 ).getTrackedEntityAttribute(), reservedValue.getTrackedEntityAttribute() );
        assertEquals( result.get( 0 ).getCreated(), reservedValue.getCreated() );
        assertEquals( result.get( 0 ).getExpiryDate(), reservedValue.getExpiryDate() );
        assertEquals( result.get( 0 ).getValue(), reservedValue.getValue() );
        
        result = trackedEntityAttributeReservedValueStore.getTrackedEntityReservedValues( trackedEntityAttributeB, "value" );
        
        assertTrue( result.size() == 0 );
    }
}
