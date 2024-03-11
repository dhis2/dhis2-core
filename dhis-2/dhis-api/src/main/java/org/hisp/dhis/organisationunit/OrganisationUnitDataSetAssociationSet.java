package org.hisp.dhis.organisationunit;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Lars Helge Overland
 */
public class OrganisationUnitDataSetAssociationSet
{
    /**
     * List of data set association sets.
     */
    private List<Set<String>> dataSetAssociationSets = new ArrayList<>();

    /**
     * Mapping between organisation unit identifier and index of association set in list.
     */
    private Map<String, Integer> organisationUnitAssociationSetMap = new HashMap<>();

    /**
     * Set of distinct data sets in all association sets.
     */
    private Set<String> distinctDataSets = new HashSet<>();

    /**
     * Map contains authorities for each DataSet for current user
     */
    private Map<String, String> dataSetAuthMap = new HashMap<>();

    public OrganisationUnitDataSetAssociationSet()
    {
    }

    public List<Set<String>> getDataSetAssociationSets()
    {
        return dataSetAssociationSets;
    }

    public void setDataSetAssociationSets( List<Set<String>> dataSetAssociationSets )
    {
        this.dataSetAssociationSets = dataSetAssociationSets;
    }

    public Map<String, Integer> getOrganisationUnitAssociationSetMap()
    {
        return organisationUnitAssociationSetMap;
    }

    public void setOrganisationUnitAssociationSetMap( Map<String, Integer> organisationUnitAssociationSetMap )
    {
        this.organisationUnitAssociationSetMap = organisationUnitAssociationSetMap;
    }

    public Set<String> getDistinctDataSets()
    {
        return distinctDataSets;
    }

    public void setDistinctDataSets( Set<String> distinctDataSets )
    {
        this.distinctDataSets = distinctDataSets;
    }

    public Map<String, String> getDataSetAuthMap()
    {
        return this.dataSetAuthMap;
    }

    public void setDataSetAuthMap( Map<String, String> dataSetAuthMap )
    {
        this.dataSetAuthMap = dataSetAuthMap;
    }
}
