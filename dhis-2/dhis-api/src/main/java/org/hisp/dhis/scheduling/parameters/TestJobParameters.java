/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.scheduling.parameters;

import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.scheduling.JobParameters;
import org.hisp.dhis.scheduling.JobProgress.FailurePolicy;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement( localName = "jobParameters", namespace = DxfNamespaces.DXF_2_0 )
public class TestJobParameters implements JobParameters
{
    /**
     * Initial waiting time in millis (as there is no actual "work" done)
     */
    private Long waitMillis;

    /**
     * Number of stages in the job, zero if {@code null}
     */
    private Integer stages;

    /**
     * Stage number at which an item fails, none of {@code null}
     */
    private Integer failAtStage;

    /**
     * Number of items in each stage, none if {@code null}
     */
    private Integer items;

    /**
     * Item number in the failing stage at which the failure occurs, none if
     * {@code null}
     */
    private Integer failAtItem;

    /**
     * Duration each item takes in millis, zero if {@code null}
     */
    private Long itemDuration;

    /**
     * The message used when failing the item
     */
    private String failWithMessage;

    /**
     * When true, an exception is used to fail, otherwise the progress tracking
     * api is used
     */
    private boolean failWithException;

    /**
     * Stage failure policy to use, when {@code null} it is the default policy
     */
    private FailurePolicy failWithPolicy;

    private boolean runStagesParallel;

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Long getWaitMillis()
    {
        return waitMillis;
    }

    public void setWaitMillis( Long waitMillis )
    {
        this.waitMillis = waitMillis;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Integer getStages()
    {
        return stages;
    }

    public void setStages( Integer stages )
    {
        this.stages = stages;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Integer getFailAtStage()
    {
        return failAtStage;
    }

    public void setFailAtStage( Integer failAtStage )
    {
        this.failAtStage = failAtStage;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Integer getItems()
    {
        return items;
    }

    public void setItems( Integer items )
    {
        this.items = items;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Integer getFailAtItem()
    {
        return failAtItem;
    }

    public void setFailAtItem( Integer failAtItem )
    {
        this.failAtItem = failAtItem;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Long getItemDuration()
    {
        return itemDuration;
    }

    public void setItemDuration( Long itemDuration )
    {
        this.itemDuration = itemDuration;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getFailWithMessage()
    {
        return failWithMessage;
    }

    public void setFailWithMessage( String failWithMessage )
    {
        this.failWithMessage = failWithMessage;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isFailWithException()
    {
        return failWithException;
    }

    public void setFailWithException( boolean failWithException )
    {
        this.failWithException = failWithException;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public FailurePolicy getFailWithPolicy()
    {
        return failWithPolicy;
    }

    public void setFailWithPolicy( FailurePolicy failWithPolicy )
    {
        this.failWithPolicy = failWithPolicy;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isRunStagesParallel()
    {
        return runStagesParallel;
    }

    public void setRunStagesParallel( boolean runStagesParallel )
    {
        this.runStagesParallel = runStagesParallel;
    }
}
