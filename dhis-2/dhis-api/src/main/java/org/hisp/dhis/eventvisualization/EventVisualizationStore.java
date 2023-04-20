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
package org.hisp.dhis.eventvisualization;

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.hisp.dhis.common.IdentifiableObjectStore;

/**
 * DEPRECATED Needed to keep EventChart and EventReports backward compatible
 * with the new entity EventVisualization.
 *
 * @author maikel arabori
 */
public interface EventVisualizationStore
    extends
    IdentifiableObjectStore<EventVisualization>
{
    /**
     * Query the EventVisualization collection and retrieve only the
     * EventVisualizations of type Chart.
     *
     * @param first the first result row
     * @param max the maximum result row
     * @return a list of Visualization containing only Charts
     */
    List<EventVisualization> getCharts( int first, int max );

    /**
     * Query the EventVisualization collection and retrieve only the
     * EventVisualizations of type Reports (Pivot and Line list).
     *
     * @param first the first result row
     * @param max the maximum result row
     * @return a list of EventVisualization containing only Reports (Pivot and
     *         Line list)
     */
    List<EventVisualization> getReports( int first, int max );

    /**
     * Query the EventVisualization collection and retrieve the
     * EventVisualizations of type Line List ONLY.
     *
     * @param first the first result row
     * @param max the maximum result row
     * @return a list of EventVisualization containing only Line List
     */
    List<EventVisualization> getLineLists( int first, int max );

    /**
     * Query the EventVisualization collection and retrieve only the
     * EventVisualizations of type Chart comparing the name using the given
     * "chars".
     *
     * @param words the characters describing the EventVisualization's name
     * @param first the first result row
     * @param max the maximum result row
     * @return a list of EventVisualization containing only Charts
     */
    List<EventVisualization> getChartsLikeName( Set<String> words, int first, int max );

    /**
     * Query the EventVisualization collection and retrieve only the
     * EventVisualizations of type Reports (Pivot and Line list) comparing the
     * name using the given "chars".
     *
     * @param words the characters describing the Visualization's name
     * @param first the first result row
     * @param max the maximum result row
     * @return a list of EventVisualization containing only Reports (Pivot and
     *         Line list)
     */
    List<EventVisualization> getReportsLikeName( Set<String> words, int first, int max );

    /**
     * Query the EventVisualization collection and retrieve only the
     * EventVisualizations of type Line list only, comparing the name based on
     * the given "words".
     *
     * @param words the characters describing the Visualization's name.
     * @param first the first result row.
     * @param max the maximum result row.
     * @return a list of EventVisualization containing only Line lists.
     */
    List<EventVisualization> getLineListsLikeName( Set<String> words, int first, int max );

    /**
     * Counts the number of Reports (Pivot and Line list) created since the
     * given date.
     *
     * @param startingAt
     * @return the total of Reports found.
     */
    int countReportsCreated( Date startingAt );

    /**
     * Counts the number of Chart created since the given date.
     *
     * @param startingAt
     * @return the total of Chart found.
     */
    int countChartsCreated( Date startingAt );

    /**
     * Counts the number of EventVisualization created since the given date.
     *
     * @param startingAt
     * @return the total of EventVisualization found.
     */
    int countEventVisualizationsCreated( Date startingAt );
}
