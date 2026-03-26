package com.maaz.saasPlatform.tenant;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/projects")
public class ProjectController {

    private final TenantRepo repo;

    public ProjectController(TenantRepo repo) {
        this.repo = repo;
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Project create(@RequestParam String name) {
        return repo.save(new Project(name));
    }

    @GetMapping
    public List<Project> all() {
        return repo.findAll();
    }
}
