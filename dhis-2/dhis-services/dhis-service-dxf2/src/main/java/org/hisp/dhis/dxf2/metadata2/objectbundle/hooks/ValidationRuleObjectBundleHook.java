package org.hisp.dhis.dxf2.metadata2.objectbundle.hooks;

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

import org.hibernate.Session;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dxf2.metadata2.objectbundle.ObjectBundle;
import org.hisp.dhis.expression.Expression;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.validation.ValidationRule;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class ValidationRuleObjectBundleHook
    extends AbstractObjectBundleHook
{
    @Override
    public void preCreate( IdentifiableObject object, ObjectBundle bundle )
    {
        if ( !ValidationRule.class.isInstance( object ) )
        {
            return;
        }

        ValidationRule validationRule = (ValidationRule) object;
        Session session = sessionFactory.getCurrentSession();

        connectExpression( bundle, validationRule.getLeftSide() );
        connectExpression( bundle, validationRule.getRightSide() );

        Expression skipTest = validationRule.getSampleSkipTest();

        if ( skipTest != null )
        {
            preheatService.connectReferences( skipTest, bundle.getPreheat(), bundle.getPreheatIdentifier() );
        }

        session.save( validationRule.getLeftSide() );
        session.save( validationRule.getRightSide() );

        if ( skipTest != null )
        {
            sessionFactory.getCurrentSession().save( skipTest );
        }

        if ( validationRule.getPeriodType() != null )
        {
            PeriodType periodType = bundle.getPreheat().getPeriodTypeMap()
                .get( validationRule.getPeriodType().getName() );
            validationRule.setPeriodType( periodType );
        }
    }

    @Override
    public void preUpdate( IdentifiableObject object, IdentifiableObject persistedObject, ObjectBundle bundle )
    {
        if ( !ValidationRule.class.isInstance( object ) )
        {
            return;
        }

        ValidationRule validationRule = (ValidationRule) object;
        Session session = sessionFactory.getCurrentSession();

        connectExpression( bundle, validationRule.getLeftSide() );
        connectExpression( bundle, validationRule.getRightSide() );

        Expression skipTest = validationRule.getSampleSkipTest();

        if ( skipTest != null )
        {
            preheatService.connectReferences( skipTest, bundle.getPreheat(), bundle.getPreheatIdentifier() );
        }

        session.save( validationRule.getLeftSide() );
        session.save( validationRule.getRightSide() );

        if ( skipTest != null )
        {
            sessionFactory.getCurrentSession().save( skipTest );
        }

        if ( validationRule.getPeriodType() != null )
        {
            PeriodType periodType = bundle.getPreheat().getPeriodTypeMap()
                .get( validationRule.getPeriodType().getName() );
            validationRule.setPeriodType( periodType );
        }
    }

    private void connectExpression( ObjectBundle bundle, Expression expression )
    {
        Set<DataElement> dataElementsInExpression = new HashSet<>();
        Set<DataElement> sampleElementsInExpression = new HashSet<>();

        expression.getDataElementsInExpression().forEach( de -> dataElementsInExpression.add( bundle.getPreheat().get( bundle.getPreheatIdentifier(), de ) ) );
        expression.getSampleElementsInExpression().forEach( de -> dataElementsInExpression.add( bundle.getPreheat().get( bundle.getPreheatIdentifier(), de ) ) );

        expression.setDataElementsInExpression( dataElementsInExpression );
        expression.setSampleElementsInExpression( sampleElementsInExpression );
    }
}
