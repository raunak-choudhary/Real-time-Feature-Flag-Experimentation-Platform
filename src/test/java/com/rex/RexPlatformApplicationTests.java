package com.rex;

import static org.assertj.core.api.Assertions.assertThat;

import com.rex.support.PostgresIntegrationTest;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Proves the Flyway migrations and the JPA entities agree.
 *
 * <p>The context runs with ddl-auto=validate, so if a migration and an entity ever drift apart the
 * context fails to start and every test in the suite fails with it.
 */
class RexPlatformApplicationTests extends PostgresIntegrationTest {

  @Autowired private DataSource dataSource;

  @Test
  @DisplayName("context starts against Postgres with Hibernate validating the Flyway schema")
  void contextLoads() {
    assertThat(dataSource).isNotNull();
  }

  @Test
  @DisplayName("every migration applied successfully")
  void migrationsApplied() throws Exception {
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement();
        ResultSet rs =
            statement.executeQuery(
                "select count(*) from flyway_schema_history where success = false")) {
      assertThat(rs.next()).isTrue();
      assertThat(rs.getInt(1)).as("failed migrations").isZero();
    }
  }

  @Test
  @DisplayName("index names are unique per schema, which Postgres requires")
  void indexNamesAreUnique() throws Exception {
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement();
        ResultSet rs =
            statement.executeQuery(
                "select count(*) from (select indexname from pg_indexes"
                    + " where schemaname = 'public' group by indexname having count(*) > 1) dupes")) {
      assertThat(rs.next()).isTrue();
      assertThat(rs.getInt(1)).as("duplicate index names").isZero();
    }
  }

  @Test
  @DisplayName("user_cohorts keeps its three indexes")
  void userCohortIndexesExist() throws Exception {
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement();
        ResultSet rs =
            statement.executeQuery(
                "select count(*) from pg_indexes where schemaname = 'public'"
                    + " and tablename = 'user_cohorts' and indexname like 'idx_%'")) {
      assertThat(rs.next()).isTrue();
      assertThat(rs.getInt(1)).as("user_cohorts indexes").isEqualTo(3);
    }
  }
}
