<?xml version="1.0" encoding="UTF-8"?>
<!--supply malicious content - START-->
<!DOCTYPE foo [ <!ENTITY xxe SYSTEM "file:///etc/passwd"> ]>
<!--supply malicious content - END-->
<ogr:FeatureCollection
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://ogr.maptools.org/ admin2.xsd"
  xmlns:ogr="http://ogr.maptools.org/"
  xmlns:gml="http://www.opengis.net/gml">
  <gml:boundedBy>
    <gml:Box>
      <gml:coord>
        <gml:X>&xxe;</gml:X>
        <gml:Y>6.928689</gml:Y>
      </gml:coord>
    </gml:Box>
  </gml:boundedBy>
</ogr:FeatureCollection>
