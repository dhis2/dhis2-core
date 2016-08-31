package org.hisp.dhis.system.util;

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

import net.sf.json.JSONObject;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeService;
import org.hisp.dhis.attribute.AttributeValue;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author mortenoh
 */
public class AttributeUtils
{
    /**
     * Given a list of JSON formatted values (with keys: 'id' and 'value'), this
     * method will add/update {@link AttributeValue} into the given {@code Set}.
     * 
     * @param jsonAttributeValues List of JSON formatted values, needs two keys:
     *        id => ID of attribute this value belongs to value => Actual value
     * @param attributeValues Set that will be updated
     * @param attributeService
     */
    public static void updateAttributeValuesFromJson( Set<AttributeValue> attributeValues,
        List<String> jsonAttributeValues, AttributeService attributeService )
    {
        attributeValues.clear();

        for ( String jsonAttributeValue : jsonAttributeValues )
        {
            JSONObject json = JSONObject.fromObject( jsonAttributeValue );

            AttributeValue attributeValue = new AttributeValue();
            attributeValue.setId( json.getInt( "id" ) );
            attributeValue.setValue( json.getString( "value" ) );

            Attribute attribute = attributeService.getAttribute( attributeValue.getId() );

            if ( attribute == null )
            {
                continue;
            }

            attributeValue.setAttribute( attribute );

            for ( AttributeValue attributeValueItem : attributeValues )
            {
                if ( attributeValueItem.getAttribute().getId() == attribute.getId() )
                {
                    if ( attributeValue == null || StringUtils.isEmpty( attributeValue.getValue() ) )
                    {
                        attributeService.deleteAttributeValue( attributeValueItem );
                    }
                    else
                    {
                        attributeValueItem.setValue( attributeValue.getValue() );
                        attributeService.updateAttributeValue( attributeValueItem );
                        attributeValue = null;
                    }
                }
            }

            if ( attributeValue != null && attributeValue.getValue() != null && !attributeValue.getValue().isEmpty())
            {
                attributeService.addAttributeValue( attributeValue );
                attributeValues.add( attributeValue );
            }
        }
    }

    /**
     * @param attributeValues
     * @return Map of <AttributeId, ValueString>
     */
    public static Map<Integer, String> getAttributeValueMap( Set<AttributeValue> attributeValues )
    {
        Map<Integer, String> attributeValuesMap = new HashMap<>();

        for ( AttributeValue attributeValue : attributeValues )
        {
            attributeValuesMap.put( attributeValue.getAttribute().getId(), attributeValue.getValue() );
        }

        return attributeValuesMap;
    }
}
