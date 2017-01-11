package org.hisp.dhis.query;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import org.hisp.dhis.schema.Schema;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public abstract class Criteria
{
    protected List<Criterion> criterions = new ArrayList<>();

    protected Set<String> aliases = new HashSet<>();

    protected final Schema schema;

    public Criteria( Schema schema )
    {
        this.schema = schema;
    }

    public List<Criterion> getCriterions()
    {
        return criterions;
    }

    public Set<String> getAliases()
    {
        return aliases;
    }

    public Criteria add( Criterion criterion )
    {
        if ( !Restriction.class.isInstance( criterion ) )
        {
            this.criterions.add( criterion ); // if conjunction/disjunction just add it and move forward
            return this;
        }

        Restriction restriction = (Restriction) criterion;

        this.criterions.add( restriction );

        return this;
    }

    public Criteria add( Criterion... criterions )
    {
        for ( Criterion criterion : criterions )
        {
            add( criterion );
        }

        return this;
    }

    public Criteria add( Collection<Criterion> criterions )
    {
        criterions.forEach( this::add );
        return this;
    }
}
