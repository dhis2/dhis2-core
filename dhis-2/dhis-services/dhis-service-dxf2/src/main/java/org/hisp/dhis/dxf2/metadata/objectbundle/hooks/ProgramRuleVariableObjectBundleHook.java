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
package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

import static org.hisp.dhis.dxf2.Constants.PROGRAM_RULE_VARIABLE_NAME_INVALID_KEYWORDS;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.programrule.ProgramRuleVariable;
import org.hisp.dhis.programrule.ProgramRuleVariableSourceType;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.springframework.stereotype.Component;

/**
 * @author Zubair Asghar.
 */
@Component
public class ProgramRuleVariableObjectBundleHook
    extends AbstractObjectBundleHook<ProgramRuleVariable> {
  private final ImmutableMap<
          ProgramRuleVariableSourceType, BiConsumer<ProgramRuleVariable, ObjectBundle>>
      SOURCE_TYPE_RESOLVER =
          new ImmutableMap.Builder<
                  ProgramRuleVariableSourceType, BiConsumer<ProgramRuleVariable, ObjectBundle>>()
              .put(ProgramRuleVariableSourceType.CALCULATED_VALUE, this::processCalculatedValue)
              .put(
                  ProgramRuleVariableSourceType.DATAELEMENT_CURRENT_EVENT, this::processDataElement)
              .put(
                  ProgramRuleVariableSourceType.DATAELEMENT_NEWEST_EVENT_PROGRAM,
                  this::processDataElement)
              .put(
                  ProgramRuleVariableSourceType.DATAELEMENT_PREVIOUS_EVENT,
                  this::processDataElement)
              .put(
                  ProgramRuleVariableSourceType.DATAELEMENT_NEWEST_EVENT_PROGRAM_STAGE,
                  this::processDataElementWithStage)
              .put(ProgramRuleVariableSourceType.TEI_ATTRIBUTE, this::processTEA)
              .build();

  private static final Set<ImportStrategy> UPDATE_STRATEGIES =
      ImmutableSet.of(
          ImportStrategy.UPDATE,
          ImportStrategy.CREATE_AND_UPDATE,
          ImportStrategy.NEW_AND_UPDATES,
          ImportStrategy.UPDATES);

  private static final String FROM_PROGRAM_RULE_VARIABLE =
      " from ProgramRuleVariable prv where prv.name = :name and prv.program.uid = :programUid";

  private static final String PROGRAM_RULE_VARIABLE_NAME_INVALID_KEYWORDS_REGEX =
      "(" + String.join("|", PROGRAM_RULE_VARIABLE_NAME_INVALID_KEYWORDS) + ")";

  @Override
  public void validate(
      ProgramRuleVariable programRuleVariable,
      ObjectBundle bundle,
      Consumer<ErrorReport> addReports) {
    validateUniqueProgramRuleName(bundle, programRuleVariable, addReports);
    validateProgramRuleNameKeyWords(programRuleVariable, addReports);
  }

  private void validateUniqueProgramRuleName(
      ObjectBundle bundle,
      ProgramRuleVariable programRuleVariable,
      Consumer<ErrorReport> addReports) {
    List<ProgramRuleVariable> prvWithSameNameAndSameProgram =
        getProgramRuleVariableQuery(programRuleVariable).getResultList();

    // update
    if (UPDATE_STRATEGIES.contains(bundle.getImportMode())) {
      if (!isLegitUpdate(programRuleVariable, prvWithSameNameAndSameProgram)) {
        failPrvWithSameNameAlreadyExists(programRuleVariable, addReports);
      }
      return;
    }

    // insert
    if (CollectionUtils.isNotEmpty(prvWithSameNameAndSameProgram)) {
      failPrvWithSameNameAlreadyExists(programRuleVariable, addReports);
    }
  }

  private boolean isLegitUpdate(
      ProgramRuleVariable programRuleVariable, List<ProgramRuleVariable> existingPrvs) {
    return existingPrvs.isEmpty()
        || existingPrvs.stream()
            .anyMatch(existingPrv -> hasSameUid(existingPrv, programRuleVariable));
  }

  private boolean hasSameUid(
      ProgramRuleVariable existingPrv, ProgramRuleVariable programRuleVariable) {
    return StringUtils.equals(existingPrv.getUid(), programRuleVariable.getUid());
  }

  private void failPrvWithSameNameAlreadyExists(
      ProgramRuleVariable programRuleVariable, Consumer<ErrorReport> addReports) {
    addReports.accept(
        new ErrorReport(
            ProgramRuleVariable.class,
            ErrorCode.E4051,
            programRuleVariable.getName(),
            programRuleVariable.getProgram().getUid()));
  }

  private Query<ProgramRuleVariable> getProgramRuleVariableQuery(
      ProgramRuleVariable programRuleVariable) {
    Session session = sessionFactory.getCurrentSession();
    Query<ProgramRuleVariable> query =
        session.createQuery(FROM_PROGRAM_RULE_VARIABLE, ProgramRuleVariable.class);

    query.setParameter("name", programRuleVariable.getName());
    query.setParameter("programUid", programRuleVariable.getProgram().getUid());
    return query;
  }

  private void validateProgramRuleNameKeyWords(
      ProgramRuleVariable programRuleVariable, Consumer<ErrorReport> addReports) {
    String[] split = programRuleVariable.getName().split("\\s");

    if (IntStream.range(0, split.length)
        .anyMatch(i -> split[i].matches(PROGRAM_RULE_VARIABLE_NAME_INVALID_KEYWORDS_REGEX))) {
      addReports.accept(
          new ErrorReport(
              ProgramRuleVariable.class, ErrorCode.E4052, programRuleVariable.getName()));
    }
  }

  @Override
  public void preUpdate(
      ProgramRuleVariable variable, ProgramRuleVariable persistedObject, ObjectBundle bundle) {
    BiConsumer<ProgramRuleVariable, ObjectBundle> mod =
        SOURCE_TYPE_RESOLVER.get(variable.getSourceType());
    if (mod != null) {
      mod.accept(variable, bundle);
    }
  }

  @Override
  public void preCreate(ProgramRuleVariable variable, ObjectBundle bundle) {
    preUpdate(variable, null, bundle);
  }

  private void processCalculatedValue(ProgramRuleVariable variable, ObjectBundle bundle) {
    variable.setAttribute(null);
    variable.setDataElement(null);
    variable.setProgramStage(null);
  }

  private void processDataElement(ProgramRuleVariable variable, ObjectBundle bundle) {
    DataElement dataElement =
        bundle.getPreheat().get(bundle.getPreheatIdentifier(), variable.getDataElement());
    variable.setAttribute(null);
    variable.setProgramStage(null);
    variable.setDataElement(dataElement);
    variable.setValueType(dataElement.getValueType());
  }

  private void processTEA(ProgramRuleVariable variable, ObjectBundle bundle) {
    TrackedEntityAttribute trackedEntityAttribute =
        bundle.getPreheat().get(bundle.getPreheatIdentifier(), variable.getAttribute());
    variable.setDataElement(null);
    variable.setProgramStage(null);
    variable.setAttribute(trackedEntityAttribute);
    variable.setValueType(trackedEntityAttribute.getValueType());
  }

  private void processDataElementWithStage(ProgramRuleVariable variable, ObjectBundle bundle) {
    variable.setAttribute(null);
  }
}
