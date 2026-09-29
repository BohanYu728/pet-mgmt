package com.petmgmt.security;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoleAssignmentService {
    private final JdbcTemplate jdbc;
    public RoleAssignmentService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Transactional
    public void assign(Long userId, String role) {
        jdbc.update("DELETE FROM user_roles WHERE user_id=?", userId);
        jdbc.update("INSERT INTO user_roles (user_id,role_id) SELECT ?,id FROM roles WHERE code=?", userId, role);
    }

    @Transactional
    public void clear(Long userId) { jdbc.update("DELETE FROM user_roles WHERE user_id=?", userId); }
}
