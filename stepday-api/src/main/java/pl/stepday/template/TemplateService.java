package pl.stepday.template;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.stepday.common.NotFoundException;
import pl.stepday.common.SortKeys;
import pl.stepday.schedule.ScheduleService;
import pl.stepday.template.TemplateDtos.ItemDto;
import pl.stepday.template.TemplateDtos.ScheduleDto;
import pl.stepday.template.TemplateDtos.TemplateRequest;
import pl.stepday.template.TemplateDtos.TemplateResponse;

@Service
public class TemplateService {

    private final BlockTemplateRepository templates;
    private final TemplateItemRepository items;
    private final ScheduleRuleRepository rules;
    private final ScheduleService schedule;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public TemplateService(BlockTemplateRepository templates, TemplateItemRepository items,
                           ScheduleRuleRepository rules, ScheduleService schedule,
                           ApplicationEventPublisher events, Clock clock) {
        this.templates = templates;
        this.items = items;
        this.rules = rules;
        this.schedule = schedule;
        this.events = events;
        this.clock = clock;
    }

    /** Zmiana szablonu; słuchacz w module dnia obsługuje „zastosuj także dziś”. */
    public record TemplateChanged(UUID userId, UUID templateId, boolean applyToday) {
    }

    @Transactional
    public TemplateResponse create(UUID userId, TemplateRequest req) {
        schedule.parse(req.schedule().rrule());
        BlockTemplate t = new BlockTemplate(userId, req.name().trim(), req.type());
        apply(t, req);
        t.setSortKey(sortKeyAfter(userId, req.afterTemplateId(), null));
        templates.save(t);
        rules.save(new ScheduleRule(t.getId(), req.schedule().dtstart(), req.schedule().rrule(), req.schedule().until()));
        syncItems(t, req.items());
        events.publishEvent(new TemplateChanged(userId, t.getId(), req.applyTodayOrFalse()));
        return toResponse(t);
    }

    @Transactional
    public TemplateResponse update(UUID userId, UUID id, TemplateRequest req) {
        schedule.parse(req.schedule().rrule());
        BlockTemplate t = owned(userId, id);
        t.setName(req.name().trim());
        t.setType(req.type());
        apply(t, req);
        if (req.afterTemplateId() != null && !req.afterTemplateId().equals(id)) {
            t.setSortKey(sortKeyAfter(userId, req.afterTemplateId(), id));
        }
        t.touch(clock.instant());
        ScheduleRule rule = rules.findById(id).orElseThrow();
        rule.update(req.schedule().dtstart(), req.schedule().rrule(), req.schedule().until());
        syncItems(t, req.items());
        events.publishEvent(new TemplateChanged(userId, t.getId(), req.applyTodayOrFalse()));
        return toResponse(t);
    }

    @Transactional(readOnly = true)
    public TemplateResponse get(UUID userId, UUID id) {
        return toResponse(owned(userId, id));
    }

    @Transactional(readOnly = true)
    public List<TemplateResponse> list(UUID userId) {
        return templates.findByUserIdAndArchivedAtIsNullOrderBySortKey(userId).stream().map(this::toResponse).toList();
    }

    /** DELETE archiwizuje — historia (instancje) zostaje nietknięta. */
    @Transactional
    public void archive(UUID userId, UUID id) {
        owned(userId, id).archive(clock.instant());
    }

    /** Nowa kolejność ścieżki: klucze nadawane po kolei w podanym porządku. */
    @Transactional
    public void reorder(UUID userId, List<UUID> orderedIds) {
        Map<UUID, BlockTemplate> byId = new HashMap<>();
        templates.findByUserIdAndArchivedAtIsNullOrderBySortKey(userId).forEach(t -> byId.put(t.getId(), t));
        String key = null;
        for (UUID id : orderedIds) {
            BlockTemplate t = byId.remove(id);
            if (t == null) {
                throw new NotFoundException("szablon " + id);
            }
            key = SortKeys.after(key);
            t.setSortKey(key);
        }
        // Szablony pominięte w żądaniu lądują na końcu, w dotychczasowej kolejności.
        for (BlockTemplate rest : byId.values().stream().sorted(Comparator.comparing(BlockTemplate::getSortKey)).toList()) {
            key = SortKeys.after(key);
            rest.setSortKey(key);
        }
    }

    public BlockTemplate owned(UUID userId, UUID id) {
        BlockTemplate t = templates.findByIdAndUserId(id, userId).orElseThrow(() -> new NotFoundException("szablon " + id));
        if (t.getArchivedAt() != null) {
            throw new NotFoundException("szablon " + id);
        }
        return t;
    }

    private void apply(BlockTemplate t, TemplateRequest req) {
        t.setRole(req.role() != null ? req.role() : BlockRole.STEP);
        t.setConfig(BlockConfig.normalize(req.type(), req.config()));
        t.setOverduePolicy(req.overduePolicy() != null
                ? req.overduePolicy()
                : schedule.defaultOverduePolicy(req.schedule().rrule()));
        t.setReminderTime(req.reminderTime());
    }

    private String sortKeyAfter(UUID userId, UUID afterId, UUID selfId) {
        List<BlockTemplate> ordered = templates.findByUserIdAndArchivedAtIsNullOrderBySortKey(userId).stream()
                .filter(t -> !t.getId().equals(selfId))
                .toList();
        if (ordered.isEmpty()) {
            return SortKeys.after(null);
        }
        if (afterId == null) {
            return SortKeys.after(ordered.getLast().getSortKey());
        }
        for (int i = 0; i < ordered.size(); i++) {
            if (ordered.get(i).getId().equals(afterId)) {
                String next = i + 1 < ordered.size() ? ordered.get(i + 1).getSortKey() : null;
                return SortKeys.between(ordered.get(i).getSortKey(), next);
            }
        }
        throw new NotFoundException("szablon " + afterId);
    }

    /** Pozycje z id są aktualizowane, nowe dodawane, brakujące archiwizowane. */
    private void syncItems(BlockTemplate t, List<ItemDto> requested) {
        List<TemplateItem> current = new ArrayList<>(items.findByTemplateIdAndArchivedAtIsNullOrderBySortKey(t.getId()));
        if (t.getType() != BlockType.LIST) {
            current.forEach(i -> i.archive(clock.instant()));
            return;
        }
        List<ItemDto> wanted = requested == null ? List.of() : requested;
        Map<UUID, TemplateItem> byId = new HashMap<>();
        current.forEach(i -> byId.put(i.getId(), i));
        String key = null;
        for (ItemDto dto : wanted) {
            key = SortKeys.after(key);
            TemplateItem existing = dto.id() == null ? null : byId.remove(dto.id());
            if (existing != null) {
                existing.setLabel(dto.label().trim());
                existing.setSortKey(key);
            } else {
                items.save(new TemplateItem(t.getId(), dto.label().trim(), key));
            }
        }
        byId.values().forEach(i -> i.archive(clock.instant()));
    }

    private TemplateResponse toResponse(BlockTemplate t) {
        ScheduleRule rule = rules.findById(t.getId()).orElse(null);
        List<ItemDto> its = items.findByTemplateIdAndArchivedAtIsNullOrderBySortKey(t.getId()).stream()
                .map(i -> new ItemDto(i.getId(), i.getLabel())).toList();
        return new TemplateResponse(t.getId(), t.getType(), t.getRole(), t.getName(), t.getConfig(), its,
                rule == null ? null : new ScheduleDto(rule.getRrule(), rule.getDtstart(), rule.getUntil()),
                t.getOverduePolicy(), t.getReminderTime(), t.getSortKey());
    }

}
