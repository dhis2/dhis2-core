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
package org.hisp.dhis.query;

import javax.persistence.criteria.Predicate;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import org.hisp.dhis.query.operators.Operator;
import org.hisp.dhis.query.planner.QueryPath;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Getter
@Accessors( chain = true )
public final class Restriction implements Criterion
{
    /**
     * Path to property you want to restrict only, one first-level properties
     * are currently supported.
     */
    private final String path;

    /**
     * Operator for restriction.
     */
    private final Operator<?> operator;

    /**
     * Query Path.
     */
    @Setter
    private QueryPath queryPath;

    @Setter
    private Predicate predicate;

    public Restriction( String path, Predicate predicate )
    {
        this.path = path;
        this.operator = null;
        this.predicate = predicate;
    }

    public Restriction( String path, Operator operator )
    {
        this.path = path;
        this.operator = operator;
    }

    @Override
    public String toString()
    {
        return "[" + path + ", op: " + operator + "]";
    }
}
