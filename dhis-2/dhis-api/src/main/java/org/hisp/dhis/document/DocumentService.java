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
package org.hisp.dhis.document;

import java.util.List;
import org.hisp.dhis.user.User;

/**
 * @author Lars Helge Overland
 * @version $Id$
 */
public interface DocumentService {
  String ID = DocumentService.class.getName();

  String DIR = "documents";

  /**
   * Saves a Document.
   *
   * @param document the Document to save.
   * @return the generated identifier.
   */
  long saveDocument(Document document);

  /**
   * Retrieves the Document with the given identifier.
   *
   * @param id the identifier of the Document.
   * @return the Document.
   */
  Document getDocument(long id);

  /**
   * Retrieves the Document with the given identifier.
   *
   * @param uid the identifier of the Document.
   * @return the Document.
   */
  Document getDocument(String uid);

  /**
   * Used when removing a file reference from a Document.
   *
   * @param document
   */
  void deleteFileFromDocument(Document document);

  /**
   * Deletes a Document.
   *
   * @param document the Document to delete.
   */
  void deleteDocument(Document document);

  /**
   * Retrieves all Documents.
   *
   * @return a Collection of Documents.
   */
  List<Document> getAllDocuments();

  int getDocumentCount();

  int getDocumentCountByName(String name);

  List<Document> getDocumentsByUid(List<String> uids);

  long getCountDocumentByUser(User user);
}
