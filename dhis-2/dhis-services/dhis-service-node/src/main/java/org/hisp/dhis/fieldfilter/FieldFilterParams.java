package org.hisp.dhis.fieldfilter;

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

import org.hisp.dhis.user.User;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public final class FieldFilterParams
{
    private User user;

    /**
     * List of object(s) to filter through. If more than one, a wrapper
     * is required.
     */
    private List<?> objects = new ArrayList<>();

    /**
     * Fields to filter by.
     */
    private List<String> fields;

    private Defaults defaults = Defaults.EXCLUDE;

    public FieldFilterParams( List<?> objects, List<String> fields )
    {
        this.objects = objects;
        this.fields = fields;
    }

    public FieldFilterParams( List<?> objects, List<String> fields, Defaults defaults )
    {
        this.objects = objects;
        this.fields = fields;
        this.defaults = defaults;
    }

    public User getUser()
    {
        return user;
    }

    public FieldFilterParams setUser( User user )
    {
        this.user = user;
        return this;
    }

    public List<?> getObjects()
    {
        return objects;
    }

    public FieldFilterParams setObjects( List<?> objects )
    {
        this.objects = objects;
        return this;
    }

    public List<String> getFields()
    {
        return fields;
    }

    public FieldFilterParams setFields( List<String> fields )
    {
        this.fields = fields;
        return this;
    }

    public Defaults getDefaults()
    {
        return defaults;
    }

    public FieldFilterParams setDefaults( Defaults defaults )
    {
        this.defaults = defaults;
        return this;
    }
}
