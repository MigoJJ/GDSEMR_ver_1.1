package com.emr.gds.main.medication.db;

import com.emr.gds.main.medication.model.MedicationGroup;
import com.emr.gds.main.medication.model.MedicationItem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseManagerTest {

    private static final String TEST_DATA_FILE = "test_med_data.xml";

    @AfterEach
    void tearDown() {
        File file = new File(TEST_DATA_FILE);
        if (file.exists()) {
            file.delete();
        }
    }

    @Test
    void testPersistence() {
        // 1. Initial Load with TEST file
        DatabaseManager db1 = new DatabaseManager(TEST_DATA_FILE);
        Map<String, List<MedicationGroup>> data1 = db1.getMedicationData();
        assertNotNull(data1, "Data should be loaded");
        assertFalse(data1.isEmpty(), "Data should not be empty (loaded from resource)");
        
        // Pick a category to add to - check first key
        String category = db1.getOrderedCategories().get(0);
        List<MedicationGroup> catGroups = data1.get(category);
        String group = catGroups.get(0).title();
        String newItemText = "Test Item 123";
        
        // 2. Add Item
        db1.addItem(category, group, new MedicationItem(newItemText));
        assertTrue(db1.hasPendingChanges(), "Should have pending changes");
        
        // 3. Commit
        db1.commitPending();
        assertFalse(db1.hasPendingChanges(), "Should not have pending changes after commit");
        
        File file = new File(TEST_DATA_FILE);
        assertTrue(file.exists(), "Storage file should be created at " + file.getAbsolutePath());

        // 4. Reload (Simulate App Restart)
        DatabaseManager db2 = new DatabaseManager(TEST_DATA_FILE);
        Map<String, List<MedicationGroup>> data2 = db2.getMedicationData();
        
        // 5. Verify
        List<MedicationGroup> groups = data2.get(category);
        assertNotNull(groups, "Groups should be loaded");
        
        boolean found = false;
        for (MedicationGroup g : groups) {
            if (g.title().equals(group)) {
                for (MedicationItem item : g.medications()) {
                    if (item.getText().equals(newItemText)) {
                        found = true;
                        break;
                    }
                }
            }
        }
        
        assertTrue(found, "New item should be persisted and reloaded");
    }
}