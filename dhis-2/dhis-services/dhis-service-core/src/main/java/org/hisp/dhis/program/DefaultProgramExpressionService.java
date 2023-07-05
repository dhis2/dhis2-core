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
package org.hisp.dhis.program;

import static org.hisp.dhis.program.ProgramExpression.DUE_DATE;
import static org.hisp.dhis.program.ProgramExpression.OBJECT_PROGRAM_STAGE;
import static org.hisp.dhis.program.ProgramExpression.OBJECT_PROGRAM_STAGE_DATAELEMENT;
import static org.hisp.dhis.program.ProgramExpression.REPORT_DATE;
import static org.hisp.dhis.program.ProgramExpression.SEPARATOR_ID;
import static org.hisp.dhis.program.ProgramExpression.SEPARATOR_OBJECT;

import java.util.Collection;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.GenericStore;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Chau Thu Tran
 */
@RequiredArgsConstructor
@Service("org.hisp.dhis.program.ProgramExpressionService")
public class DefaultProgramExpressionService implements ProgramExpressionService {
  private static final String REGEXP =
      "\\[("
          + OBJECT_PROGRAM_STAGE_DATAELEMENT
          + "|"
          + OBJECT_PROGRAM_STAGE
          + ")"
          + SEPARATOR_OBJECT
          + "([a-zA-Z0-9\\- ]+["
          + SEPARATOR_ID
          + "([a-zA-Z0-9\\- ]|"
          + DUE_DATE
          + "|"
          + REPORT_DATE
          + ")+]*)\\]";

  @Qualifier("org.hisp.dhis.program.ProgramExpressionStore")
  private final GenericStore<ProgramExpression> programExpressionStore;

  private final ProgramStageService programStageService;

  private final DataElementService dataElementService;

  // -------------------------------------------------------------------------
  // ProgramExpression CRUD operations
  // -------------------------------------------------------------------------

  @Override
  @Transactional
  public long addProgramExpression(ProgramExpression programExpression) {
    programExpressionStore.save(programExpression);
    return programExpression.getId();
  }

  @Override
  @Transactional
  public void updateProgramExpression(ProgramExpression programExpression) {
    programExpressionStore.update(programExpression);
  }

  @Override
  @Transactional
  public void deleteProgramExpression(ProgramExpression programExpression) {
    programExpressionStore.delete(programExpression);
  }

  @Override
  @Transactional(readOnly = true)
  public ProgramExpression getProgramExpression(long id) {
    return programExpressionStore.get(id);
  }

  @Override
  @Transactional(readOnly = true)
  public String getExpressionDescription(String programExpression) {
    StringBuffer description = new StringBuffer();

    Pattern pattern = Pattern.compile(REGEXP);
    Matcher matcher = pattern.matcher(programExpression);
    int countFormula = 0;

    while (matcher.find()) {
      countFormula++;

      String match = matcher.group();
      String key = matcher.group(1);
      match = match.replaceAll("[\\[\\]]", "");

      String[] info = match.split(SEPARATOR_OBJECT);
      String[] ids = info[1].split(SEPARATOR_ID);

      ProgramStage programStage = programStageService.getProgramStage(ids[0]);
      String name = ids[1];

      if (programStage == null) {
        return INVALID_CONDITION;
      } else if (!name.equals(DUE_DATE) && !name.equals(REPORT_DATE)) {
        DataElement dataElement = dataElementService.getDataElement(name);

        if (dataElement == null) {
          return INVALID_CONDITION;
        } else {
          name = dataElement.getDisplayName();
        }
      }

      matcher.appendReplacement(
          description,
          "["
              + key
              + ProgramExpression.SEPARATOR_OBJECT
              + programStage.getDisplayName()
              + SEPARATOR_ID
              + name
              + "]");
    }

    StringBuffer tail = new StringBuffer();
    matcher.appendTail(tail);

    if (countFormula > 1
        || !tail.toString().isEmpty()
        || (countFormula == 0 && !tail.toString().isEmpty())) {
      return INVALID_CONDITION;
    }

    return description.toString();
  }

  @Override
  @Transactional(readOnly = true)
  public Collection<DataElement> getDataElements(String programExpression) {
    Collection<DataElement> dataElements = new HashSet<>();

    Pattern pattern = Pattern.compile(REGEXP);
    Matcher matcher = pattern.matcher(programExpression);

    while (matcher.find()) {
      String match = matcher.group();
      match = match.replaceAll("[\\[\\]]", "");

      String[] info = match.split(SEPARATOR_OBJECT);
      String[] ids = info[1].split(SEPARATOR_ID);

      DataElement dataElement = dataElementService.getDataElement(ids[1]);
      dataElements.add(dataElement);
    }

    return dataElements;
  }
}
