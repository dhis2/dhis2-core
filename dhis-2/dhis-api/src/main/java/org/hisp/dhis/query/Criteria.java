package org.hisp.dhis.query;

/*
 * Copyright (c) 2004-2015, University of Oslo
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
import java.util.List;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public abstract class Criteria
{
    protected List<Criterion> criterions = new ArrayList<>();

    protected final Schema schema;

    public Criteria( Schema schema )
    {
        this.schema = schema;
    }

    public List<Criterion> getCriterions()
    {
        return criterions;
    }

    public Criteria add( Criterion... criterions )
    {
        for ( Criterion criterion : criterions )
        {
            if ( !Restriction.class.isInstance( criterion ) )
            {
                this.criterions.add( criterion ); // if conjunction/disjunction just add it and move forward
                continue;
            }

            Restriction restriction = (Restriction) criterion;

            if ( !schema.haveProperty( restriction.getPath() ) )
            {
                continue;
            }

            if ( (restriction.getOperator().getMax() != null && restriction.getParameters().size() > restriction.getOperator().getMax())
                || (restriction.getOperator().getMin() != null && restriction.getParameters().size() < restriction.getOperator().getMin()) )
            {
                continue;
            }

            this.criterions.add( restriction );
        }

        return this;
    }

    public Criteria add( Collection<Criterion> criterions )
    {
        this.criterions.addAll( criterions );
        return this;
    }
}
