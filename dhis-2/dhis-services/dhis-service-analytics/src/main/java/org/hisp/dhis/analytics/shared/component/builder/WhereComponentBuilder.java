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
package org.hisp.dhis.analytics.shared.component.builder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hisp.dhis.analytics.shared.component.WhereComponent;
import org.hisp.dhis.analytics.shared.component.element.Element;
import org.hisp.dhis.analytics.shared.component.element.where.EnrollmentDateValueWhereElement;
import org.hisp.dhis.analytics.shared.component.element.where.TeaValueWhereElement;
import org.hisp.dhis.analytics.shared.visitor.where.WhereVisitor;
import org.hisp.dhis.analytics.tei.TeiParams;

/**
 * WhereComponentBuilder is responsible for building the from section of sql
 * query
 *
 * @author dusan bernat
 */
public class WhereComponentBuilder
{
    private TeiParams teiParams;

    /**
     * Instance
     *
     * @return
     */
    public static WhereComponentBuilder builder()
    {
        return new WhereComponentBuilder();
    }

    /**
     * with method of builder
     *
     * @param teiParams
     * @return
     */
    public WhereComponentBuilder withTeiParams( TeiParams teiParams )
    {
        this.teiParams = teiParams;

        return this;
    }

    /**
     * Instance of component all element has to be included here
     *
     * @return
     */
    public WhereComponent build()
    {
        Map<String, String> inputUidMap = new HashMap<>();
        inputUidMap.put( "w75KJ2mc4zz", "John" );
        inputUidMap.put( "zDhUuAYrxNC", "Kelly" );

        List<Element<WhereVisitor>> elements = inputUidMap
            .keySet()
            .stream()
            .map( k -> new TeaValueWhereElement( k, inputUidMap.get( k ) ) ).collect( Collectors.toList() );

        elements.add( new EnrollmentDateValueWhereElement( List.of( "ur1Edk5Oe2n", "IpHINAT79UW" ), "2022-01-01" ) );

        return new WhereComponent( elements );
    }

}
