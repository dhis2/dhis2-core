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
package org.hisp.dhis.analytics.data.handler;

/*
 * Copyright (c) 2004-2020, University of Oslo
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
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import static org.hisp.dhis.analytics.OutputFormat.DATA_VALUE_SET;
import static org.hisp.dhis.common.DimensionalObjectUtils.asTypedList;
import static org.hisp.dhis.common.DimensionalObjectUtils.getDataElementOperandIdSchemeMap;
import static org.hisp.dhis.common.DimensionalObjectUtils.getDimensionItemIdSchemeMap;

import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.option.Option;
import org.springframework.stereotype.Component;

/**
 * This component is responsible for encapsulating the id schema mapping for the
 * response elements based on the given URL id schema params.
 *
 * @author maikel arabori
 */
@Component
public class SchemaIdResponseMapper
{
    /**
     * This method will map the respective element UID's with their respective
     * id schema set. The 'outputIdScheme' is considered the most general id
     * schema parameter. If set, it will map the schema id set to all dimension
     * items.
     *
     * The other two id schema parameters supported ('outputDataElementIdScheme'
     * and 'outputOrgUnitIdScheme') will allow fine-grained id schema
     * definitions on top of the general 'outputIdScheme'. If they are set, they
     * will override the 'outputIdScheme' definition.
     *
     * @param params the params where the id schema options are defined. The
     *        current URL params supported are: outputIdScheme,
     *        outputDataElementIdScheme and outputOrgUnitIdScheme.
     * @return a Map of <uid, mapping value>
     */
    public Map<String, String> getSchemeIdResponseMap( final DataQueryParams params )
    {
        final Map<String, String> responseMap = getDimensionItemIdSchemeMap( params.getAllDimensionItems(),
            params.getOutputIdScheme() );

        if ( params.isGeneralOutputIdSchemeSet() )
        {
            // Apply a schema to all data element operands using the general
            // output schema defined.
            applyGeneralIdSchemaMapping( params, responseMap );
        }

        // This section overrides the general schema, so it can be fine-grained.
        if ( params.isOutputFormat( DATA_VALUE_SET ) )
        {
            // If "outputDataElementIdScheme" is set, we replace all data
            // elements values
            // respecting it's definition.
            if ( params.isOutputDataElementIdSchemeSet() )
            {
                if ( !params.getDataElementOperands().isEmpty() )
                {
                    // Replace all data elements operands respecting it's
                    // schema definition.
                    applyDataElementOperandsIdSchemaMapping( params, responseMap );
                }
                else if ( !params.getDataElements().isEmpty() )
                {
                    // Replace all data elements respecting it's schema
                    // definition.
                    applyDataElementsIdSchemaMapping( params, responseMap );
                }
            }
        }

        // If "outputOrgUnitIdScheme" is set, we replace all org units
        // values respecting
        // it's definition.
        if ( params.isOutputOrgUnitIdSchemeSet() )
        {
            applyOrgUnitIdSchemaMapping( params, responseMap );
        }

        return responseMap;
    }

    private void applyGeneralIdSchemaMapping( final DataQueryParams params, final Map<String, String> map )
    {
        map.putAll( getDataElementOperandIdSchemeMap(
            asTypedList( params.getDataElementOperands() ), params.getOutputIdScheme() ) );

        if ( params.getProgramStage() != null )
        {
            map.put( params.getProgramStage().getUid(),
                params.getProgramStage().getPropertyValue( params.getOutputIdScheme() ) );
        }

        if ( params.getProgram() != null )
        {
            map.put( params.getProgram().getUid(), params.getProgram().getPropertyValue( params.getOutputIdScheme() ) );
        }

        if ( params instanceof EventQueryParams
            && CollectionUtils.isNotEmpty( ((EventQueryParams) params).getItemOptions() ) )
        {
            final Set<Option> options = ((EventQueryParams) params).getItemOptions();

            for ( final Option option : options )
            {
                map.put( option.getCode(), option.getPropertyValue( params.getOutputIdScheme() ) );
            }
        }
    }

    private void applyDataElementOperandsIdSchemaMapping( final DataQueryParams params, final Map<String, String> map )
    {
        map.putAll( getDataElementOperandIdSchemeMap( asTypedList( params.getDataElementOperands() ),
            params.getOutputDataElementIdScheme() ) );
    }

    private void applyDataElementsIdSchemaMapping( final DataQueryParams params, final Map<String, String> map )
    {
        map.putAll( getDimensionItemIdSchemeMap( asTypedList( params.getDataElements() ),
            params.getOutputDataElementIdScheme() ) );
    }

    private void applyOrgUnitIdSchemaMapping( final DataQueryParams params, final Map<String, String> map )
    {
        map.putAll( getDimensionItemIdSchemeMap( asTypedList( params.getOrganisationUnits() ),
            params.getOutputOrgUnitIdScheme() ) );
    }
}
