package org.hisp.dhis.deduplication;

/*
 * Copyright (c) 2004-2019, University of Oslo
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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.annotation.Property;
import org.hisp.dhis.schema.annotation.PropertyRange;

public class PotentialDuplicate
    extends BaseIdentifiableObject
{

        private long id;
        /**
         * teiA represents the UID of a TrackedEntityInstance.
         * teiA is required.
         * teiA is a potential duplicate of teiB.
         * if teiB is null, it indicates a user has flagged teiA as
         * a potential duplicate, without knowing which TrackedEntityInstance
         * it is a duplicate of.
         */
        private String teiA;

        /**
         * teiB represents the UID of a TrackedEntityInstance.
         * teiB is optional.
         * teiB is a potential duplicate of teiA.
         */
        private String teiB;

        /**
         * status represents the state of the PotentialDuplicate.
         * all new Potential duplicates are OPEN by default.
         */
        private DeduplicationStatus status = DeduplicationStatus.OPEN;

        public PotentialDuplicate()
        {
        }

        public PotentialDuplicate( String teiA )
        {
                this.teiA = teiA;
        }

        public PotentialDuplicate( String teiA, String teiB )
        {
                this.teiA = teiA;
                this.teiB = teiB;
        }

        @JsonProperty
        @JacksonXmlProperty
        @Property( value = PropertyType.IDENTIFIER, required = Property.Value.TRUE )
        @PropertyRange( min = 11, max = 11 )
        public String getTeiA()
        {
                return teiA;
        }

        public void setTeiA( String teiA )
        {
                this.teiA = teiA;
        }

        @JsonProperty
        @JacksonXmlProperty
        @Property( value = PropertyType.IDENTIFIER, required = Property.Value.FALSE )
        @PropertyRange( min = 11, max = 11 )
        public String getTeiB()
        {
                return teiB;
        }

        public void setTeiB( String teiB )
        {
                this.teiB = teiB;
        }

        @JsonProperty
        @JacksonXmlProperty
        public DeduplicationStatus getStatus()
        {
                return status;
        }

        public void setStatus( DeduplicationStatus status )
        {
                this.status = status;
        }
}
