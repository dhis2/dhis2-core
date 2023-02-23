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
package org.hisp.dhis.webapi.controller.tracker.export.fieldsmapper;

import static org.hisp.dhis.dxf2.events.Param.DELETED;
import static org.hisp.dhis.webapi.controller.tracker.export.fieldsmapper.FieldsParamMapper.rootFields;

import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.dxf2.events.EnrollmentParams;
import org.hisp.dhis.dxf2.events.Param;
import org.hisp.dhis.fieldfiltering.FieldFilterService;
import org.hisp.dhis.fieldfiltering.FieldPath;
import org.hisp.dhis.fieldfiltering.FieldPreset;
import org.hisp.dhis.webapi.controller.tracker.view.Enrollment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EnrollmentFieldsParamMapper
{
    private final FieldFilterService fieldFilterService;

    public EnrollmentParams map( List<FieldPath> fields )
    {
        return map( fields, false );
    }

    public EnrollmentParams map( List<FieldPath> fields, boolean includeDeleted )
    {
        EnrollmentParams params = initUsingAllOrNoFields( rootFields( fields ) );

        for ( Param p : Param.values() )
        {
            params = params.with( p,
                fieldFilterService.filterIncludes( Enrollment.class, fields, p.getFieldPath() ) );
        }
        return params.with( DELETED, includeDeleted );
    }

    private static EnrollmentParams initUsingAllOrNoFields( Map<String, FieldPath> roots )
    {
        EnrollmentParams params = EnrollmentParams.FALSE;
        if ( roots.containsKey( FieldPreset.ALL ) )
        {
            FieldPath p = roots.get( FieldPreset.ALL );
            if ( p.isRoot() && !p.isExclude() )
            {
                params = EnrollmentParams.TRUE;
            }
        }
        return params;
    }
}
