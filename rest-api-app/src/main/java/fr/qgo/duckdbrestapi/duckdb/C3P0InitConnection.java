package fr.qgo.duckdbrestapi.duckdb;

import com.mchange.v2.c3p0.C3P0Registry;
import com.mchange.v2.c3p0.ConnectionCustomizer;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

@Slf4j
public class C3P0InitConnection implements ConnectionCustomizer {
    @Override
    public void onAcquire(Connection c, String pdsIdt) throws SQLException {
        String initSql = getInitSql(pdsIdt);
        if (initSql != null && !initSql.isBlank()) {
            log.debug("onAcquire({}) init connection with initSql", pdsIdt);
            try (Statement statement = c.createStatement()) {
                statement.execute(initSql);
            }
        } else {
            log.debug("onAcquire({}) init connection skip no initSql script provide", pdsIdt);
        }
    }

    private static String getInitSql(String pdsIdt) {
        return (String) C3P0Registry.extensionsForToken(pdsIdt).get("initSql");
    }

    @Override
    public void onDestroy(Connection c, String pdsIdt) {
        log.debug("onDestroy({})", pdsIdt);
    }

    @Override
    public void onCheckOut(Connection c, String pdsIdt) {
        log.debug("onCheckOut({})", pdsIdt);
    }

    @Override
    public void onCheckIn(Connection c, String pdsIdt) {
        log.debug("onCheckIn({})", pdsIdt);
    }
}
