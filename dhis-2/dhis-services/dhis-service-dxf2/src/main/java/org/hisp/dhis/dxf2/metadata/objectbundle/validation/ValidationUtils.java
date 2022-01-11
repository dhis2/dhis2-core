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
package org.hisp.dhis.dxf2.metadata.objectbundle.validation;

import static java.util.Collections.singletonList;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.feedback.ObjectReport;

/**
 * @author Luciano Fiandesio
 */
public class ValidationUtils
{
    private ValidationUtils()
    {
        throw new UnsupportedOperationException( "util" );
    }

    public static <T> List<T> joinObjects( List<T> persistedObjects,
        List<T> nonPersistedObjects )
    {
        if ( persistedObjects.isEmpty() )
        {
            return nonPersistedObjects;
        }
        if ( nonPersistedObjects.isEmpty() )
        {
            return persistedObjects;
        }
        return Stream.concat( persistedObjects.stream(), nonPersistedObjects.stream() ).collect( Collectors.toList() );
    }

    public static ObjectReport createObjectReport( List<ErrorReport> reports, IdentifiableObject object,
        ObjectBundle bundle )
    {
        ObjectReport objectReport = new ObjectReport( object, bundle );
        objectReport.setDisplayName( IdentifiableObjectUtils.getDisplayName( object ) );
        objectReport.addErrorReports( reports );
        return objectReport;
    }

    public static ObjectReport createObjectReport( ErrorReport report, IdentifiableObject object, ObjectBundle bundle )
    {
        return createObjectReport( singletonList( report ), object, bundle );
    }
}
