package com.maaz.saasPlatform.bugs.repository;

import com.maaz.saasPlatform.bugs.BugSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BugSessionRepository extends JpaRepository<BugSession, UUID> {
}
