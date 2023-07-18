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
package org.hisp.dhis.helpers.file;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.function.Function;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.hisp.dhis.actions.IdGenerator;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class XmlFileReader implements FileReader {
  Document document;

  public XmlFileReader(File file) throws ParserConfigurationException, IOException, SAXException {
    document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file);
  }

  @Override
  public FileReader read(File file) throws Exception {
    return new XmlFileReader(file);
  }

  @Override
  public FileReader replacePropertyValuesWithIds(String propertyName) {
    return replacePropertyValuesWith(propertyName, "UNIQUEID");
  }

  @Override
  public FileReader replacePropertyValuesWith(String propertyName, String replacedValue) {
    XPath xPath = XPathFactory.newInstance().newXPath();

    try {
      NodeList nodes =
          (NodeList) xPath.evaluate("//*[@" + propertyName + "]", document, XPathConstants.NODESET);

      for (int i = 0; i < nodes.getLength(); i++) {
        Node node = nodes.item(i).getAttributes().getNamedItem(propertyName);

        if (replacedValue.equalsIgnoreCase("uniqueid")) {
          node.setNodeValue(new IdGenerator().generateUniqueId());
          continue;
        }
        node.setNodeValue(replacedValue);
      }

    } catch (XPathExpressionException e) {
      e.printStackTrace();
    }

    return this;
  }

  @Override
  public FileReader replacePropertyValuesRecursivelyWith(
      String propertyName, String replacedValue) {
    return null;
  }

  @Override
  public FileReader replace(Function<Object, Object> function) {
    return null;
  }

  @Override
  public Object get() {
    try {
      StringWriter stringWriter = new StringWriter();
      StreamResult streamResult = new StreamResult(stringWriter);

      Transformer transformer = TransformerFactory.newInstance().newTransformer();
      transformer.transform(new DOMSource(document), streamResult);

      return stringWriter.toString();
    } catch (TransformerConfigurationException e) {
      e.printStackTrace();
    } catch (TransformerException e) {
      e.printStackTrace();
    }

    return "";
  }
}
