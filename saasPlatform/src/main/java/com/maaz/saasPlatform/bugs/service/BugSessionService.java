package com.maaz.saasPlatform.bugs.service;

import com.maaz.saasPlatform.bugs.BugSession;
import com.maaz.saasPlatform.bugs.BugStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.maaz.saasPlatform.bugs.repository.BugSessionRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BugSessionService {

    private final BugSessionRepository repository;

    public BugSession createBug(
            String title,
            String description,
            String createdBy,
            String assignedTo
    ) {
        BugSession bug = new BugSession();
        bug.setTitle(title);
        bug.setDescription(description);
        bug.setStatus(BugStatus.OPEN);
        bug.setCreatedBy(createdBy);
        bug.setAssignedTo(assignedTo);

        return repository.save(bug);
    }

    public List<BugSession> getAllBugs() {
        return repository.findAll();
    }

    public BugSession getBugById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bug not found"));
    }

    public BugSession replaceBug(
            UUID id,
            String title,
            String description,
            BugStatus status,
            String assignedTo
    ) {
        BugSession bug = getBugById(id);
        bug.setTitle(title);
        bug.setDescription(description);
        bug.setStatus(status);
        bug.setAssignedTo(assignedTo);
        return repository.save(bug);
    }

    public BugSession patchBug(
            UUID id,
            String title,
            String description,
            BugStatus status,
            String assignedTo
    ) {
        BugSession bug = getBugById(id);

        if (title != null && !title.isBlank()) {
            bug.setTitle(title);
        }

        if (description != null) {
            bug.setDescription(description);
        }

        if (status != null) {
            bug.setStatus(status);
        }

        if (assignedTo != null) {
            bug.setAssignedTo(assignedTo);
        }

        return repository.save(bug);
    }
}
