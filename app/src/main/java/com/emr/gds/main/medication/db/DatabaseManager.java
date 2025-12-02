package com.emr.gds.main.medication.db;

import com.emr.gds.main.medication.model.MedicationGroup;
import com.emr.gds.main.medication.model.MedicationItem;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.InputStream;
import java.util.*;

public class DatabaseManager {
    private static final String DEFAULT_DATA_FILE_NAME = "med_data_storage.xml";
    private final String dataFileName;
    private boolean pendingChanges = false;
    private Map<String, List<MedicationGroup>> cachedData = null;
    private List<String> cachedCategories = null;

    public DatabaseManager() {
        this(DEFAULT_DATA_FILE_NAME);
    }

    public DatabaseManager(String dataFileName) {
        this.dataFileName = dataFileName;
    }

    public void createTables() {
        // Mock implementation
    }

    public void ensureSeedData() {
        // Mock implementation
    }

    public List<String> getOrderedCategories() {
        if (cachedCategories == null) {
            loadData();
        }
        return cachedCategories;
    }

    public Map<String, List<MedicationGroup>> getMedicationData() {
        if (cachedData == null) {
            loadData();
        }
        return cachedData;
    }

    private void loadData() {
        cachedData = new HashMap<>();
        cachedCategories = new ArrayList<>();
        
        File externalFile = new File(this.dataFileName);
        
        if (externalFile.exists()) {
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.parse(externalFile);
                parseDocument(doc);
                return; // Loaded from file successfully
            } catch (Exception e) {
                System.err.println("Failed to load from " + this.dataFileName + ": " + e.getMessage());
                // Fallback to resource
            }
        }

        try (InputStream is = getClass().getResourceAsStream("/com/emr/gds/main/medication/med_data.xml")) {
            if (is == null) {
                System.err.println("Could not find med_data.xml resource");
                return;
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(is);
            parseDocument(doc);

        } catch (Exception e) {
            e.printStackTrace();
            // Fallback to empty if error
            cachedCategories = new ArrayList<>();
            cachedData = new HashMap<>();
        }
    }
    
    private void parseDocument(Document doc) {
        doc.getDocumentElement().normalize();

        NodeList catList = doc.getElementsByTagName("category");
        for (int i = 0; i < catList.getLength(); i++) {
            Node catNode = catList.item(i);
            if (catNode.getNodeType() == Node.ELEMENT_NODE) {
                Element catElement = (Element) catNode;
                String categoryName = catElement.getAttribute("name");
                cachedCategories.add(categoryName);

                List<MedicationGroup> groups = new ArrayList<>();
                NodeList groupList = catElement.getElementsByTagName("group");

                for (int j = 0; j < groupList.getLength(); j++) {
                    Node groupNode = groupList.item(j);
                    if (groupNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element groupElement = (Element) groupNode;
                        String groupName = groupElement.getAttribute("name");

                        List<MedicationItem> items = new ArrayList<>();
                        NodeList itemList = groupElement.getElementsByTagName("item");

                        for (int k = 0; k < itemList.getLength(); k++) {
                            Node itemNode = itemList.item(k);
                            if (itemNode.getNodeType() == Node.ELEMENT_NODE) {
                                String itemText = itemNode.getTextContent().trim();
                                items.add(new MedicationItem(itemText));
                            }
                        }
                        groups.add(new MedicationGroup(groupName, items));
                    }
                }
                cachedData.put(categoryName, groups);
            }
        }
    }

    public boolean hasPendingChanges() {
        return pendingChanges;
    }

    public void markDirty() {
        this.pendingChanges = true;
    }

    public void commitPending() {
        if (cachedCategories == null || cachedData == null) return;

        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            Document doc = docBuilder.newDocument();
            Element rootElement = doc.createElement("medications");
            doc.appendChild(rootElement);

            for (String categoryName : cachedCategories) {
                Element category = doc.createElement("category");
                category.setAttribute("name", categoryName);
                rootElement.appendChild(category);
                
                List<MedicationGroup> groups = cachedData.get(categoryName);
                if (groups != null) {
                    for (MedicationGroup group : groups) {
                        Element groupElem = doc.createElement("group");
                        groupElem.setAttribute("name", group.title());
                        category.appendChild(groupElem);
                        
                        for (MedicationItem item : group.medications()) {
                            Element itemElem = doc.createElement("item");
                            itemElem.setTextContent(item.getText());
                            groupElem.appendChild(itemElem);
                        }
                    }
                }
            }

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File(this.dataFileName));

            transformer.transform(source, result);

            pendingChanges = false;
            System.out.println("Changes saved to " + new File(this.dataFileName).getAbsolutePath());

        } catch (ParserConfigurationException | TransformerException e) {
            e.printStackTrace();
            System.err.println("Failed to save changes: " + e.getMessage());
        }
    }

    public void removeItem(MedicationItem item) {
        if (cachedData == null) return;
        for (List<MedicationGroup> groups : cachedData.values()) {
            for (MedicationGroup group : groups) {
                if (group.medications().remove(item)) {
                    markDirty();
                    return;
                }
            }
        }
    }

    public void addItem(String category, String groupName, MedicationItem item) {
        if (cachedData == null) return;
        List<MedicationGroup> groups = cachedData.get(category);
        if (groups != null) {
            for (MedicationGroup group : groups) {
                if (group.title().equals(groupName)) {
                    group.medications().add(item);
                    markDirty();
                    return;
                }
            }
        }
    }
}
