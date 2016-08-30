package org.hisp.dhis.trackedentity.action.trackedentityattribute;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import com.opensymphony.xwork2.Action;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeService;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.legend.LegendService;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.option.OptionService;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.system.util.AttributeUtils;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Chau Thu Tran
 */
public class ShowAddUpdateAttributeAction
    implements Action
{
    // -------------------------------------------------------------------------
    // Dependency
    // -------------------------------------------------------------------------

    @Autowired
    private TrackedEntityAttributeService trackedEntityAttributeService;

    @Autowired
    private TrackedEntityService trackedEntityService;

    @Autowired
    private ProgramService programService;

    @Autowired
    private PeriodService periodService;

    @Autowired
    private OptionService optionService;

    @Autowired
    private LegendService legendService;

    @Autowired
    private AttributeService attributeService;

    @Autowired
    private DhisConfigurationProvider configurationProvider;

    // -------------------------------------------------------------------------
    // Input/Output
    // -------------------------------------------------------------------------

    private Integer id;

    public void setId( Integer id )
    {
        this.id = id;
    }

    private TrackedEntityAttribute attribute;

    public TrackedEntityAttribute getAttribute()
    {
        return attribute;
    }

    private List<Program> programs;

    public List<Program> getPrograms()
    {
        return programs;
    }

    private List<PeriodType> periodTypes;

    public List<PeriodType> getPeriodTypes()
    {
        return periodTypes;
    }

    private List<OptionSet> optionSets;

    public List<OptionSet> getOptionSets()
    {
        return optionSets;
    }

    private List<LegendSet> legendSets;

    public List<LegendSet> getLegendSets()
    {
        return legendSets;
    }

    private List<Attribute> attributes;

    public List<Attribute> getAttributes()
    {
        return attributes;
    }

    private Map<Integer, String> attributeValues = new HashMap<>();

    public Map<Integer, String> getAttributeValues()
    {
        return attributeValues;
    }

    private List<TrackedEntity> trackedEntities;

    public List<TrackedEntity> getTrackedEntities()
    {
        return trackedEntities;
    }

    private boolean encryptionAvailable;
    
    public boolean isEncryptionAvailable()
    {
        return encryptionAvailable;
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
        throws Exception
    {
        if ( id != null )
        {
            attribute = trackedEntityAttributeService.getTrackedEntityAttribute( id );
            attributeValues = AttributeUtils.getAttributeValueMap( attribute.getAttributeValues() );

            programs = programService.getAllPrograms();
            programs.removeAll( programService.getPrograms( ProgramType.WITHOUT_REGISTRATION ) );
            Collections.sort( programs );
        }
        
        encryptionAvailable = configurationProvider.getEncryptionStatus().isOk();
        
        periodTypes = periodService.getAllPeriodTypes();
        optionSets = optionService.getAllOptionSets();
        legendSets = legendService.getAllLegendSets();
        attributes = attributeService.getAttributes( TrackedEntityAttribute.class );
        trackedEntities = trackedEntityService.getAllTrackedEntity();

        Collections.sort( optionSets );
        Collections.sort( legendSets );
        Collections.sort( trackedEntities );

        return SUCCESS;
    }
}
