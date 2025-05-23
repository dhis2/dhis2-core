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
package org.hisp.dhis.webapi.controller.dataentry;

import static org.hisp.dhis.common.collection.CollectionUtils.mapToList;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.dataset.LockException;
import org.hisp.dhis.webapi.webdomain.dataentry.DataEntryDtoMapper;
import org.hisp.dhis.webapi.webdomain.dataentry.LockExceptionsDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Lars Helge Overland
 */
@OpenApi.Document(
    entity = LockException.class,
    classifiers = {"team:platform", "purpose:metadata"})
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dataEntry")
public class DataSetLockExceptionController {
  private final DataSetService dataSetService;

  @GetMapping("/lockExceptions")
  public LockExceptionsDto getLockExceptions() {
    List<LockException> lockExceptions = dataSetService.getDataWriteLockExceptions();

    return new LockExceptionsDto()
        .setLockExceptions(mapToList(lockExceptions, DataEntryDtoMapper::toDto));
  }
}
