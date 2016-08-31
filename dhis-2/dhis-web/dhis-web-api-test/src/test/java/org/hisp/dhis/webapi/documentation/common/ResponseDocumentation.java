package org.hisp.dhis.webapi.documentation.common;

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

import com.google.common.collect.Lists;
import org.springframework.restdocs.payload.FieldDescriptor;

import java.util.List;

import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public final class ResponseDocumentation
{
    public static List<FieldDescriptor> identifiableObject()
    {
        return Lists.newArrayList(
            fieldWithPath( "id" ).description( "Identifier" ),
            fieldWithPath( "name" ).description( "Name" ),
            fieldWithPath( "displayName" ).description( "Property" ),
            fieldWithPath( "code" ).description( "Code" ),
            fieldWithPath( "created" ).description( "Property" ),
            fieldWithPath( "lastUpdated" ).description( "Property" ),
            fieldWithPath( "href" ).description( "Property" ),
            fieldWithPath( "publicAccess" ).description( "Property" ),
            fieldWithPath( "externalAccess" ).description( "Property" ),
            fieldWithPath( "access" ).description( "Property" ),
            fieldWithPath( "userGroupAccesses" ).description( "Property" ),
            fieldWithPath( "attributeValues" ).description( "Property" ),
            fieldWithPath( "translations" ).description( "Property" )
        );
    }

    public static List<FieldDescriptor> nameableObject()
    {
        return Lists.newArrayList(
            fieldWithPath( "shortName" ).description( "Property" ),
            fieldWithPath( "displayShortName" ).description( "Property" ),
            fieldWithPath( "description" ).description( "Property" ),
            fieldWithPath( "displayDescription" ).description( "Property" )
        );
    }

    public static List<FieldDescriptor> pager()
    {
        return Lists.newArrayList(
            fieldWithPath( "pager.page" ).description( "Property" ),
            fieldWithPath( "pager.pageCount" ).description( "Property" ),
            fieldWithPath( "pager.total" ).description( "Property" ),
            fieldWithPath( "pager.pageSize" ).description( "Property" )
        );
    }
}
