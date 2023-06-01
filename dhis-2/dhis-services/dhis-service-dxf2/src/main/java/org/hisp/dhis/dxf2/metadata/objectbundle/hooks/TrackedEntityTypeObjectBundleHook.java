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
package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.preheat.PreheatIdentifier;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.springframework.stereotype.Component;

/**
 * Validation hook for embedded TrackEntityAttribute in TrackedEntityType. If
 * TrackEntityAttributes are associated, then we check if they exist in Preheat
 * by identifier otherwise we report error
 *
 * @author Luca Cambi <luca@dhis2.org>
 */
@Component( "org.hisp.dhis.dxf2.metadata.objectbundle.hooks.TrackedEntityTypeObjectBundleHook" )
public class TrackedEntityTypeObjectBundleHook extends AbstractObjectBundleHook<TrackedEntityType>
{
    @Override
    public void validate( TrackedEntityType object, ObjectBundle bundle, Consumer<ErrorReport> addReports )
    {
        List<TrackedEntityAttribute> attributes = object.getTrackedEntityAttributes();
        if ( attributes != null && !attributes.isEmpty() )
        {
            attributes.stream().filter( Objects::nonNull ).forEach( tea -> {
                PreheatIdentifier preheatIdentifier = bundle.getPreheatIdentifier();

                if ( bundle.getPreheat().get( preheatIdentifier, tea ) == null )
                {
                    addReports.accept(
                        new ErrorReport( TrackedEntityAttribute.class, ErrorCode.E5001, preheatIdentifier,
                            preheatIdentifier.getIdentifiersWithName( tea ) ).setErrorProperty( "id" )
                            .setMainId( tea.getUid() ) );
                }
            } );
        }
    }
}
