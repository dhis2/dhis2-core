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
package org.hisp.dhis.dxf2.geojson;

import org.hisp.dhis.dxf2.importsummary.ImportConflictDescriptor;
import org.hisp.dhis.feedback.ErrorCode;

/**
 * The different conflicts that can occur during a GeoJSON import.
 *
 * @author Jan Bernitt
 */
public enum GeoJsonImportConflict implements ImportConflictDescriptor
{
    /* global conflicts */
    /**
     * When reading the input stream fails for some IO related reason
     */
    INPUT_IO_ERROR( ErrorCode.E7700, "" ),
    /**
     * When parsing the JSON document showed that the input is not valid
     * (generic) JSON
     */
    INPUT_FORMAT_ERROR( ErrorCode.E7701, "" ),
    /**
     * When an attribute was defined but does not exist in DHIS2
     */
    ATTRIBUTE_NOT_FOUND( ErrorCode.E7702, "" ),
    /**
     * When an attribute was defined, but the existing
     * {@link org.hisp.dhis.attribute.Attribute} is not of
     * {@link org.hisp.dhis.common.ValueType#GEOJSON}
     */
    ATTRIBUTE_NOT_GEO_JSON( ErrorCode.E7703, "" ),
    /**
     * When an attribute was defined, exists and has
     * {@link org.hisp.dhis.common.ValueType#GEOJSON} but is not usable for
     * {@link org.hisp.dhis.organisationunit.OrganisationUnit}s.
     */
    ATTRIBUTE_NOT_USABLE( ErrorCode.E7704, "" ),

    /* individual conflicts */
    /**
     * When extracting the geometry identifier from the GeoJSON feature results
     * in null value
     */
    FEATURE_LACKS_IDENTIFIER( ErrorCode.E7705, "feature" ),
    /**
     * When the feature simply lacks a definition of a geometry
     */
    FEATURE_LACKS_GEOMETRY( ErrorCode.E7706, "feature" ),
    /**
     * When validating or parsing (and setting) the provided GeoJSON geometry
     * fails
     */
    GEOMETRY_INVALID( ErrorCode.E7707, "geometry" ),
    /**
     * When the organisation unit referenced by the feature does not exist
     */
    ORG_UNIT_NOT_FOUND( ErrorCode.E7708, "" ),
    /**
     * When storing the updated
     * {@link org.hisp.dhis.organisationunit.OrganisationUnit} fails for some
     * reason
     */
    ORG_UNIT_INVALID( ErrorCode.E7709, "geometry" );

    private final ErrorCode errorCode;

    private final String property;

    GeoJsonImportConflict( ErrorCode errorCode, String property )
    {
        this.errorCode = errorCode;
        this.property = property;
    }

    @Override
    public ErrorCode getErrorCode()
    {
        return errorCode;
    }

    @Override
    public Class<?>[] getObjectTypes()
    {
        return new Class[0];
    }

    @Override
    public String getProperty()
    {
        return property;
    }
}
