package pl.stepday.week;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.stepday.day.Day;
import pl.stepday.day.DayClock;
import pl.stepday.day.DayRepository;
import pl.stepday.day.DayStatus;
import pl.stepday.day.InstanceState;
import pl.stepday.template.BlockConfig;
import pl.stepday.template.BlockType;
import pl.stepday.user.AppUser;
import tools.jackson.databind.json.JsonMapper;

/**
 * Podsumowanie tygodnia jest wyliczane, nie przechowywane (dokument, sekcje 2 i 5).
 * SUCCESS tylko gdy wszystkie 7 dni ma status FULL; inaczej GAPS z listą „co dokładnie”.
 */
@Service
public class WeekSummaryService {

    /** Zapytanie z dokumentu (sekcja 5) + {@code bi.config} dla jednostki i progu licznika. */
    static final String GAPS_SQL = """
            SELECT d.local_date, d.status, bi.id AS instance_id, bi.name, bi.type, bi.state,
                   bi.value_int, (bi.config->>'target')::int AS target, bi.config::text AS config,
                   array_agg(ii.label ORDER BY ii.sort_key)
                     FILTER (WHERE ii.checked_at IS NULL)     AS unchecked,
                   array_agg(ii.label ORDER BY ii.sort_key)
                     FILTER (WHERE ii.checked_at IS NOT NULL) AS checked
            FROM day d
            JOIN block_instance bi ON bi.day_id = d.id
            LEFT JOIN instance_item ii ON ii.instance_id = bi.id
            WHERE d.user_id = :userId
              AND d.local_date BETWEEN :weekStart AND :weekEnd
              AND d.status IN ('PARTIAL','MISSED')
              AND (bi.state NOT IN ('DONE','CARRIED')
                   OR (bi.type = 'COUNTER' AND bi.value_int < (bi.config->>'target')::int))
            GROUP BY d.local_date, d.status, bi.id
            ORDER BY d.local_date, bi.sort_key""";

    public enum Outcome { SUCCESS, GAPS }

    public record DaySummary(LocalDate date, DayStatus status) {
    }

    public record GapBlock(UUID instanceId, String name, BlockType type, InstanceState state,
                           Integer value, Integer target, Integer threshold, String unit,
                           List<String> unchecked, List<String> checked) {
    }

    public record GapDay(LocalDate date, DayStatus status, List<GapBlock> blocks) {
    }

    public record Counts(int full, int partial, int missed, int completed) {
    }

    /**
     * @param finished false, gdy tydzień jeszcze trwa (dni przyszłe/otwarte mają status null).
     */
    public record WeekSummary(LocalDate weekStart, LocalDate weekEnd, Outcome outcome, boolean finished,
                              Counts counts, List<DaySummary> days, List<GapDay> gaps) {
    }

    private final NamedParameterJdbcTemplate jdbc;
    private final DayRepository days;
    private final DayClock dayClock;
    private final JsonMapper json;

    public WeekSummaryService(NamedParameterJdbcTemplate jdbc, DayRepository days, DayClock dayClock, JsonMapper json) {
        this.jdbc = jdbc;
        this.days = days;
        this.dayClock = dayClock;
        this.json = json;
    }

    @Transactional(readOnly = true)
    public WeekSummary summary(AppUser user, LocalDate weekStart) {
        if (weekStart.getDayOfWeek() != DayOfWeek.MONDAY) {
            throw new IllegalArgumentException("Tydzień zaczyna się w poniedziałek, a " + weekStart + " nim nie jest");
        }
        LocalDate weekEnd = weekStart.plusDays(6);
        LocalDate today = dayClock.today(user);

        Map<LocalDate, Day> rows = new LinkedHashMap<>();
        days.findByUserIdAndLocalDateBetweenOrderByLocalDate(user.getId(), weekStart, weekEnd)
                .forEach(d -> rows.put(d.getLocalDate(), d));

        List<DaySummary> daySummaries = new ArrayList<>();
        for (LocalDate d = weekStart; !d.isAfter(weekEnd); d = d.plusDays(1)) {
            Day row = rows.get(d);
            DayStatus status;
            if (row != null) {
                status = row.getStatus() == DayStatus.OPEN ? null : row.getStatus();
            } else {
                // Dzień bez wiersza w day liczy się jako niezaliczony (jeśli już minął).
                status = d.isBefore(today) ? DayStatus.MISSED : null;
            }
            daySummaries.add(new DaySummary(d, status));
        }

        int full = count(daySummaries, DayStatus.FULL);
        int partial = count(daySummaries, DayStatus.PARTIAL);
        int missed = count(daySummaries, DayStatus.MISSED);
        boolean finished = daySummaries.stream().allMatch(s -> s.status() != null);
        Outcome outcome = full == 7 ? Outcome.SUCCESS : Outcome.GAPS;

        Map<LocalDate, GapDay> gapDays = new LinkedHashMap<>();
        jdbc.query(GAPS_SQL, new MapSqlParameterSource()
                .addValue("userId", user.getId())
                .addValue("weekStart", weekStart)
                .addValue("weekEnd", weekEnd), (ResultSet rs) -> {
            LocalDate date = rs.getObject("local_date", LocalDate.class);
            GapDay gd = gapDays.computeIfAbsent(date,
                    k -> new GapDay(k, safeStatus(rs), new ArrayList<>()));
            gd.blocks().add(toBlock(rs));
        });
        // Dni bez wiersza (aplikacja nieotwarta) też są brakiem, choć nie wiemy „co dokładnie”.
        for (DaySummary s : daySummaries) {
            if (s.status() == DayStatus.MISSED && !rows.containsKey(s.date())) {
                gapDays.put(s.date(), new GapDay(s.date(), DayStatus.MISSED, List.of()));
            }
        }
        List<GapDay> gaps = gapDays.values().stream().sorted(java.util.Comparator.comparing(GapDay::date)).toList();

        return new WeekSummary(weekStart, weekEnd, outcome, finished,
                new Counts(full, partial, missed, full + partial), daySummaries, gaps);
    }

    private GapBlock toBlock(ResultSet rs) throws SQLException {
        BlockType type = BlockType.valueOf(rs.getString("type"));
        boolean counter = type == BlockType.COUNTER;
        Map<String, Object> config = counter ? json.readValue(rs.getString("config"), Map.class) : Map.of();
        return new GapBlock(
                rs.getObject("instance_id", UUID.class),
                rs.getString("name"),
                type,
                InstanceState.valueOf(rs.getString("state")),
                counter ? rs.getInt("value_int") : null,
                counter ? rs.getInt("target") : null,
                counter ? BlockConfig.threshold(config) : null,
                counter ? String.valueOf(config.getOrDefault("unit", "")) : null,
                type == BlockType.LIST ? labels(rs.getArray("unchecked")) : null,
                type == BlockType.LIST ? labels(rs.getArray("checked")) : null);
    }

    private static DayStatus safeStatus(ResultSet rs) {
        try {
            return DayStatus.valueOf(rs.getString("status"));
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private static List<String> labels(Array array) throws SQLException {
        return array == null ? List.of() : Arrays.asList((String[]) array.getArray());
    }

    private static int count(List<DaySummary> list, DayStatus status) {
        return (int) list.stream().filter(s -> s.status() == status).count();
    }
}
