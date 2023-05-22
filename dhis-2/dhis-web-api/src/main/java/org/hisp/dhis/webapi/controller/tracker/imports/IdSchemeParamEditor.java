/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.webapi.controller.tracker.imports;

import java.beans.PropertyEditorSupport;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.tracker.imports.TrackerIdScheme;
import org.hisp.dhis.tracker.imports.TrackerIdSchemeParam;
import org.springframework.util.StringUtils;

public class IdSchemeParamEditor extends PropertyEditorSupport
{
    private static final String VALID_VALUES = "Valid values are: [UID, CODE, NAME, ATTRIBUTE:attributeUid]";

    @Override
    public void setAsText( String source )
    {
        if ( !StringUtils.hasText( source ) )
        {
            throw new IllegalArgumentException( VALID_VALUES );
        }

        String[] splitParam = source.split( ":" );
        String attributeUid = splitParam.length > 1 ? splitParam[1] : null;

        TrackerIdScheme idScheme;
        try
        {
            idScheme = TrackerIdScheme.valueOf( splitParam[0] );
        }
        catch ( IllegalArgumentException ex )
        {
            throw new IllegalArgumentException( VALID_VALUES );
        }

        boolean isInvalidAttribute = attributeUid != null && !CodeGenerator.isValidUid( attributeUid );
        boolean isInvalidFormat = splitParam.length > 2;

        boolean attributeIdSchemeHasNoAttributeId = idScheme == TrackerIdScheme.ATTRIBUTE && attributeUid == null;
        boolean notAttributeIdSchemeHasAttributeId = idScheme != TrackerIdScheme.ATTRIBUTE && attributeUid != null;
        if ( isInvalidAttribute || isInvalidFormat ||
            attributeIdSchemeHasNoAttributeId || notAttributeIdSchemeHasAttributeId )
        {
            throw new IllegalArgumentException( VALID_VALUES );
        }

        switch ( idScheme )
        {
            case UID:
                setValue( TrackerIdSchemeParam.UID );
                break;
            case NAME:
                setValue( TrackerIdSchemeParam.NAME );
                break;
            case CODE:
                setValue( TrackerIdSchemeParam.CODE );
                break;
            case ATTRIBUTE:
                setValue( TrackerIdSchemeParam.ofAttribute( attributeUid ) );
                break;
            default:
                throw new IllegalArgumentException( VALID_VALUES );
        }
    }
}