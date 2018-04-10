package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.Objects;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.textpattern.TextPattern;
import org.hisp.dhis.textpattern.TextPatternParser;
import org.hisp.dhis.textpattern.TextPatternValidationUtils;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;

import java.util.ArrayList;
import java.util.List;

public class TrackedEntityAttributeObjectBundleHook
    extends AbstractObjectBundleHook
{
    @Override
    public <T extends IdentifiableObject> List<ErrorReport> validate( T object, ObjectBundle bundle )
    {
        List<ErrorReport> errorReports = new ArrayList<>();

        /*
         * Validate that the RenderType (if any) conforms to the constraints of ValueType or OptionSet.
         */
        if ( object != null && object.getClass().isAssignableFrom( TrackedEntityAttribute.class ) )
        {
            TrackedEntityAttribute attr = (TrackedEntityAttribute) object;

            errorReports.addAll( textPatternValid( attr ) );

        }

        return errorReports;
    }

    @Override
    public <T extends IdentifiableObject> void postCreate( T persistedObject, ObjectBundle bundle )
    {
        if ( persistedObject != null && persistedObject.getClass().isAssignableFrom( TrackedEntityAttribute.class ) )
        {
            updateTextPattern( (TrackedEntityAttribute) persistedObject );
        }
    }

    @Override
    public <T extends IdentifiableObject> void postUpdate( T persistedObject, ObjectBundle bundle )
    {
        if ( persistedObject != null && persistedObject.getClass().isAssignableFrom( TrackedEntityAttribute.class ) )
        {
            updateTextPattern( (TrackedEntityAttribute) persistedObject );
        }
    }

    private void updateTextPattern( TrackedEntityAttribute attr )
    {
        if ( attr.isGenerated() )
        {
            try
            {
                TextPattern textPattern = TextPatternParser.parse( attr.getPattern() );
                textPattern.setOwnerObject( Objects.TRACKEDENTITYATTRIBUTE );
                textPattern.setOwnerUid( attr.getUid() );
                attr.setTextPattern( textPattern );
            }
            catch ( TextPatternParser.TextPatternParsingException e )
            {
                e.printStackTrace();
            }
        }
    }

    private List<ErrorReport> textPatternValid( TrackedEntityAttribute attr )
    {
        List<ErrorReport> errorReports = new ArrayList<>();

        if ( attr.isGenerated() )
        {
            try
            {
                TextPattern tp = TextPatternParser.parse( attr.getPattern() );

                long generatedSegments = tp.getSegments().stream().filter( ( s ) -> s.getMethod().isGenerated() )
                    .count();

                if ( generatedSegments != 1 )
                {
                    errorReports.add( new ErrorReport( TrackedEntityAttribute.class, ErrorCode.E4021 ) );
                }

                if ( !TextPatternValidationUtils.validateValueType( tp, attr.getValueType() ) )
                {
                    errorReports.add( new ErrorReport( TrackedEntityAttribute.class, ErrorCode.E4022, attr.getPattern(),
                        attr.getValueType().name() ) );
                }
            }
            catch ( TextPatternParser.TextPatternParsingException e )
            {
                errorReports.add( new ErrorReport( TrackedEntityAttribute.class, ErrorCode.E4019, attr.getPattern(),
                    e.getMessage() ) );
            }

        }

        return errorReports;
    }
}
