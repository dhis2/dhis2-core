<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="html" encoding="UTF-8"/>

<xsl:param name="sectionId">resourceTables</xsl:param>

<xsl:template match="section">
  <h3><xsl:value-of select="title"/></h3>
  <xsl:apply-templates select="para|orderedlist|itemizedlist|section"/>
</xsl:template>

<xsl:template match="para">
  <p><xsl:value-of select="."/></p>
</xsl:template>

<xsl:template match="orderedlist">
  <ol><xsl:apply-templates/></ol>
</xsl:template>

<xsl:template match="itemizedlist">
  <ul><xsl:apply-templates/></ul>
</xsl:template>

<xsl:template match="listitem">
  <li><xsl:apply-templates select="para|orderedlist|itemizedlist"/></li>
</xsl:template>
 
<xsl:template match="/">
  <xsl:apply-templates select="book/chapter//section[@id=$sectionId]"/>
</xsl:template>

</xsl:stylesheet>