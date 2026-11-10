package pl.stepday.day;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.stepday.cache.DayCache;
import pl.stepday.common.NotFoundException;
import pl.stepday.day.DayDtos.BlockView;
import pl.stepday.day.DayDtos.Board;
import pl.stepday.day.DayDtos.DayView;
import pl.stepday.day.DayDtos.ItemView;
import pl.stepday.day.DayDtos.Progress;
import pl.stepday.day.DayDtos.WeekDay;
import pl.stepday.schedule.ScheduleService;
import pl.stepday.template.BlockConfig;
import pl.stepday.template.BlockRole;
import pl.stepday.template.BlockType;
import pl.stepday.template.ScheduleRuleRepository;
import pl.stepday.user.AppUser;

@Service
public class DayService {

    private final DayGenerator generator;
    private final DayRepository days;
    private final BlockInstanceRepository instances;
    private final InstanceItemRepository items;
    private final ScheduleRuleRepository rules;
    private final ScheduleService schedule;
    private final DayClock dayClock;
    private final DayCache cache;

    public DayService(DayGenerator generator, DayRepository days, BlockInstanceRepository instances,
                      InstanceItemRepository items, ScheduleRuleRepository rules, ScheduleService schedule,
                      DayClock dayClock, DayCache cache) {
        this.generator = generator;
        this.days = days;
        this.instances = instances;
        this.items = items;
        this.rules = rules;
        this.schedule = schedule;
        this.dayClock = dayClock;
        this.cache = cache;
    }

    /** {@code GET /days/{date}}: ensureDay + widok. Dni przyszłe nie są generowane (A14). */
    @Transactional
    public DayView view(AppUser user, LocalDate date) {
        LocalDate today = dayClock.today(user);
        if (date.isAfter(today)) {
            throw new IllegalArgumentException("Dzień " + date + " jeszcze się nie zaczął");
        }
        return cache.get(user.getId(), date).orElseGet(() -> {
            UUID dayId = generator.ensureDay(user.getId(), date);
            recomputeStatus(dayId);
            DayView view = build(user.getId(), days.findById(dayId).orElseThrow(), today);
            cache.put(user.getId(), date, view);
            return view;
        });
    }

    /** Przelicza {@code day.status} z instancji; wołane po każdym zapisie. */
    @Transactional
    public DayStatus recomputeStatus(UUID dayId) {
        Day day = days.findById(dayId).orElseThrow(() -> new NotFoundException("dzień " + dayId));
        List<BlockInstance> list = instances.findByDayIdOrderBySortKey(dayId);
        Map<UUID, List<InstanceItem>> its = itemsOf(list);
        List<Completion> completions = list.stream()
                .map(bi -> BlockEvaluator.completion(bi, its.getOrDefault(bi.getId(), List.of())))
                .toList();
        DayStatus status = BlockEvaluator.dayStatus(completions, day.isClosed());
        day.setStatus(status);
        cache.evict(day.getUserId(), day.getLocalDate());
        return status;
    }

    private DayView build(UUID userId, Day day, LocalDate today) {
        List<BlockInstance> list = instances.findByDayIdOrderBySortKey(day.getId());
        Map<UUID, List<InstanceItem>> its = itemsOf(list);

        Comparator<BlockInstance> pathOrder = Comparator
                .comparing((BlockInstance bi) -> bi.getDeferredAt() != null) // „Później” na koniec ścieżki
                .thenComparing(bi -> bi.getDeferredAt() == null ? java.time.Instant.EPOCH : bi.getDeferredAt())
                .thenComparing(BlockInstance::getSortKey);

        List<BlockView> steps = list.stream()
                .filter(bi -> bi.getRole() == BlockRole.STEP && bi.getState() != InstanceState.CARRIED)
                .sorted(pathOrder)
                .map(bi -> toView(bi, its.getOrDefault(bi.getId(), List.of())))
                .toList();
        List<BlockView> allDay = list.stream()
                .filter(bi -> bi.getRole() == BlockRole.ALL_DAY && bi.getState() != InstanceState.CARRIED)
                .map(bi -> toView(bi, its.getOrDefault(bi.getId(), List.of())))
                .toList();

        int done = (int) steps.stream().filter(s -> s.state() == InstanceState.DONE).count();
        return new DayView(day.getLocalDate(), day.getLocalDate().equals(today), day.getStatus(), day.isClosed(),
                new Progress(done, steps.size()), steps, allDay,
                week(userId, day.getLocalDate(), today), board(userId, day.getLocalDate()), streak(userId, today));
    }

