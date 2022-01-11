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
package org.hisp.dhis.schema.descriptors;

import java.util.Collection;
import java.util.function.Function;

import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.programrule.ProgramRuleVariable;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaDescriptor;
import org.hisp.dhis.security.Authority;
import org.hisp.dhis.security.AuthorityType;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

/**
 * @author markusbekken
 */
public class ProgramRuleVariableSchemaDescriptor implements SchemaDescriptor
{
    public static final String SINGULAR = "programRuleVariable";

    public static final String PLURAL = "programRuleVariables";

    public static final String API_ENDPOINT = "/" + PLURAL;

    @Override
    public Schema getSchema()
    {
        Schema schema = new Schema( ProgramRuleVariable.class, SINGULAR, PLURAL );
        schema.setRelativeApiEndpoint( API_ENDPOINT );
        schema.setOrder( 1600 );

        schema.add( new Authority( AuthorityType.CREATE, Lists.newArrayList( "F_PROGRAM_RULE_MANAGEMENT" ) ) );
        schema.add( new Authority( AuthorityType.DELETE, Lists.newArrayList( "F_PROGRAM_RULE_MANAGEMENT" ) ) );

        schema.setUniqueMultiPropertiesExctractors(
            ImmutableMap.<Collection<String>, Collection<Function<IdentifiableObject, String>>> builder()
                .put(
                    ImmutableList.of( "name", "programuid" ),
                    ImmutableList.of(
                        nameExtractor(),
                        programUidExtractor() ) )
                .build() );

        return schema;
    }

    private Function<IdentifiableObject, String> nameExtractor()
    {
        return o -> castAndExtract( o, BaseIdentifiableObject::getName );
    }

    private Function<IdentifiableObject, String> programUidExtractor()
    {
        return o -> castAndExtract( o, programRuleVariable -> programRuleVariable.getProgram().getUid() );
    }

    private String castAndExtract( Object o, Function<ProgramRuleVariable, String> extractor )
    {
        ProgramRuleVariable programRuleVariable = (ProgramRuleVariable) o;
        return extractor.apply( programRuleVariable );
    };

}
