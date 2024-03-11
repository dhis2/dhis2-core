<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="html" encoding="UTF-8"/>

<xsl:template match="chapter">
  <h6><xsl:value-of select="title"/></h6>
  <div>
    <ul><xsl:apply-templates select="descendant::section[@id]"/></ul>
  </div>
</xsl:template>

<xsl:template match="section">
  <li><a href="javascript:getHelpItemContent('{@id}')"><xsl:value-of select="title"/></a></li>
</xsl:template>

<xsl:template match="/">
  <xsl:apply-templates select="book/chapter"/>
</xsl:template>

</xsl:stylesheet>
