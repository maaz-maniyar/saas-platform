package com.maaz.saasPlatform.tenant;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantRepo extends JpaRepository<Project, Long> {}
