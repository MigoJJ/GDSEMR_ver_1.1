package com.emr.gds.main.clinicalLab.db;

import com.emr.gds.main.clinicalLab.model.ClinicalLabItem;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.io.File;

public class ClinicalLabDatabase {
    
    private String getDbUrl() {
        String[] possiblePaths = {
            "app/db/ClinicalLabItemsSqlite3.db",
            "db/ClinicalLabItemsSqlite3.db",
            "../app/db/ClinicalLabItemsSqlite3.db"
        };

        for (String path : possiblePaths) {
            File file = new File(path);
            if (file.exists()) {
                // System.out.println("ClinicalLabDatabase: DB found at " + file.getAbsolutePath());
                return "jdbc:sqlite:" + file.getAbsolutePath();
            }
        }
        System.err.println("ClinicalLabDatabase: DB file not found! Defaulting to app/db/...");
        return "jdbc:sqlite:app/db/ClinicalLabItemsSqlite3.db";
    }

    public List<ClinicalLabItem> getAllItems() {
        List<ClinicalLabItem> items = new ArrayList<>();
        String sql = "SELECT * FROM clinical_lab_items";

        try (Connection conn = DriverManager.getConnection(getDbUrl());
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                items.add(mapResultSetToItem(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching lab items: " + e.getMessage());
        }
        return items;
    }

    public List<ClinicalLabItem> searchItems(String query) {
        List<ClinicalLabItem> items = new ArrayList<>();
        String sql = "SELECT * FROM clinical_lab_items WHERE test_name LIKE ? OR category LIKE ?";

        try (Connection conn = DriverManager.getConnection(getDbUrl());
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            String param = "%" + query + "%";
            pstmt.setString(1, param);
            pstmt.setString(2, param);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    items.add(mapResultSetToItem(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error searching lab items: " + e.getMessage());
        }
        return items;
    }

    private ClinicalLabItem mapResultSetToItem(ResultSet rs) throws SQLException {
        return new ClinicalLabItem(
            rs.getInt("id"),
            rs.getString("category"),
            rs.getString("test_name"),
            rs.getString("unit"),
            getObjectOrNull(rs, "male_range_low"),
            getObjectOrNull(rs, "male_range_high"),
            getObjectOrNull(rs, "female_range_low"),
            getObjectOrNull(rs, "female_range_high"),
            rs.getString("male_reference_range"),
            rs.getString("female_reference_range")
        );
    }

    private Double getObjectOrNull(ResultSet rs, String column) throws SQLException {
        double val = rs.getDouble(column);
        return rs.wasNull() ? null : val;
    }
}
