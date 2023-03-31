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
package org.hisp.dhis.analytics.data.handler;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.hisp.dhis.analytics.OutputFormat.DATA_VALUE_SET;
import static org.hisp.dhis.common.DimensionalObjectUtils.asTypedList;
import static org.hisp.dhis.common.DimensionalObjectUtils.getDataElementOperandIdSchemeMap;
import static org.hisp.dhis.common.DimensionalObjectUtils.getDimensionItemIdSchemeMap;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.common.params.CommonParams;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.springframework.stereotype.Component;

/**
 * This component is responsible for providing methods that encapsulate the id
 * schema mapping for response elements based on given URL ID schemes.
 *
 * @author maikel arabori
 */
@Component
public class SchemeIdResponseMapper
{
    /**
     * This method will map the respective element UID's with their respective
     * ID scheme set. The 'outputIdScheme' is considered the most general ID
     * scheme parameter. If set, it will map the scheme id set to all dimension
     * items.
     *
     * The other two ID scheme parameters supported ('outputDataElementIdScheme'
     * and 'outputOrgUnitIdScheme') will allow fine-grained id scheme
     * definitions on top of the general 'outputIdScheme'. If they are set, they
     * will override the 'outputIdScheme' definition.
     *
     * @param params the {@link DataQueryParams} where the identifier scheme
     *        options are defined. The supported URL parameters are
     *        outputIdScheme, outputDataElementIdScheme and
     *        outputOrgUnitIdScheme.
     * @return a map of UID and mapping value.
     */
    public Map<String, String> getSchemeIdResponseMap( DataQueryParams params )
    {
        Map<String, String> responseMap = getDimensionItemIdSchemeMap( params.getAllDimensionItems(),
            params.getOutputIdScheme() );

        if ( params.isGeneralOutputIdSchemeSet() )
        {
            // Apply an ID scheme to all data element operands using the general
            // output ID scheme defined.
            applyIdSchemeMapping( params, responseMap );
        }

        // This section overrides the general ID scheme, so it can be
        // fine-grained.
        if ( params.isOutputFormat( DATA_VALUE_SET ) && params.isOutputDataElementIdSchemeSet() )
        {
            if ( !params.getDataElementOperands().isEmpty() )
            {
                // Replace all data elements operands respecting their ID
                // scheme definition.
                applyDataElementOperandIdSchemeMapping( params, responseMap );
            }
            else if ( !params.getDataElements().isEmpty() )
            {
                // Replace all data elements respecting their ID scheme
                // definition.
                applyDataElementsIdSchemeMapping( params.getDataElements(), responseMap,
                    params.getOutputDataElementIdScheme() );
            }
        }

        // If "outputOrgUnitIdScheme" is set, we replace all org units
        // values respecting its definition.
        if ( params.isOutputOrgUnitIdSchemeSet() )
        {
            applyOrgUnitIdSchemeMapping( params.getOrganisationUnits(), responseMap,
                params.getOutputOrgUnitIdScheme() );
        }

        return responseMap;
    }

    /**
     * This method will map the respective element UID's with their respective
     * ID scheme set. The 'outputIdScheme' is considered the most general ID
     * scheme parameter. If set, it will map the scheme id set to all dimension
     * items.
     *
     * The other two ID scheme parameters supported ('outputDataElementIdScheme'
     * and 'outputOrgUnitIdScheme') will allow fine-grained id scheme
     * definitions on top of the general 'outputIdScheme'. If they are set, they
     * will override the 'outputIdScheme' definition.
     *
     * @param params the {@link CommonParams} where the identifier scheme
     *        options are defined. The supported URL parameters are
     *        outputIdScheme, outputDataElementIdScheme and
     *        outputOrgUnitIdScheme.
     * @return a map of UID and mapping value.
     */
    public Map<String, String> getSchemeIdResponseMap( CommonParams params )
    {
        Map<String, String> responseMap = getDimensionItemIdSchemeMap( params.delegate().getAllDimensionalItemObjects(),
            params.getOutputIdScheme() );

        if ( params.isGeneralOutputIdSchemeSet() )
        {
            // Apply an ID scheme to all data element operands using the general output ID scheme defined.
            applyIdSchemeMapping( params, responseMap );
        }

        List<DimensionalItemObject> dataElements = params.delegate().getAllDataElements();

        if ( isNotEmpty( dataElements ) )
        {
            // Replace all data elements respecting their ID scheme definition.
            applyDataElementsIdSchemeMapping( dataElements, responseMap, params.getOutputDataElementIdScheme() );
        }

        List<DimensionalItemObject> orgUnits = params.delegate().getOrgUnitDimensionOrFilterItems();

        // If "outputOrgUnitIdScheme" is set, we replace all org units values respecting its definition.
        if ( params.isOutputOrgUnitIdSchemeSet() && isNotEmpty( orgUnits ) )
        {
            applyOrgUnitIdSchemeMapping( orgUnits, responseMap, params.getOutputOrgUnitIdScheme() );
        }

        return responseMap;
    }

