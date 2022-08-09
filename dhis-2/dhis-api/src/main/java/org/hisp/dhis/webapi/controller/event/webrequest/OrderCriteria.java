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
package org.hisp.dhis.webapi.controller.event.webrequest;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.webapi.controller.event.mapper.OrderParam;
import org.hisp.dhis.webapi.controller.event.mapper.SortDirection;

/**
 * This class is used as a container for order parameters and is deserialized
 * from web requests
 *
 * @author Giuseppe Nespolino <g.nespolino@gmail.com>
 */
@Data
@AllArgsConstructor( staticName = "of" )
@NoArgsConstructor
@With
public class OrderCriteria
{
    private String field;

    private SortDirection direction;

    public OrderParam toOrderParam()
    {
        return OrderParam.builder()
            .field( field )
            .direction( direction )
            .build();
    }

    public static List<OrderCriteria> fromOrderString( String source )
    {
        return Optional.of( source )
            .filter( StringUtils::isNotBlank )
            .map( String::trim )
            .map( OrderCriteria::toOrderCriterias )
            .orElse( Collections.emptyList() );
    }

    private static List<OrderCriteria> toOrderCriterias( String s )
    {
        return Arrays.stream( s.split( "," ) )
            .map( OrderCriteria::toOrderCriteria )
            .collect( Collectors.toList() );
    }

    private static OrderCriteria toOrderCriteria( String s1 )
    {
        String[] props = s1.split( ":" );
        if ( props.length == 2 )
        {
            return OrderCriteria.of( props[0], SortDirection.of( props[1] ) );
        }
        if ( props.length == 1 )
        {
            return OrderCriteria.of( props[0], SortDirection.ASC );
        }
        return null;
    }

}
