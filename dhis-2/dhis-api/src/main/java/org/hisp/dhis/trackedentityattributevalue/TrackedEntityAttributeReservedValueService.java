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

import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;

import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * @author Markus Bekken
 */
public interface TrackedEntityAttributeReservedValueService
{
    /**
     * Marks a {@link TrackedEntityAttributeReservedValue} as utilized(used/taken)
     * (Can now be sanitized from the TrackedEntityAttributeReservedValue table)
     * 
     * @param TrackedEntityInstance that used the value
     * @param TrackedEntityAttribute that was utilized
     * @param value that was utilized
     * @return the {@link TrackedEntityAttributeReservedValue} that was used, 
     * null if no reserved value was fount
     */
    TrackedEntityAttributeReservedValue markTrackedEntityAttributeReservedValueAsUtilized(
        TrackedEntityAttribute attribute, TrackedEntityInstance trackedEntityInstance, String value );
        
    /**
     * Gets a list of {@link TrackedEntityAttributeReservedValue} matching the parameters.
     * @param attribute {@link TrackedEntityAttribute} to get reserved values for.
     * @param valuesToCreate the number of reserved values to create and return
     * @return a list of {@link TrackedEntityAttributeReservedValue}
     * @throws Exception 
     */
    List<TrackedEntityAttributeReservedValue> createTrackedEntityReservedValues( 
        TrackedEntityAttribute attribute, int valuesToCreate );
    
    /**
     * Gets a generated non-taken value for the tracked entity attribute matching the parameters.
     * The value is not reserved, but was not taken at the time whe it was generated.
     * @param attribute {@link TrackedEntityAttribute} to get reserved values for
     * @return String value
     * @throws TimeoutException 
     * @throws Exception 
     */
    String getGeneratedValue( TrackedEntityAttribute attribute ) 
        throws TimeoutException;
}
