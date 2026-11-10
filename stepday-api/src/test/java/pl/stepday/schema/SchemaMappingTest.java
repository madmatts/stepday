package pl.stepday.schema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import pl.stepday.support.PostgresTestConfig;
import pl.stepday.template.BlockRole;
import pl.stepday.template.BlockTemplate;
import pl.stepday.template.BlockTemplateRepository;
import pl.stepday.template.BlockType;
import pl.stepday.template.OverduePolicy;
import pl.stepday.template.ScheduleRule;
import pl.stepday.template.ScheduleRuleRepository;
import pl.stepday.user.AppUser;
import pl.stepday.user.AppUserRepository;

/** Migracje Flyway na prawdziwym PostgreSQL + walidacja mapowań JPA (ddl-auto=validate). */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(PostgresTestConfig.class)
class SchemaMappingTest {

    @Autowired AppUserRepository users;
    @Autowired BlockTemplateRepository templates;
    @Autowired ScheduleRuleRepository rules;
    @Autowired JdbcTemplate jdbc;

    @Test
    void templateWithJsonConfigAndEnumsRoundTrips() {
        AppUser user = users.saveAndFlush(new AppUser(UUID.randomUUID()));
        BlockTemplate water = new BlockTemplate(user.getId(), "Woda", BlockType.COUNTER);
        water.setRole(BlockRole.ALL_DAY);
        water.setConfig(Map.of("target", 8, "unit", "szklanek", "step", 1, "threshold_pct", 80));
        water.setSortKey("a0");
        templates.saveAndFlush(water);
        rules.saveAndFlush(new ScheduleRule(water.getId(), LocalDate.of(2026, 11, 10), "FREQ=DAILY", null));

        BlockTemplate loaded = templates.findById(water.getId()).orElseThrow();
        assertThat(loaded.getType()).isEqualTo(BlockType.COUNTER);
        assertThat(loaded.getRole()).isEqualTo(BlockRole.ALL_DAY);
        assertThat(loaded.getOverduePolicy()).isEqualTo(OverduePolicy.DROP);
        assertThat(loaded.getConfig()).containsEntry("target", 8).containsEntry("unit", "szklanek");
        assertThat(jdbc.queryForObject("SELECT config->>'threshold_pct' FROM block_template WHERE id = ?",
                String.class, water.getId())).isEqualTo("80");
    }

    @Test
    void counterWithoutTargetViolatesCheck() {
        AppUser user = users.saveAndFlush(new AppUser(UUID.randomUUID()));
        BlockTemplate bad = new BlockTemplate(user.getId(), "Woda", BlockType.COUNTER);
        bad.setSortKey("a0");
        assertThatThrownBy(() -> templates.saveAndFlush(bad))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("counter_has_target");
    }

    @Test
    void carryCountIsLimitedToTwo() {
        UUID userId = users.saveAndFlush(new AppUser(UUID.randomUUID())).getId();
        UUID dayId = jdbc.queryForObject(
                "INSERT INTO day (user_id, local_date) VALUES (?, DATE '2026-11-10') RETURNING id", UUID.class, userId);
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO block_instance (day_id, carry_count, name, type, role, config, sort_key)
                VALUES (?, 3, 'Filtr', 'BOOLEAN', 'STEP', '{}', 'a0')""", dayId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
