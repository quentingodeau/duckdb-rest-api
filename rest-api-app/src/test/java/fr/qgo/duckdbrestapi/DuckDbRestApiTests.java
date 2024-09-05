package fr.qgo.duckdbrestapi;

import fr.qgo.duckdbrestapi.testtools.DuckDbTestSetup;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.sql.SQLException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class DuckDbRestApiTests extends DuckDbTestSetup {
    @Autowired
    private MockMvc mockMvc;

    @BeforeAll
    static void setup() throws SQLException, IOException {
        setupDbContext();
    }

    @AfterAll
    static void cleanup() {
        cleanUpDbContext();
    }

    @DynamicPropertySource
    static void mockDuckPath(DynamicPropertyRegistry registry) {
        registry.add("app.duckDbConfig.jdbcUrl.", () -> "jdbc:duckdb:" + dbFile);
        registry.add("testParquetPath", () -> parquet);
    }

    @Test
    void test1QueryPayloadMap() throws Exception {
        mockMvc.perform(post("/api/v1/queries/test1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id": 1}
                                """
                        ))
                .andExpect(request().asyncStarted())
                .andDo(MvcResult::getAsyncResult)
                .andExpect(status().isOk())
                .andExpect(content().string("""
                        {"str":"a"}
                        """));
    }

    @Test
    void test2QueryParquetWithDefineStruct() throws Exception {
        mockMvc.perform(post("/api/v1/queries/test2")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "str": "b",
                            "id": 3
                        }
                        """
                ))
                .andExpect(request().asyncStarted())
                .andDo(MvcResult::getAsyncResult)
                .andExpect(status().isOk())
                .andExpect(content().string("""
                        {"id":3,"str":"b","s":{"i":11,"j":"val"},"m":{"21":"2key","11":"1key","31":"3key"},"l":["c","z"]}
                        """));
    }

    @Test
    void test3QueryWithUserDefineElements() throws Exception {
        mockMvc.perform(post("/api/v1/queries/test3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {"id": 1}
                        """
                        ))
                .andExpect(request().asyncStarted())
                .andDo(MvcResult::getAsyncResult)
                .andExpect(status().isOk())
                .andExpect(content().string("""
                        {"id":1,"str":"a"}
                        """));
    }
}
