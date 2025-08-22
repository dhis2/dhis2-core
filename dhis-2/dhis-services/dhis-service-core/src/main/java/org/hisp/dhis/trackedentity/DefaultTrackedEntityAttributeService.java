/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.trackedentity;

import static java.util.stream.Collectors.toList;

import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramTrackedEntityAttributeStore;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Abyot Asalefew
 */
@RequiredArgsConstructor
@Service("org.hisp.dhis.trackedentity.TrackedEntityAttributeService")
public class DefaultTrackedEntityAttributeService implements TrackedEntityAttributeService {
  private static final int VALUE_MAX_LENGTH = 50000;

  private static final Set<String> VALID_IMAGE_FORMATS =
      ImmutableSet.<String>builder().add(ImageIO.getReaderFormatNames()).build();

  // -------------------------------------------------------------------------
  // Dependencies
  // -------------------------------------------------------------------------

  private final AclService aclService;

  private final TrackedEntityAttributeStore trackedEntityAttributeStore;

  private final TrackedEntityTypeAttributeStore entityTypeAttributeStore;

  private final ProgramTrackedEntityAttributeStore programAttributeStore;

  // -------------------------------------------------------------------------
  // Implementation methods
  // -------------------------------------------------------------------------

  @Override
  @Transactional
  public void deleteTrackedEntityAttribute(TrackedEntityAttribute attribute) {
    trackedEntityAttributeStore.delete(attribute);
  }

  @Override
  @Transactional(readOnly = true)
  public List<TrackedEntityAttribute> getAllTrackedEntityAttributes() {
    return trackedEntityAttributeStore.getAll();
  }

  @Override
  @Transactional(readOnly = true)
  public List<TrackedEntityAttribute> getProgramTrackedEntityAttributes(List<Program> programs) {
    return programAttributeStore.getAttributes(programs);
  }

  @Override
  @Transactional(readOnly = true)
  public TrackedEntityAttribute getTrackedEntityAttribute(long id) {
    return trackedEntityAttributeStore.get(id);
  }

  @Override
  @Transactional
  public long addTrackedEntityAttribute(TrackedEntityAttribute attribute) {
    trackedEntityAttributeStore.save(attribute);
    return attribute.getId();
  }

  @Override
  @Transactional
  public void updateTrackedEntityAttribute(TrackedEntityAttribute attribute) {
    trackedEntityAttributeStore.update(attribute);
  }

  @Override
  @Transactional(readOnly = true)
  public TrackedEntityAttribute getTrackedEntityAttributeByName(String name) {
    return trackedEntityAttributeStore.getByName(name);
  }

  @Override
  @Transactional(readOnly = true)
  public TrackedEntityAttribute getTrackedEntityAttribute(String uid) {
    return trackedEntityAttributeStore.getByUid(uid);
  }

  @Override
  @Transactional(readOnly = true)
  public List<TrackedEntityAttribute> getTrackedEntityAttributes(@Nonnull List<String> uids) {
    return trackedEntityAttributeStore.getByUid(uids);
  }

  @Override
  @Transactional(readOnly = true)
  public List<TrackedEntityAttribute> getTrackedEntityAttributesById(List<Long> ids) {
    return trackedEntityAttributeStore.getById(ids);
  }

  @Override
  @Transactional(readOnly = true)
  public List<TrackedEntityAttribute> getTrackedEntityAttributesByDisplayOnVisitSchedule(
      boolean displayOnVisitSchedule) {
    return trackedEntityAttributeStore.getByDisplayOnVisitSchedule(displayOnVisitSchedule);
  }

  @Override
  @Transactional(readOnly = true)
  public Set<TrackedEntityAttribute> getProgramAttributes(Program program) {
    return getAllUserReadableTrackedEntityAttributes(List.of(program), List.of());
  }

  @Override
  @Transactional(readOnly = true)
  public Set<TrackedEntityAttribute> getTrackedEntityTypeAttributes(
      TrackedEntityType trackedEntityType) {
    return getAllUserReadableTrackedEntityAttributes(List.of(), List.of(trackedEntityType));
  }

  @Override
  @Transactional(readOnly = true)
  public Set<TrackedEntityAttribute> getAllUserReadableTrackedEntityAttributes(
      List<Program> programs, List<TrackedEntityType> trackedEntityTypes) {
    UserDetails userDetails = CurrentUserUtil.getCurrentUserDetails();
    Set<TrackedEntityAttribute> attributes = new HashSet<>();

    if (programs != null && !programs.isEmpty()) {
      attributes.addAll(
          programAttributeStore.getAttributes(
              programs.stream()
                  .filter(program -> aclService.canDataRead(userDetails, program))
                  .collect(toList())));
    }

    if (trackedEntityTypes != null && !trackedEntityTypes.isEmpty()) {
      attributes.addAll(
          entityTypeAttributeStore.getAttributes(
              trackedEntityTypes.stream()
                  .filter(
                      trackedEntityType -> aclService.canDataRead(userDetails, trackedEntityType))
                  .collect(toList())));
    }

    return attributes;
  }

  @Transactional(readOnly = true)
  @Override
  public Set<TrackedEntityAttribute> getAllTrigramIndexableTrackedEntityAttributes() {
    return trackedEntityAttributeStore.getAllTrigramIndexableTrackedEntityAttributes();
  }

  @Transactional(readOnly = true)
  @Override
  public Set<String> getAllTrigramIndexedTrackedEntityAttributes() {
    return trackedEntityAttributeStore.getAllTrigramIndexedTrackedEntityAttributes();
  }

  // -------------------------------------------------------------------------
  // ProgramTrackedEntityAttribute
  // -------------------------------------------------------------------------

  @Override
  @Transactional(readOnly = true)
  public List<TrackedEntityAttribute> getAllSystemWideUniqueTrackedEntityAttributes() {
    return getAllTrackedEntityAttributes().stream()
        .filter(TrackedEntityAttribute::isSystemWideUnique)
        .collect(toList());
  }

  @Override
  @Transactional(readOnly = true)
  public List<TrackedEntityAttribute> getAllUniqueTrackedEntityAttributes() {
    return getAllTrackedEntityAttributes().stream()
        .filter(TrackedEntityAttribute::isUnique)
        .collect(toList());
  }

  @Override
  @Transactional(readOnly = true)
  public Set<TrackedEntityAttribute> getTrackedEntityAttributesByTrackedEntityTypes() {
    return trackedEntityAttributeStore.getTrackedEntityAttributesByTrackedEntityTypes();
  }

  @Override
  @Transactional(readOnly = true)
  public Set<String> getTrackedEntityAttributesInProgram(@Nonnull Program program) {
    return trackedEntityAttributeStore.getTrackedEntityAttributesInProgram(program);
  }
}
