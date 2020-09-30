/*
 *  Copyright (c) 2004-2020, University of Oslo
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
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 *  ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.programrule.ProgramRule;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableList;

/**
 * @author Giuseppe Nespolino <g.nespolino@gmail.com>
 */
@Component
public class ProgramRuleObjectBundleHook extends AbstractObjectBundleHook
{

    @Override
    public <T extends IdentifiableObject> List<ErrorReport> validate( T object, ObjectBundle bundle )
    {
        if ( !(object instanceof ProgramRule) )
        {
            return super.validate( object, bundle );
        }

        Session session = sessionFactory.getCurrentSession();
        ProgramRule programRule = (ProgramRule) object;

        Query<ProgramRule> query = session.createQuery(
            " from ProgramRule pr where pr.name = :name and pr.program.uid = :programUid", ProgramRule.class );

        query.setParameter( "name", programRule.getName() );
        query.setParameter( "programUid", programRule.getProgram().getUid() );

        int allowedCount = bundle.getImportMode() == ImportStrategy.UPDATE ? 1 : 0;

        if ( query.getResultList().size() > allowedCount )
        {
            return ImmutableList.of(
                new ErrorReport( ProgramRule.class, ErrorCode.E4031, programRule.getName() ) );
        }

        return super.validate( object, bundle );

    }
}
