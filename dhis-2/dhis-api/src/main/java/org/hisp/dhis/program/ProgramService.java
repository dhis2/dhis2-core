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

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import org.apache.commons.collections4.SetValuedMap;
import org.hisp.dhis.dataentryform.DataEntryForm;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.trackedentity.TrackedEntityType;

/**
 * @author Abyot Asalefew
 */
public interface ProgramService {
  String ID = ProgramService.class.getName();

  Pattern INPUT_PATTERN = Pattern.compile("(<input.*?/>)", Pattern.DOTALL);

  Pattern DYNAMIC_ATTRIBUTE_PATTERN = Pattern.compile("attributeid=\"(\\w+)\"");

  Pattern PROGRAM_PATTERN = Pattern.compile("programid=\"(\\w+)\"");

  Pattern VALUE_TAG_PATTERN = Pattern.compile("value=\"(.*?)\"", Pattern.DOTALL);

  Pattern TITLE_TAG_PATTERN = Pattern.compile("title=\"(.*?)\"", Pattern.DOTALL);

  Pattern SUGGESTED_VALUE_PATTERN = Pattern.compile("suggested=('|\")(\\w*)('|\")");

  Pattern CLASS_PATTERN = Pattern.compile("class=('|\")(\\w*)('|\")");

  Pattern STYLE_PATTERN = Pattern.compile("style=('|\")([\\w|\\d\\:\\;]+)('|\")");

  /**
   * Adds an {@link Program}
   *
   * @param program The to Program add.
   * @return A generated unique id of the added {@link Program}.
   */
  long addProgram(Program program);

  /**
   * Updates an {@link Program}.
   *
   * @param program the Program to update.
   */
  void updateProgram(Program program);

  /**
   * Deletes a {@link Program}. All {@link ProgramStage}, {@link ProgramInstance} and {@link
   * ProgramStageInstance} belong to this program are removed
   *
   * @param program the Program to delete.
   */
  void deleteProgram(Program program);

  /**
   * Returns a {@link Program}.
   *
   * @param id the id of the Program to return.
   * @return the Program with the given id
   */
  Program getProgram(long id);

  /**
   * Returns all {@link Program}.
   *
   * @return a collection of all Program, or an empty collection if there are no Programs.
   */
  List<Program> getAllPrograms();

  Collection<Program> getPrograms(@Nonnull Collection<String> uids);

  /**
   * Get all {@link Program} belong to a orgunit
   *
   * @param organisationUnit {@link OrganisationUnit}
   * @return The program list
   */
  List<Program> getPrograms(OrganisationUnit organisationUnit);

  /**
   * Returns the {@link Program} with the given UID.
   *
   * @param uid the UID.
   * @return the Program with the given UID, or null if no match.
   */
  Program getProgram(String uid);

  /**
   * Get {@link TrackedEntityType} by TrackedEntityType
   *
   * @param trackedEntityType {@link TrackedEntityType}
   */
  List<Program> getProgramsByTrackedEntityType(TrackedEntityType trackedEntityType);

  /**
   * Get all Programs with the given DataEntryForm.
   *
   * @param dataEntryForm the DataEntryForm.
   * @return a list of Programs.
   */
  List<Program> getProgramsByDataEntryForm(DataEntryForm dataEntryForm);

  /**
   * Get {@link Program} by the current user. Returns all programs if current user is superuser.
   * Returns an empty list if there is no current user.
   *
   * @return Immutable set of programs associated with the current user.
   */
  List<Program> getCurrentUserPrograms();

  /**
   * Returns a list of generated, non-persisted program data elements for the program with the given
   * identifier.
   *
   * @param programUid the program identifier.
   * @return a list of program data elements.
   */
  List<ProgramDataElementDimensionItem> getGeneratedProgramDataElements(String programUid);

  /** Checks whether the given {@link OrganisationUnit} belongs to the specified {@link Program} */
  boolean hasOrgUnit(Program program, OrganisationUnit organisationUnit);

  /**
   * Get all the organisation unit associated for a set of program uids. This method uses jdbc to
   * directly fetch the associated org unit uids for every program uid. This method also filters the
   * results to respect the current users organisation unit scope and sharing settings.
   *
   * @param programUids A set of program uids
   * @return A object of {@link IdentifiableObjectAssociations} containing association for each
   *     programUid
   */
  SetValuedMap<String, String> getProgramOrganisationUnitsAssociationsForCurrentUser(
      Set<String> programUids);

  /**
   * Look for a program - org Unit association in a Cache. If the association exists we return true,
   * otherwise we do a database lookup in the Store and add to the cache. This method checks the
   * associations irrespective of the sharing settings or org unit scopes.
   *
   * @param program input program uid
   * @param orgUnit
   * @return whether a org Unit is associated to the input program
   */
  boolean checkProgramOrganisationUnitsAssociations(String program, String orgUnit);
}
