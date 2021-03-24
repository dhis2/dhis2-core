/*
 * Copyright (c) 2004-2004-2020, University of Oslo
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
package org.hisp.dhis.commons.jsonfiltering.metric;

import java.util.SortedMap;

import org.hisp.dhis.commons.jsonfiltering.bean.BeanInfoIntrospector;
import org.hisp.dhis.commons.jsonfiltering.filter.JsonFilteringPropertyFilter;
import org.hisp.dhis.commons.jsonfiltering.metric.source.CompositeJsonFilteringMetricsSource;
import org.hisp.dhis.commons.jsonfiltering.metric.source.JsonFilteringMetricsSource;
import org.hisp.dhis.commons.jsonfiltering.parser.JsonFilteringParser;

import com.google.common.collect.Maps;

/**
 * Provides API for obtaining various metrics in the json-filtering libraries,
 * such as cache statistics.
 */
public class JsonFilteringMetrics
{

    private static final JsonFilteringMetricsSource METRICS_SOURCE;

    static
    {
        METRICS_SOURCE = new CompositeJsonFilteringMetricsSource(
            JsonFilteringParser.getMetricsSource(),
            JsonFilteringPropertyFilter.getMetricsSource(),
            BeanInfoIntrospector.getMetricsSource() );
    }

    private JsonFilteringMetrics()
    {
    }

    /**
     * Gets the metrics as a map whose keys are the metric name and whose values
     * are the metric values.
     *
     * @return map
     */
    public static SortedMap<String, Object> asMap()
    {
        SortedMap<String, Object> metrics = Maps.newTreeMap();
        METRICS_SOURCE.applyMetrics( metrics );
        return metrics;
    }
}
