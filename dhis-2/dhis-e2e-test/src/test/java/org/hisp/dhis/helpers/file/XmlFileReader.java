package org.hisp.dhis.helpers.file;

import org.hisp.dhis.actions.IdGenerator;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

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
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.function.Function;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class XmlFileReader
    implements FileReader
{
    Document document;

    public XmlFileReader( File file )
        throws ParserConfigurationException, IOException, SAXException
    {
        document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse( file );
    }

    @Override
    public FileReader read( File file )
        throws Exception
    {
        return new XmlFileReader( file );
    }

    @Override
    public FileReader replacePropertyValuesWithIds( String propertyValues )
    {
        XPath xPath = XPathFactory.newInstance().newXPath();

        try
        {
            NodeList nodes = (NodeList) xPath.evaluate( "//*[@" + propertyValues + "]", document, XPathConstants.NODESET );

            for ( int i = 0; i < nodes.getLength(); i++ )
            {
                Node node = nodes.item( i ).getAttributes().getNamedItem( propertyValues );

                node.setNodeValue( new IdGenerator().generateUniqueId() );
            }

        }
        catch ( XPathExpressionException e )
        {
            e.printStackTrace();
        }

        return this;
    }

    @Override
    public FileReader replace( Function<Object, Object> function )
    {
        return null;
    }

    @Override
    public Object get()
    {
        try
        {
            StringWriter stringWriter = new StringWriter();
            StreamResult streamResult = new StreamResult( stringWriter );

            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.transform( new DOMSource( document ), streamResult );

            return stringWriter.toString();
        }
        catch ( TransformerConfigurationException e )
        {
            e.printStackTrace();
        }
        catch ( TransformerException e )
        {
            e.printStackTrace();
        }

        return "";
    }
}
