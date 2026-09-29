-- Apply to an existing MySQL database after a backup. This migration preserves users and business rows.
CREATE TABLE IF NOT EXISTS roles (id BIGINT PRIMARY KEY AUTO_INCREMENT, code VARCHAR(32) NOT NULL UNIQUE);
CREATE TABLE IF NOT EXISTS permissions (id BIGINT PRIMARY KEY AUTO_INCREMENT, code VARCHAR(64) NOT NULL UNIQUE);
CREATE TABLE IF NOT EXISTS user_roles (user_id BIGINT NOT NULL, role_id BIGINT NOT NULL,
  PRIMARY KEY (user_id, role_id), FOREIGN KEY (user_id) REFERENCES users(id), FOREIGN KEY (role_id) REFERENCES roles(id));
CREATE TABLE IF NOT EXISTS role_permissions (role_id BIGINT NOT NULL, permission_id BIGINT NOT NULL,
  PRIMARY KEY (role_id, permission_id), FOREIGN KEY (role_id) REFERENCES roles(id), FOREIGN KEY (permission_id) REFERENCES permissions(id));
CREATE TABLE IF NOT EXISTS revoked_tokens (token_id VARCHAR(64) PRIMARY KEY, expires_at TIMESTAMP NOT NULL);
INSERT IGNORE INTO roles (code) VALUES ('user'),('merchant'),('admin');
INSERT IGNORE INTO permissions (code) VALUES ('profile:write'),('pet:write'),('shop:order'),('merchant:manage'),('admin:manage');
INSERT IGNORE INTO role_permissions (role_id,permission_id)
  SELECT r.id,p.id FROM roles r JOIN permissions p ON
    (r.code='user' AND p.code IN ('profile:write','pet:write','shop:order')) OR
    (r.code='merchant' AND p.code IN ('profile:write','merchant:manage')) OR
    (r.code='admin');
INSERT IGNORE INTO user_roles (user_id,role_id)
  SELECT u.id,r.id FROM users u JOIN roles r ON r.code=CASE WHEN u.role IN ('user','merchant','admin') THEN u.role ELSE 'user' END;
