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
package org.hisp.dhis.analytics.event.data;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.legend.Legend;
import org.hisp.dhis.option.Option;

/**
 * @author Dusan Bernat
 */
public class QueryItemHelper
{
    private static final String ITEM_NAME_SEP = ": ";

    private static final String NA = "[N/A]";

    /**
     * Returns an item value (legend) for OutputIdScheme (Code, Name, Id, Uid).
     *
     * @param itemValue the item value.
     * @param params the {@link EventQueryParams}.
     */
    public static String getItemOptionValue( String itemValue, EventQueryParams params )
    {
        Optional<Option> itemOption = params.getItemOptions().stream()
            .filter( option -> option.getDisplayName().equalsIgnoreCase( itemValue ) )
            .findFirst();

        return itemOption.map( option -> params.getOutputIdScheme() == IdScheme.UID ? option.getUid()
            : params.getOutputIdScheme() == IdScheme.CODE ? option.getCode()
                : params.getOutputIdScheme() == IdScheme.NAME ? option.getName()
                    : Long.toString( option.getId() ) )
            .orElse( null );
    }

    /**
     * Returns an item value (option) for OutputIdScheme (Code, Name, Id, Uid).
     *
     * @param itemValue the item value.
     * @param params the {@link EventQueryParams}.
     */
    public static String getItemLegendValue( String itemValue, EventQueryParams params )
    {
        Optional<Legend> itemLegend = params.getItemLegends().stream()
            .filter( legend -> legend.getDisplayName().equalsIgnoreCase( itemValue ) )
            .findFirst();

        return itemLegend.map( legend -> params.getOutputIdScheme() == IdScheme.UID ? legend.getUid()
            : params.getOutputIdScheme() == IdScheme.CODE ? legend.getCode()
                : params.getOutputIdScheme() == IdScheme.NAME ? legend.getName()
                    : Long.toString( legend.getId() ) )
            .orElse( null );
    }

    /**
     * Returns an item value for the given query, query item and value. Assumes
     * that data dimensions are collapsed for the given query. Returns the short
     * name of the given query item followed by the item value. If the given
     * query item has a legend set, the item value is treated as an id and
     * substituted with the matching legend name. If the given query item has an
     * option set, the item value is treated as a code and substituted with the
     * matching option name.
     *
     * @param item the {@link QueryItem}.
     * @param itemValue the item value.
     */
    public static String getCollapsedDataItemValue( QueryItem item, String itemValue )
    {
        String value = item.getItem().getDisplayShortName() + ITEM_NAME_SEP;

        Legend legend;
        Option option;

        if ( item.hasLegendSet() && (legend = item.getLegendSet().getLegendByUid( itemValue )) != null )
        {
            return value + legend.getDisplayName();
        }
        else if ( item.hasOptionSet() && (option = item.getOptionSet().getOptionByCode( itemValue )) != null )
        {
            return value + option.getDisplayName();
        }
        else
        {
            itemValue = StringUtils.defaultString( itemValue, NA );

            return value + itemValue;
        }
    }
}
