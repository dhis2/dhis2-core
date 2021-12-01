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
package org.hisp.dhis.parser.expression.function;

import java.util.Optional;
import java.util.regex.Pattern;

import org.hisp.dhis.antlr.ParserException;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.parser.expression.CommonExpressionVisitor;
import org.hisp.dhis.parser.expression.ExpressionItem;
import org.hisp.dhis.parser.expression.antlr.ExpressionParser;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageService;

/**
 * @author Dusan Bernat
 */

public class StageOffset implements ExpressionItem
{
    private static final int UID_LENGTH = 11;

    @Override
    public Object evaluate( ExpressionParser.ExprContext ctx, CommonExpressionVisitor visitor )
    {
        return visitor.visit( ctx.expr( 0 ) );
    }

    @Override
    public Object getDescription( ExpressionParser.ExprContext ctx, CommonExpressionVisitor visitor )
    {
        String programStageUid = getProgramStageUid( ctx.expr( 0 ) );

        ProgramStageService stageService = visitor.getProgramStageService();

        ProgramStage stage = stageService != null ? stageService.getProgramStage( programStageUid ) : null;

        if ( stage == null || !stage.getRepeatable() )
        {
            String errorMessage = "StageOffset is allowed only for repeatable stages";

            if ( isValidUid( programStageUid ) )
            {
                errorMessage += " (" + programStageUid + " is not repeatable)";
            }

            throw new ParserException( errorMessage );
        }

        return ExpressionItem.super.getDescription( ctx, visitor );
    }

    @Override
    public Object getSql( ExpressionParser.ExprContext ctx, CommonExpressionVisitor visitor )
    {
        String programStageUid = getProgramStageUid( ctx.expr( 0 ) );

        Optional<DataElement> dataElement = visitor.getProgramIndicator().getProgram().getDataElements().stream()
            .filter( de -> visitor.getProgramIndicator().getExpression().contains( de.getUid() ) ).findFirst();

        Optional<ProgramStage> programStage = visitor.getProgramIndicator().getProgram().getProgramStages()
            .stream().filter( ps -> programStageUid.equals( ps.getUid() ) && ps.getRepeatable() ).findFirst();

        if ( programStage.isPresent() && dataElement.isPresent() )
        {
            return " coalesce("
                + visitor.getStatementBuilder().getProgramIndicatorEventColumnSql( programStageUid, ctx.stage.getText(),
                    "\"" + dataElement.get().getUid() + "\"",
                    visitor.getReportingStartDate(), visitor.getReportingEndDate(), visitor.getProgramIndicator() )
                + "::numeric, 0)";
        }
        else
        {
            throw new ParserException( "StageOffset is allowed only for repeatable stages" );
        }
    }

    private String getProgramStageUid( ExpressionParser.ExprContext ctx )
    {
        if ( ctx.uid0 != null )
        {
            return ctx.uid0.getText();
        }

        String firstFragment = ctx.getText().split( "\\." )[0];

        if ( firstFragment.length() >= UID_LENGTH )
        {
            return firstFragment.substring( firstFragment.length() - UID_LENGTH );
        }

        return firstFragment;
    }

    private boolean isValidUid( String value )
    {
        final Pattern uidPattern = Pattern.compile( "^[a-zA-Z]{1}[a-zA-Z0-9]{10}$" );

        return value != null && uidPattern.matcher( value ).matches();
    }
}
