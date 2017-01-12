package org.hisp.dhis.trackedentityattributevalue;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeoutException;

/**
 * @author Markus Bekken
 */
public class DefaultTrackedEntityAttributeReservedValueService
    implements TrackedEntityAttributeReservedValueService
{
    private static final Log log = LogFactory.getLog( DefaultTrackedEntityAttributeReservedValueService.class );
    private static final int GENERATION_TIMEOUT = 50;
    
    @Autowired
    private TrackedEntityAttributeReservedValueStore trackedEntityAttributeReservedValueStore;
    
    @Autowired
    private TrackedEntityAttributeValueService trackedEntityAttributeValueService;

    @Override
    public TrackedEntityAttributeReservedValue markTrackedEntityAttributeReservedValueAsUtilized(
        TrackedEntityAttribute attribute, TrackedEntityInstance trackedEntityInstance, String value )
    {
        if ( attribute.isGenerated() ) 
        {
            TrackedEntityAttributeReservedValue utilized = findTrackedEntityAttributeReservedValue(
                attribute, value );
            
            if ( utilized != null )
            {
                utilized.setValueUtilizedByTEI( trackedEntityInstance );
                utilized = trackedEntityAttributeReservedValueStore.saveTrackedEntityAttributeReservedValue( utilized );
            }
            
            return utilized;
        }
        
        return null;
    }

    @Override
    public List<TrackedEntityAttributeReservedValue> createTrackedEntityReservedValues(
        TrackedEntityAttribute attribute, int valuesToCreate )
    {
        Assert.isTrue( attribute.isGenerated(), "Attribute must be of type generated in order to reserve values" );
        
        ArrayList<TrackedEntityAttributeReservedValue> reservedValues = new ArrayList<TrackedEntityAttributeReservedValue>();
        
        for ( int i = 0; i < valuesToCreate; i++ ) 
        {
            try 
            {
                reservedValues.add( reserveCandidateValue( attribute ) );
            }
            catch ( Exception e ) 
            {
                log.warn( "Not able to provide all requested reserved values for  "
                    + attribute.getUid() + ".  " + (i + 1) + " of " + valuesToCreate + " created." );
                break;
            }
        }

        return reservedValues;
    }
    
    private String findValueCandidate( TrackedEntityAttribute trackedEntityAttribute ) 
        throws TimeoutException 
    {
        int timeout = 0;
        
        while ( timeout < GENERATION_TIMEOUT ) 
        {
            String candidate = generateRandomValueInPattern( trackedEntityAttribute.getPattern() );
            
            if ( !trackedEntityAttributeValueService.exists( trackedEntityAttribute, candidate ) )
            {
                //The generated ID was available. Check that it is not already reserved
                if ( findTrackedEntityAttributeReservedValue( trackedEntityAttribute, candidate ) == null ) 
                {
                    return candidate;
                }
            }
            
            timeout ++;
        }
        
        throw new TimeoutException( "Timeout while generating values, could not find unused values for "
            + trackedEntityAttribute.getUid() + " in " + GENERATION_TIMEOUT + " tries." );
    }
    
    @Transactional
    public TrackedEntityAttributeReservedValue reserveCandidateValue( TrackedEntityAttribute trackedEntityAttribute ) 
        throws TimeoutException 
    {
        //Retrieve a valid, non-taken and non-reserved ID
        String reservableValue = findValueCandidate( trackedEntityAttribute );
        
        if ( reservableValue != null ) 
        {
            TrackedEntityAttributeReservedValue newReservation = 
                new TrackedEntityAttributeReservedValue( trackedEntityAttribute, reservableValue );
            
            return trackedEntityAttributeReservedValueStore.saveTrackedEntityAttributeReservedValue( newReservation );
        }
        
        return null;
    }
    
    private String generateRandomValueInPattern( String pattern ) 
    {   
        if ( pattern.isEmpty() || ( pattern.matches( " *(#+|[0-9]+) *" ) && pattern.length() > 0 ) )
        {
            // This is a simplified pattern
            long min = 1000000;
            long max = 9999999;
            
            if ( !pattern.isEmpty() ) 
            {
                // Generate a random number with the same number of digits as given in the pattern.
                min = (long)Math.pow( 10, pattern.length() - 1 );
                max = (long)Math.pow( 10, pattern.length() ) - 1;
            }
           
            return String.valueOf( ThreadLocalRandom.current().nextLong( min, max ) );
        }
        else 
        {
            // RegEx generation, not covered yet
            throw new NotImplementedException();
        }
    }
    
    private TrackedEntityAttributeReservedValue findTrackedEntityAttributeReservedValue(
        TrackedEntityAttribute attribute, String value )
    {
        List<TrackedEntityAttributeReservedValue> values = trackedEntityAttributeReservedValueStore.
            getTrackedEntityReservedValues( attribute, value );
        
        Assert.state( ( values.size() <= 1 ), "Duplicate values reserved for attribute " + attribute.getUid() + " value " + value );
        
        return values.size() == 1 ? values.get( 0 ) : null;
    }

    @Override
    public String getGeneratedValue( TrackedEntityAttribute attribute )
        throws TimeoutException
    {
        Assert.state( attribute.isGenerated(), "Tracked entity attribute " + attribute.getUid() + " must be of type generated" );
        
        return findValueCandidate( attribute );
    }
}
