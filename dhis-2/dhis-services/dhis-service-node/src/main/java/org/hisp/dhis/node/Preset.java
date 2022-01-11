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
package org.hisp.dhis.node;

import java.util.List;

import com.google.common.collect.Lists;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public enum Preset
{
    ID( "id", Lists.newArrayList( "id" ) ),
    CODE( "code", Lists.newArrayList( "code" ) ),
    ID_NAME( "idName", Lists.newArrayList( "id", "displayName" ) ),
    ALL( "all", Lists.newArrayList( "*" ) ),
    IDENTIFIABLE( "identifiable", Lists.newArrayList( "id", "name", "code", "created", "lastUpdated", "href" ) ),
    NAMEABLE( "nameable",
        Lists.newArrayList( "id", "name", "shortName", "description", "code", "created", "lastUpdated", "href" ) );

    private String name;

    private List<String> fields;

    Preset( String name, List<String> fields )
    {
        this.name = name;
        this.fields = fields;
    }

    public String getName()
    {
        return name;
    }

    public List<String> getFields()
    {
        return fields;
    }

    public static Preset defaultPreset()
    {
        return Preset.ID_NAME;
    }

    public static Preset defaultAssociationPreset()
    {
        return Preset.ID;
    }
}
