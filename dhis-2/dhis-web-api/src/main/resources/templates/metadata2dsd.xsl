<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet
  xmlns:d2="http://dhis2.org/schema/dxf/2.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  version="1.0">

  <xsl:output method="xml" indent="yes" />

  <xsl:key name="disaggs_per_categorycombo"
    match="/d2:metaData/d2:categoryOptionCombos/d2:categoryOptionCombo"
    use="d2:categoryCombo/@id" />

  <xsl:template match="/">
    <reportTemplates>
      <dataElements>
        <xsl:apply-templates select="/d2:metaData/d2:dataElements" />
      </dataElements>
      <disaggregations>
        <xsl:apply-templates select="/d2:metaData/d2:categoryOptionCombos" />
      </disaggregations>
      <xsl:apply-templates select="/d2:metaData/d2:dataSets" />
    </reportTemplates>
  </xsl:template>

  <xsl:template match="d2:dataElement">
    <dataElement uid="{@id}" code="{@code}" name="{@name}" type="{d2:type}" />
  </xsl:template>

  <xsl:template match="d2:categoryOptionCombo">
    <!-- catoptcombos have no code :-(
      <disaggregation uid="{@id}" code="{@code}" name="{@name}" />
    -->
    <disaggregation uid="{@id}" code="{@id}" name="{@name}" />
  </xsl:template>

  <xsl:template match="d2:dataSet">
    <reportTemplate>
      <name>
        <xsl:value-of select="@name" />
      </name>
      <uid>
        <xsl:value-of select="@id" />
      </uid>
      <code>
        <xsl:value-of select="@code" />
      </code>
      <periodType>
        <xsl:value-of select="d2:periodType" />
      </periodType>
      <dataValueTemplates>
        <xsl:apply-templates select="d2:dataElements" mode="dataset" />
      </dataValueTemplates>
    </reportTemplate>
  </xsl:template>

  <xsl:template match="d2:dataElement" mode="dataset">
    <xsl:variable name="de_id" select="@id" />
    <xsl:variable name="de_code" select="@code" />
    <xsl:variable name="catcombo"
      select="/d2:metaData/d2:dataElements/d2:dataElement[@id=$de_id]/d2:categoryCombo/@id" />
    <xsl:for-each select="key('disaggs_per_categorycombo',$catcombo)">
      <xsl:element name="dataValueTemplate">
        <xsl:attribute name="dataElement">
          <xsl:value-of select="$de_code" />
        </xsl:attribute>
        <xsl:attribute name="disaggregation">
          <xsl:value-of select="@id" />
        </xsl:attribute>
      </xsl:element>
    </xsl:for-each>
  </xsl:template>

</xsl:stylesheet>