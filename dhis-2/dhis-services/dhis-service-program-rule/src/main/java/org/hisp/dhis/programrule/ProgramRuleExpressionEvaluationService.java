package org.hisp.dhis.programrule;

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

import com.google.common.collect.ImmutableMap;
import org.hisp.dhis.commons.sqlfunc.OneIfZeroOrPositiveSqlFunction;
import org.hisp.dhis.commons.sqlfunc.SqlFunction;
import org.hisp.dhis.program.AddDaysProgramD2Function;
import org.hisp.dhis.program.BaseProgramExpressionEvaluationService;
import org.hisp.dhis.program.CeilProgramD2Function;
import org.hisp.dhis.program.ConcatenateProgramD2Function;
import org.hisp.dhis.program.FloorProgramD2Function;
import org.hisp.dhis.program.HasValueProgramD2Function;
import org.hisp.dhis.program.LeftProgramD2Function;
import org.hisp.dhis.program.LengthProgramD2Function;
import org.hisp.dhis.program.ModulusProgramD2Function;
import org.hisp.dhis.program.OrgUnitGroupProgramD2Function;
import org.hisp.dhis.program.ProgramD2Function;
import org.hisp.dhis.program.RightProgramD2Function;
import org.hisp.dhis.program.RoundProgramD2Function;
import org.hisp.dhis.program.SplitProgramD2Function;
import org.hisp.dhis.program.SubStringProgramD2Function;
import org.hisp.dhis.program.UserRoleProgramD2Function;
import org.hisp.dhis.program.ValidatePatternProgramD2Function;
import org.hisp.dhis.program.ZPVCProgramD2Function;
import org.hisp.dhis.program.ZingProgramD2Function;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

/**
 * @Author Zubair Asghar.
 */
public class ProgramRuleExpressionEvaluationService extends BaseProgramExpressionEvaluationService
{
    private static Map<String, ProgramD2Function> PROGRAM_RULE_D2_FUNC_MAP = ImmutableMap.<String, ProgramD2Function> builder()
        .put( UserRoleProgramD2Function.KEY, new UserRoleProgramD2Function() )
        .put( OrgUnitGroupProgramD2Function.KEY, new OrgUnitGroupProgramD2Function() )
        .put( LengthProgramD2Function.KEY, new LengthProgramD2Function() )
        .put( SplitProgramD2Function.KEY, new SplitProgramD2Function() )
        .put( SubStringProgramD2Function.KEY, new SubStringProgramD2Function() )
        .put( LeftProgramD2Function.KEY, new LeftProgramD2Function() )
        .put( RightProgramD2Function.KEY, new RightProgramD2Function() )
        .put( ValidatePatternProgramD2Function.KEY, new ValidatePatternProgramD2Function() )
        .put( ZPVCProgramD2Function.KEY, new ZPVCProgramD2Function() )
        .put( HasValueProgramD2Function.KEY, new HasValueProgramD2Function() )
        .put( AddDaysProgramD2Function.KEY, new AddDaysProgramD2Function() )
        .put( ConcatenateProgramD2Function.KEY, new ConcatenateProgramD2Function() )
        .put( OneIfZeroOrPositiveSqlFunction.KEY, new ConcatenateProgramD2Function() )
        .put( ZingProgramD2Function.KEY, new ZingProgramD2Function() )
        .put( ModulusProgramD2Function.KEY, new ModulusProgramD2Function() )
        .put( RoundProgramD2Function.KEY, new RoundProgramD2Function() )
        .put( FloorProgramD2Function.KEY, new FloorProgramD2Function() )
        .put( CeilProgramD2Function.KEY, new CeilProgramD2Function() )
        .build();

    @Override
    protected Map<String, ProgramD2Function> getD2Functions()
    {
        D2_FUNC_MAP.putAll( PROGRAM_RULE_D2_FUNC_MAP );
        return D2_FUNC_MAP;
    }

    @Override
    protected Map<String, SqlFunction> getSQLFunctions()
    {
        return new HashMap<>();
    }

    @Override
    protected Map<String, String> getSourceDataElement( String uid, Matcher matcher )
    {
        return null;
    }

    @Override
    protected Map<String, String> getSourceAttribute( String uid, Matcher matcher )
    {
        return null;
    }
}
