
package com.example.sysc;

import oracle.jdbc.OracleConnection;
import oracle.jdbc.OracleStatement;
import oracle.jdbc.dcn.DatabaseChangeEvent;
import oracle.jdbc.dcn.DatabaseChangeListener;
import oracle.jdbc.dcn.DatabaseChangeRegistration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

@Service
public class SyscDcnService {

    @Autowired
    private DataSource dataSource;

    private volatile String currentMode = "DAY";

    private OracleConnection dcnConnection;

    @PostConstruct
    public void init() {
        try {
            registerForChangeNotification();
            refreshSysc();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void registerForChangeNotification() throws Exception {

        Connection connection = dataSource.getConnection();
        dcnConnection = connection.unwrap(OracleConnection.class);

        Properties prop = new Properties();
        prop.setProperty(OracleConnection.DCN_NOTIFY_ROWIDS, "true");

        DatabaseChangeRegistration dcr =
                dcnConnection.registerDatabaseChangeNotification(prop);

        dcr.addListener(new DatabaseChangeListener() {
            @Override
            public void onDatabaseChangeNotification(DatabaseChangeEvent event) {
                System.out.println("SYSC table changed! Refreshing...");
                refreshSysc();
            }
        });

        Statement stmt = dcnConnection.createStatement();
        ((OracleStatement) stmt).setDatabaseChangeRegistration(dcr);

        ResultSet rs = stmt.executeQuery(
                "SELECT SYSC_VARIABLE FROM SYSC_TABLE"
        );

        while (rs.next()) {}

        rs.close();
        stmt.close();

        System.out.println("DCN Registered Successfully for SYSC_TABLE");
    }

    private synchronized void refreshSysc() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT SYSC_VARIABLE FROM SYSC_TABLE"
             )) {

            if (rs.next()) {

                String syscVar = rs.getString(1);

                String mode = syscVar.substring(10, 11);
                String refCheck = syscVar.substring(74, 75);

                if ("D".equals(mode)) {
                    currentMode = "DAY";
                } else if ("N".equals(mode) && "r".equalsIgnoreCase(refCheck)) {
                    currentMode = "NIGHT_REF";
                } else {
                    currentMode = "NIGHT_DAY";
                }

                System.out.println("SYSC Mode Updated To: " + currentMode);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getCurrentMode() {
        return currentMode;
    }
}
