/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.analytics.event.data;

import java.util.List;

import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.feedback.ErrorCode;

/**
 * @author Dusan Bernat
 */
public interface EventCoordinateService
{
    /**
     * Verifies the validity of fallback coordinate field.
     *
     * @param isRegistration true when underlying program is registration
     * @param coordinateField the name of coordinate field (uid or name)
     * @return returns true if valid.
     * @throws IllegalQueryException
     */
    boolean isFallbackCoordinateFieldValid( boolean isRegistration, String coordinateField )
        throws IllegalQueryException;

    /**
     * Provides list of coordinate fields.
     *
     * @param program underlying program
     * @param fallbackCoordinateField fallback
     * @param defaultCoordinateFallback fallback cascade should be applied when
     *        true
     * @return list of coordinate fields
     * @throws IllegalQueryException
     */
    List<String> getFallbackCoordinateFields( String program, String fallbackCoordinateField,
        boolean defaultCoordinateFallback );

    /**
     * Provides verified geometry.
     *
     * @param valueType value type
     * @param field geometry
     * @param errorCode code for standard error message
     * @return the coordinate field.
     * @throws IllegalQueryException
     */
    String getCoordinateField( ValueType valueType, String field, ErrorCode errorCode )
        throws IllegalQueryException;

    /**
     * Provides verified geometry.
     *
     * @param program underlying program
     * @param coordinateField geometry
     * @param errorCode code for standard error message
     * @return the coordinate field.
     * @throws IllegalQueryException
     */
    public String getCoordinateField( String program, String coordinateField, ErrorCode errorCode )
        throws IllegalQueryException;
}