    /**
     * Substitutes the metadata of the grid with the identifier scheme metadata
     * property indicated in the query. This happens only when a custom
     * identifier scheme is specified.
     *
     * @param params the {@link EventQueryParams}.
     * @param grid the {@link Grid}.
     */
    public void applyCustomIdScheme( EventQueryParams params, Grid grid )
    {
        if ( !params.isSkipMeta() && params.hasCustomIdSchemaSet() )
        {
            grid.substituteMetaData( getSchemeIdResponseMap( params ) );
        }
    }

    /**
     * Substitutes the metadata of the grid with the identifier scheme metadata
     * property indicated in the query. This happens only when a custom
     * identifier scheme is specified.
     *
     * @param params the {@link CommonParams}.
     * @param grid the {@link Grid}.
     */
    public void applyCustomIdScheme( CommonParams params, Grid grid )
    {
        if ( !params.isSkipMeta() && params.hasCustomIdSchemaSet() )
        {
            grid.substituteMetaData( getSchemeIdResponseMap( params ) );
        }
    }

    /**
     * Substitutes the metadata in the given grid. The replacement will only be
     * done if the grid header has option set or legend set.
     *
     * @param grid the {@link Grid}.
     * @param idScheme the {@link IdScheme}.
     */
    public void applyOptionAndLegendSetMapping( Grid grid, IdScheme idScheme )
    {
        if ( idScheme != null )
        {
            for ( int i = 0; i < grid.getHeaders().size(); i++ )
            {
                GridHeader header = grid.getHeaders().get( i );

                if ( header.hasOptionSet() )
                {
                    Map<String, String> optionMap = header.getOptionSetObject().getOptionCodePropertyMap( idScheme );
                    grid.substituteMetaData( i, i, optionMap );
                }
                else if ( header.hasLegendSet() )
                {
                    Map<String, String> legendMap = header.getLegendSetObject().getLegendUidPropertyMap( idScheme );
                    grid.substituteMetaData( i, i, legendMap );
                }
            }
        }
    }

    /**
     * Based on the given parameters and map, this method will, internally, add
     * elements into the map based on its own uid. Each element added will have
     * its associated value based on the current "outputIdScheme".
     *
     * @param params the {@link CommonParams}.
     * @param map to be populated with elements to be replaced.
     */
    private void applyIdSchemeMapping( CommonParams params, Map<String, String> map )
    {
        if ( isNotEmpty( params.getPrograms() ) )
        {
            for ( Program program : params.getPrograms() )
            {
                map.put( program.getUid(), program.getPropertyValue( params.getOutputIdScheme() ) );
            }
        }

        if ( isNotEmpty( params.delegate().getProgramStages() ) )
        {
            for ( ProgramStage stage : params.delegate().getProgramStages() )
            {
                map.put( stage.getUid(), stage.getPropertyValue( params.getOutputIdScheme() ) );
            }
        }

        if ( isNotEmpty( params.delegate().getItemsOptions() ) )
        {
            Set<Option> options = params.delegate().getItemsOptions();

            for ( Option option : options )
            {
                map.put( option.getCode(), option.getPropertyValue( params.getOutputIdScheme() ) );
            }
        }
    }

    /**
     * Based on the given parameters and map, this method will, internally, add
     * elements into the map based on its own uid. Each element added will have
     * its associated value based on the current "outputIdScheme".
     *
     * @param params the {@link DataQueryParams}.
     * @param map to be populated with elements to be replaced.
     */
    private void applyIdSchemeMapping( DataQueryParams params, Map<String, String> map )
    {
        map.putAll( getDataElementOperandIdSchemeMap(
            asTypedList( params.getDataElementOperands() ), params.getOutputIdScheme() ) );

        if ( params.hasProgramStage() )
        {
            map.put( params.getProgramStage().getUid(),
                params.getProgramStage().getPropertyValue( params.getOutputIdScheme() ) );
        }

        if ( params.hasProgram() )
        {
            map.put( params.getProgram().getUid(), params.getProgram().getPropertyValue( params.getOutputIdScheme() ) );
        }

        if ( params instanceof EventQueryParams
            && isNotEmpty( ((EventQueryParams) params).getItemOptions() ) )
        {
            Set<Option> options = ((EventQueryParams) params).getItemOptions();

            for ( Option option : options )
            {
                map.put( option.getCode(), option.getPropertyValue( params.getOutputIdScheme() ) );
            }
        }
    }

    private void applyDataElementOperandIdSchemeMapping( DataQueryParams params, Map<String, String> map )
    {
        map.putAll( getDataElementOperandIdSchemeMap( asTypedList( params.getDataElementOperands() ),
            params.getOutputDataElementIdScheme() ) );
    }

    private void applyDataElementsIdSchemeMapping( List<DimensionalItemObject> dataElements, Map<String, String> map,
        IdScheme outputDataElementIdScheme )
    {
        map.putAll( getDimensionItemIdSchemeMap( asTypedList( dataElements ), outputDataElementIdScheme ) );
    }

    private void applyOrgUnitIdSchemeMapping( List<DimensionalItemObject> orgUnits, Map<String, String> map,
        IdScheme outputOrgUnitIdScheme )
    {
        map.putAll( getDimensionItemIdSchemeMap( asTypedList( orgUnits ), outputOrgUnitIdScheme ) );
    }
}
