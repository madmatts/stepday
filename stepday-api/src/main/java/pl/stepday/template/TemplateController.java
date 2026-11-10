package pl.stepday.template;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pl.stepday.template.TemplateDtos.OrderRequest;
import pl.stepday.template.TemplateDtos.TemplateRequest;
import pl.stepday.template.TemplateDtos.TemplateResponse;
import pl.stepday.user.CurrentUser;

@RestController
@RequestMapping("/templates")
public class TemplateController {

    private final TemplateService service;
    private final CurrentUser user;

    public TemplateController(TemplateService service, CurrentUser user) {
        this.service = service;
        this.user = user;
    }

    @GetMapping
    public List<TemplateResponse> list() {
        return service.list(user.id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TemplateResponse create(@Valid @RequestBody TemplateRequest req) {
        return service.create(user.id(), req);
    }

    @GetMapping("/{id}")
    public TemplateResponse get(@PathVariable UUID id) {
        return service.get(user.id(), id);
    }

    @PutMapping("/{id}")
    public TemplateResponse update(@PathVariable UUID id, @Valid @RequestBody TemplateRequest req) {
        return service.update(user.id(), id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.archive(user.id(), id);
    }

    @PutMapping("/order")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reorder(@Valid @RequestBody OrderRequest req) {
        service.reorder(user.id(), req.orderedIds());
    }
}
