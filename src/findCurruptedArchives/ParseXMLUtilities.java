package findCurruptedArchives;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;

/**
 * Created by anatoliyvazhenin on 10/20/14.
 */
public class ParseXMLUtilities {

    File xmlFile;
    NodeList nodelist;

    public ParseXMLUtilities() {

    }

    public ParseXMLUtilities(String xmlFilePath) {
        this.xmlFile = new File(xmlFilePath);
    }

    void initiate() {

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(this.xmlFile);
            nodelist = document.getDocumentElement().getChildNodes();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    NodeList getChildNodes(String nodeName) {
        return recursiveNodes(this.nodelist, nodeName);
    }

    // get child nodes
    NodeList recursiveNodes(NodeList nodes, String nodeName) {

        for (int i = 0; i < nodes.getLength(); i++) {
            if (nodes.item(i).getBaseURI() != null) {
                NodeList list = null;
                if (nodes.item(i).getNodeName().equalsIgnoreCase(nodeName)) {
                    list = nodes.item(i).getChildNodes();
                    return list;
                } else {
                    // try to search in child nodes if there are any nodes with other child nodes
                    NodeList nodeList = nodes.item(i).getChildNodes();
                    for (int j = 0; j < nodeList.getLength(); j++) {
                        if (nodeList.item(j).getChildNodes().getLength() > 1) {
                            return recursiveNodes(nodeList, nodeName);
                        }
                    }
                }
            }
        }
        return null;
    }

    // loop through node list searching for specific named node's value
    String getNodeValue(NodeList nodes, String nodeName) {
        try {
            for (int i = 0; i < nodes.getLength(); i++) {
                if (nodes.item(i).getNodeName().equalsIgnoreCase(nodeName)) {
                    return nodes.item(i).getChildNodes().item(0).getNodeValue();
                }
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    String[] getNodeArrayValues(String NodeName) {
        ArrayList<String> result = new ArrayList<String>();

        for (int i = 0; i < getChildNodes(NodeName).getLength(); i++) {
            if (getChildNodes(NodeName).item(i).getBaseURI() != null) {
                result.add(getChildNodes(NodeName).item(i).getChildNodes().item(0).getNodeValue());
            }
        }
        return result.toArray(new String[result.size()]);
    }

}