    private BlockView toView(BlockInstance bi, List<InstanceItem> its) {
        Map<String, Object> c = bi.getConfig();
        boolean counter = bi.getType() == BlockType.COUNTER;
        boolean fromCalendar = bi.getTemplateId() != null && rules.findById(bi.getTemplateId())
                .map(r -> !schedule.isEveryDay(r.getRrule()))
                .orElse(false);
        return new BlockView(
                bi.getId(), bi.getName(), bi.getType(), bi.getRole(), bi.getState(),
                BlockEvaluator.completion(bi, its),
                bi.getDeferredAt() != null, bi.getCarryCount(), fromCalendar,
                bi.getType() == BlockType.LIST
                        ? its.stream().map(i -> new ItemView(i.getId(), i.getLabel(), i.isChecked())).toList()
                        : null,
                counter ? bi.getValueInt() : null,
                counter ? BlockConfig.intValue(c, "target") : null,
                counter ? BlockConfig.threshold(c) : null,
                counter ? String.valueOf(c.getOrDefault("unit", "")) : null,
                counter ? BlockConfig.intValue(c, "step", 1) : null,
                bi.getType() == BlockType.TIMER ? BlockConfig.intValue(c, "minutes") : null,
                bi.getTimerStartedAt(), bi.getTimerStoppedAt(),
                bi.getType() == BlockType.NOTE ? bi.getNoteText() : null);
    }

    /** Pasek tygodnia (poniedziałek–niedziela). Dni przyszłe i nieistniejące mają status null. */
    private List<WeekDay> week(UUID userId, LocalDate date, LocalDate today) {
        LocalDate monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        Map<LocalDate, DayStatus> statuses = days
                .findByUserIdAndLocalDateBetweenOrderByLocalDate(userId, monday, monday.plusDays(6)).stream()
                .collect(Collectors.toMap(Day::getLocalDate, Day::getStatus));
        List<WeekDay> week = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate d = monday.plusDays(i);
            week.add(new WeekDay(d, statuses.get(d), d.equals(today)));
        }
        return week;
    }

    /** D1: pionek stoi na polu (zaliczone dni miesiąca do {@code date} włącznie) + 1. */
    private Board board(UUID userId, LocalDate date) {
        YearMonth ym = YearMonth.from(date);
        long completed = days.findByUserIdAndLocalDateBetweenOrderByLocalDate(userId, ym.atDay(1), date).stream()
                .filter(d -> d.getStatus().isCompleted())
                .count();
        int total = ym.lengthOfMonth();
        return new Board((int) Math.min(completed + 1, total), total, ym.getYear(), ym.getMonthValue());
    }

    /** D3: seria dni zaliczonych; przerywa ją tylko MISSED (lub brak wiersza). Dzisiejszy OPEN nie przerywa. */
    public int streak(UUID userId, LocalDate today) {
        Map<LocalDate, DayStatus> statuses = days
                .findByUserIdAndLocalDateBetweenOrderByLocalDate(userId, today.minusYears(1), today).stream()
                .collect(Collectors.toMap(Day::getLocalDate, Day::getStatus));
        int streak = 0;
        DayStatus todayStatus = statuses.get(today);
        if (todayStatus != null && todayStatus.isCompleted()) {
            streak++;
        }
        for (LocalDate d = today.minusDays(1); ; d = d.minusDays(1)) {
            DayStatus s = statuses.get(d);
            if (s == null || !s.isCompleted()) {
                return streak;
            }
            streak++;
        }
    }

    private Map<UUID, List<InstanceItem>> itemsOf(List<BlockInstance> list) {
        Map<UUID, List<InstanceItem>> map = new HashMap<>();
        if (list.isEmpty()) {
            return map;
        }
        for (InstanceItem i : items.findByInstanceIdInOrderBySortKey(list.stream().map(BlockInstance::getId).toList())) {
            map.computeIfAbsent(i.getInstanceId(), k -> new ArrayList<>()).add(i);
        }
        return map;
    }
}
