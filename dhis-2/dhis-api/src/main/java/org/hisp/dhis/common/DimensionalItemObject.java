package org.hisp.dhis.common;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.legend.LegendSet;

import java.util.List;

/**
* @author Lars Helge Overland
*/
public interface DimensionalItemObject
    extends NameableObject
{
    /**
     * Gets the dimension item identifier.
     */
    String getDimensionItem();
    
    /**
     * Gets the dimension item identifier based on the given
     * identifier scheme.
     * 
     * @param idScheme the identifier scheme.
     */
    String getDimensionItem( IdScheme idScheme );
    
    /**
     * Gets the dimension type of this dimension item.
     */
    DimensionItemType getDimensionItemType();

    /**
     * Gets the legend sets.
     */
    List<LegendSet> getLegendSets();

    /**
     * Gets the first legend set in the legend set list. This
     * field is derived from {@link DimensionalObject#getLegendSet()} and
     * is not persisted.
     * 
     * Will be removed from serialization in 2.28.
     */
    LegendSet getLegendSet();

    /**
     * Indicates whether this dimension has a legend set.
     */
    boolean hasLegendSet();
    
    /**
     * Gets the aggregation type.
     */
    AggregationType getAggregationType();
    
    /**
     * Indicates whether this dimension has an aggregation type.
     */
    boolean hasAggregationType();
    
    /**
     * Gets the total aggregation type, meaning how total values
     * should be aggregated across multiple values.
     */
    TotalAggregationType getTotalAggregationType();
}
