/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.analytics.event.data.sql.transform.model.element.where;

public class PredicateElement
{
    private final String leftExpression;

    private final String rightExpression;

    private final String relation;

    private final String logicalOperator;

    public PredicateElement( String leftExpression, String rightExpression, String relation, String logicalOperator )
    {
        this.leftExpression = leftExpression;
        this.rightExpression = rightExpression;
        this.relation = relation;
        this.logicalOperator = logicalOperator;
    }

    public String getLeftExpression()
    {
        return leftExpression;
    }

    public String getRightExpression()
    {
        return rightExpression;
    }

    public String getRelation()
    {
        return relation;
    }

    public String getLogicalOperator()
    {
        return logicalOperator;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( !(o instanceof PredicateElement) )
        {
            return super.equals( o );
        }
        PredicateElement toCompare = (PredicateElement) o;

        if ( leftExpression == null || rightExpression == null || logicalOperator == null || relation == null ||
            toCompare.getLeftExpression() == null || toCompare.getRightExpression() == null ||
            toCompare.getLogicalOperator() == null || toCompare.getRelation() == null )
        {
            return false;
        }
        return leftExpression.equals( toCompare.getLeftExpression() ) &&
            rightExpression.equals( toCompare.getRightExpression() ) &&
            relation.equals( toCompare.getRelation() ) &&
            logicalOperator.equals( toCompare.getLogicalOperator() );
    }

    @Override
    public int hashCode()
    {
        return 1;
    }
}
