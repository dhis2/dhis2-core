package org.hisp.dhis.program;

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

import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValue;

import java.util.Collection;
import java.util.Map;

/**
 * An Expression is the expression of e.g. a validation rule of a program. It
 * consist of a String representation of the rule as well as references to the
 * data elements and program stages included in the expression.
 * <p/>
 * The expression contains references to data elements and program stages on the
 * form:
 * <p/>
 * i) [DE:1.2] where 1 refers to the program stage identifier and 2 refers to
 * the data element identifier.
 * <p/>
 *
 * @author Chau Thu Tran
 * @version ProgramExpressionService.java 2:59:58 PM Nov 8, 2012 $
 */
public interface ProgramExpressionService
{
    String ID = ProgramExpressionService.class.getName();

    String INVALID_CONDITION = "Expression is not well-formed";
    
    /**
     * Adds an {@link ProgramExpression}
     *
     * @param programExpression The to ProgramExpression add.
     * @return A generated unique id of the added {@link ProgramExpression}.
     */
    int addProgramExpression( ProgramExpression programExpression );

    /**
     * Updates an {@link ProgramExpression}.
     *
     * @param programExpression the ProgramExpression to update.
     */
    void updateProgramExpression( ProgramExpression programExpression );

    /**
     * Deletes a {@link ProgramExpression}.
     *
     * @param programExpression the ProgramExpression to delete.
     */
    void deleteProgramExpression( ProgramExpression programExpression );

    /**
     * Returns a {@link ProgramExpression}.
     *
     * @param id the id of the ProgramExpression to return.
     * @return the ProgramExpression with the given id
     */
    ProgramExpression getProgramExpression( int id );

    /**
     * Get value of program expression
     *
     * @param programExpression         {@link ProgramExpression}
     * @param programStageInstance      The {@link ProgramStageInstance} associate
     *                                  with this expression
     * @param trackedEntityDataValueMap TrackedEntityDataValue<The id of {@link DataElement}
     *                                  , {@link TrackedEntityDataValue} >
     * @return The expression value
     */
    String getProgramExpressionValue( ProgramExpression programExpression, ProgramStageInstance programStageInstance,
        Map<String, String> trackedEntityDataValueMap );

    /**
     * Get the description of a program expression
     *
     * @param programExpression The expression
     * @return the description of an expression
     */
    String getExpressionDescription( String programExpression );
    
    /**
     * Get the Data Element collection of a program expression
     *
     * @param programExpression The expression
     * @return the DataElement collection
     */
    Collection<DataElement> getDataElements( String programExpression );

}
