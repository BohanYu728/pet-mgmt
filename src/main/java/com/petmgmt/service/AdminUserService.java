package com.petmgmt.service;

import com.petmgmt.dto.AdminUserRequest;
import com.petmgmt.mapper.AdminUserMapper;
import com.petmgmt.model.User;
import com.petmgmt.security.RoleAssignmentService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class AdminUserService {
    private final AdminUserMapper users;
    private final RoleAssignmentService roles;
    private final PasswordEncoder passwords;
    public AdminUserService(AdminUserMapper users,RoleAssignmentService roles,PasswordEncoder passwords) {
        this.users=users; this.roles=roles; this.passwords=passwords;
    }
    public List<User> all() { return users.all(); }
    public User find(Long id) { return users.find(id); }

    @Transactional
    public User create(AdminUserRequest request) {
        if (request.password()==null || request.password().isBlank()) throw new IllegalArgumentException("密码不能为空");
        User user=copy(request,new User());
        user.setIsApproved(true);
        users.insert(user);
        roles.assign(user.getId(),user.getRole());
        return users.find(user.getId());
    }

    @Transactional
    public User update(Long id,AdminUserRequest request) {
        User user=users.find(id);
        if (user==null) return null;
        copy(request,user);
        users.update(user);
        roles.assign(id,user.getRole());
        return users.find(id);
    }

    @Transactional
    public boolean delete(Long id,Long actingUserId) {
        if (users.find(id)==null) return false;
        if (id.equals(actingUserId)) throw new IllegalArgumentException("不能删除自己");
        if (users.relatedCount(id)>0) throw new IllegalStateException("用户仍有关联业务数据，不能删除");
        roles.clear(id);
        users.delete(id);
        return true;
    }

    private User copy(AdminUserRequest req,User user) {
        user.setUsername(req.username()); user.setEmail(req.email()); user.setPhone(req.phone());
        user.setRole(req.role()); user.setIsAdmin("admin".equals(req.role()));
        if (req.password()!=null && !req.password().isBlank()) user.setPasswordHash(passwords.encode(req.password()));
        return user;
    }
}
