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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitStore;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeStore;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceStore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class TrackedEntityAttributeReservedValueServiceTest 
    extends DhisSpringTest
{
    private TrackedEntityAttribute trackedEntityAttributeA;
    private TrackedEntityAttribute trackedEntityAttributeB;
    private TrackedEntityAttribute trackedEntityAttributeC;
    private TrackedEntityAttribute trackedEntityAttributeD;
    
    private TrackedEntityInstance trackedEntityInstanceA;
    
    @Autowired
    private TrackedEntityAttributeReservedValueService trackedEntityAttributeReservedValueService;
    
    @Autowired
    private TrackedEntityAttributeReservedValueStore trackedEntityAttributeReservedValueStore;
    
    @Autowired
    private TrackedEntityAttributeStore trackedEntityAttributeStore;
    
    @Autowired
    private TrackedEntityInstanceStore trackedEntityInstanceStore;
    
    @Autowired
    private OrganisationUnitStore organisationUnitStore;
    
    @Override
    public void setUpTest()
    {
        trackedEntityAttributeA = createTrackedEntityAttribute( 'A' );
        trackedEntityAttributeB = createTrackedEntityAttribute( 'B' );
        trackedEntityAttributeC = createTrackedEntityAttribute( 'C' );
        trackedEntityAttributeD = createTrackedEntityAttribute( 'D' );
        trackedEntityAttributeA.setGenerated( true );
        trackedEntityAttributeB.setGenerated( false );
        trackedEntityAttributeC.setGenerated( true );
        trackedEntityAttributeC.setPattern( "######" );
        trackedEntityAttributeD.setGenerated( true );
        trackedEntityAttributeD.setPattern( "#" );
        
        trackedEntityAttributeStore.save( trackedEntityAttributeA );
        trackedEntityAttributeStore.save( trackedEntityAttributeB );
        trackedEntityAttributeStore.save( trackedEntityAttributeC );
        trackedEntityAttributeStore.save( trackedEntityAttributeD );
        
        OrganisationUnit ou = createOrganisationUnit( 'A' );
        organisationUnitStore.save( ou );
        
        trackedEntityInstanceA = createTrackedEntityInstance( 'A', ou );
        trackedEntityInstanceStore.save( trackedEntityInstanceA );
    }
    
    @Test
    public void testMarkAsUtilized()
    {
        TrackedEntityAttributeReservedValue reservedValue = new TrackedEntityAttributeReservedValue(
            trackedEntityAttributeA, "value");
        reservedValue = trackedEntityAttributeReservedValueStore.saveTrackedEntityAttributeReservedValue( reservedValue );
        
        reservedValue.setValueUtilizedByTEI( trackedEntityInstanceA );
        
        TrackedEntityAttributeReservedValue saved = trackedEntityAttributeReservedValueService.markTrackedEntityAttributeReservedValueAsUtilized( 
            trackedEntityAttributeA, trackedEntityInstanceA, "value" );
        assertNotNull( saved );
        assertTrue( saved.getValueUtilizedByTEI() == reservedValue.getValueUtilizedByTEI() );
    }
    
    @Test
    public void testReserveOneTrackedEntityAttributeValue() 
    {
        try 
        {
            List<TrackedEntityAttributeReservedValue> reservedValues = trackedEntityAttributeReservedValueService.createTrackedEntityReservedValues( trackedEntityAttributeA, 1 );

            assertTrue( reservedValues.size() == 1 );
        }
        catch( Exception e )
        {
            fail( "service threw unexpected exception" );
        }
    }
    
    @Test
    public void testReserveTrackedEntityAttributeValues() 
    {
        try
        {
            List<TrackedEntityAttributeReservedValue> reservedValues = trackedEntityAttributeReservedValueService.createTrackedEntityReservedValues( trackedEntityAttributeA, 10 );
            
            assertTrue( reservedValues.size() == 10 );
            assertTrue( reservedValues.get( 0 ).getValue() != null );
            assertTrue( reservedValues.get( 0 ).getValue() != reservedValues.get( 1 ).getValue() );
        }
        catch ( Exception e )
        {
            fail( "service threw unexpected exception" );
        }
    }
    
    @Test
    public void testReserveTrackedEntityAttributeValuesWhenNotGenerated() 
    {
        try
        {
            trackedEntityAttributeReservedValueService.createTrackedEntityReservedValues( trackedEntityAttributeB, 10 );
            
            fail( "Expected exception, as the TEA is not configured to be generated" );
        }
        catch ( Exception e )
        {
            //All good - expected exception when trying to generate a value for a non-generated TEA
        }
    }
    
    @Test
    public void testReserveTrackedEntityAttributeValueTillTimeout() 
    {
        try
        {
            List<TrackedEntityAttributeReservedValue> reservedValues = trackedEntityAttributeReservedValueService.createTrackedEntityReservedValues( trackedEntityAttributeD, 11 );
            //We requested 11 values, but expect less(as there are only 10 possible values for the pattern);
            
            assertTrue( reservedValues.size() < 11 );
        }
        catch ( Exception e )
        {
            fail( "No exception is expected to be thrown" );
        }
    }
    
    @Test
    public void testGenerateTrackedEntityAttributeValues() 
    {
        try
        {
            String reserved = trackedEntityAttributeReservedValueService.getGeneratedValue( trackedEntityAttributeC );
            
            assertTrue( reserved.length() == 6 );
        }
        catch ( Exception e )
        {
            fail( "service threw unexpected exception" );
        }
    }
    
    @Test
    public void testGenerateTrackedEntityAttributeValuesWhenNotGenerated() 
    {
        try
        {
            trackedEntityAttributeReservedValueService.createTrackedEntityReservedValues( trackedEntityAttributeB, 10 );
            
            fail( "Expected exception, as the TEA is not configured to be generated" );
        }
        catch ( Exception e )
        {
            //All good - expected exception when trying to generate a value for a non-generated TEA
        }
    }
}
