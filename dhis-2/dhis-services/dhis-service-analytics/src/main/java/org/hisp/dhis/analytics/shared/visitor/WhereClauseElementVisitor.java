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
package org.hisp.dhis.analytics.shared.visitor;

import java.util.List;

import org.hisp.dhis.analytics.shared.component.element.where.EnrollmentDateValueElement;
import org.hisp.dhis.analytics.shared.component.element.where.TeavValueElement;

/**
 * @see SelectElementVisitor
 *
 * @author dusan bernat
 */
public class WhereClauseElementVisitor implements WhereElementVisitor
{
    private final List<String> predicates;

    public WhereClauseElementVisitor( List<String> predicates )
    {
        if ( predicates == null )
        {
            throw new IllegalArgumentException( "predicates" );
        }
        this.predicates = predicates;
    }

    @Override
    public void visit( TeavValueElement element )
    {
        predicates.add( "(SELECT teav.VALUE" +
            "       FROM trackedentityattributevalue teav," +
            "            trackedentityattribute tea" +
            "       WHERE teav.trackedentityinstanceid = t.trackedentityinstanceid" +
            "         AND teav.trackedentityattributeid = tea.trackedentityattributeid" +
            "         AND tea.uid = '" + element.getUid() + "'" +
            "       LIMIT 1) = " + "'" + element.getFilterValue() + "'" );
    }

    @Override
    public void visit( EnrollmentDateValueElement element )
    {
        predicates.add( "exists(" +
            "        SELECT 1" +
            "        FROM programinstance pi," +
            "             program p" +
            "        WHERE pi.programid = p.programid" +
            "          AND pi.trackedentityinstanceid = t.trackedentityinstanceid" +
            "          AND p.uid IN ('" + String.join( "','", element.getProgramUidList() ) + "')" +
            "          AND pi.enrollmentdate > '" + element.getDate() + "'" +
            "    )" );
    }
}
