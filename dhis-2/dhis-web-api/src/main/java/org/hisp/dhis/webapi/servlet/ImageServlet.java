/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.webapi.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.awt.Color;
import java.awt.Dimension;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.sf.jasperreports.engine.DefaultJasperReportsContext;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRPrintImage;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReportsContext;
import net.sf.jasperreports.engine.export.HtmlExporter;
import net.sf.jasperreports.engine.type.ImageTypeEnum;
import net.sf.jasperreports.engine.type.ModeEnum;
import net.sf.jasperreports.engine.type.OnErrorTypeEnum;
import net.sf.jasperreports.engine.util.JRTypeSniffer;
import net.sf.jasperreports.renderers.DataRenderable;
import net.sf.jasperreports.renderers.Renderable;
import net.sf.jasperreports.renderers.ResourceRenderer;
import net.sf.jasperreports.renderers.util.RendererUtil;
import net.sf.jasperreports.repo.RepositoryUtil;

/**
 * This code is a modified version of the original code from JasperReports project. It supports
 * {@code jakarta.servlet} package.
 */
public class ImageServlet extends HttpServlet {
  public static final String IMAGE_NAME_REQUEST_PARAMETER = "image";

  public ImageServlet() {}

  public void service(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    byte[] imageData = null;
    String imageMimeType = null;
    String imageName = request.getParameter("image");
    if ("px".equals(imageName)) {
      try {
        imageData =
            RepositoryUtil.getInstance(this.getJasperReportsContext())
                .getBytesFromLocation("net/sf/jasperreports/engine/images/pixel.GIF");
        imageMimeType = ImageTypeEnum.GIF.getMimeType();
      } catch (JRException var16) {
        JRException e = var16;
        throw new ServletException(e);
      }
    } else {
      List<JasperPrint> jasperPrintList = getJasperPrintList(request);
      if (jasperPrintList == null) {
        throw new ServletException("No JasperPrint documents found on the HTTP session.");
      }

      JRPrintImage image = HtmlExporter.getImage(jasperPrintList, imageName);
      Renderable renderer = image.getRenderer();
      Dimension dimension = new Dimension(image.getWidth(), image.getHeight());
      Color backcolor = ModeEnum.OPAQUE == image.getModeValue() ? image.getBackcolor() : null;
      RendererUtil rendererUtil = RendererUtil.getInstance(this.getJasperReportsContext());

      try {
        imageData = this.process(renderer, dimension, backcolor);
      } catch (Exception var15) {
        Exception e = var15;

        try {
          Renderable onErrorRenderer =
              rendererUtil.handleImageError(e, image.getOnErrorTypeValue());
          if (onErrorRenderer != null) {
            imageData = this.process(onErrorRenderer, dimension, backcolor);
          }
        } catch (Exception var14) {
          Exception ex = var14;
          throw new ServletException(ex);
        }
      }

      imageMimeType =
          rendererUtil.isSvgData(imageData)
              ? "image/svg+xml"
              : JRTypeSniffer.getImageTypeValue(imageData).getMimeType();
    }

    if (imageData != null && imageData.length > 0) {
      if (imageMimeType != null) {
        response.setHeader("Content-Type", imageMimeType);
      }

      response.setContentLength(imageData.length);
      ServletOutputStream outputStream = response.getOutputStream();
      outputStream.write(imageData, 0, imageData.length);
      outputStream.flush();
      outputStream.close();
    }
  }

  protected byte[] process(Renderable renderer, Dimension dimension, Color backcolor)
      throws JRException {
    RendererUtil rendererUtil = RendererUtil.getInstance(this.getJasperReportsContext());
    if (renderer instanceof ResourceRenderer) {
      renderer =
          rendererUtil.getNonLazyRenderable(
              ((ResourceRenderer) renderer).getResourceLocation(), OnErrorTypeEnum.ERROR);
    }

    DataRenderable dataRenderer = rendererUtil.getDataRenderable(renderer, dimension, backcolor);
    return dataRenderer.getData(this.getJasperReportsContext());
  }

  public JasperReportsContext getJasperReportsContext() {
    return DefaultJasperReportsContext.getInstance();
  }

  public static List<JasperPrint> getJasperPrintList(HttpServletRequest request) {
    String jasperPrintListSessionAttr = request.getParameter("jrprintlist");
    if (jasperPrintListSessionAttr == null) {
      jasperPrintListSessionAttr = "net.sf.jasperreports.j2ee.jasper_print_list";
    }

    String jasperPrintSessionAttr = request.getParameter("jrprint");
    if (jasperPrintSessionAttr == null) {
      jasperPrintSessionAttr = "net.sf.jasperreports.j2ee.jasper_print";
    }

    List<JasperPrint> jasperPrintList =
        (List) request.getSession().getAttribute(jasperPrintListSessionAttr);
    if (jasperPrintList == null) {
      JasperPrint jasperPrint =
          (JasperPrint) request.getSession().getAttribute(jasperPrintSessionAttr);
      if (jasperPrint != null) {
        jasperPrintList = new ArrayList();
        ((List) jasperPrintList).add(jasperPrint);
      }
    }

    return (List) jasperPrintList;
  }
}
