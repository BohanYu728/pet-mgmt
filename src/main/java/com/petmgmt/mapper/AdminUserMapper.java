package com.petmgmt.mapper;

import com.petmgmt.model.User;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface AdminUserMapper {
    @Select("SELECT id,username,email,password_hash AS passwordHash,phone,role,is_approved AS isApproved,created_at AS createdAt FROM users ORDER BY id")
    List<User> all();

    @Select("SELECT id,username,email,password_hash AS passwordHash,phone,role,is_approved AS isApproved,created_at AS createdAt FROM users WHERE id=#{id}")
    User find(Long id);

    @Insert("INSERT INTO users (username,email,password_hash,phone,role,is_admin,is_approved,rent_amount,rent_due_day,created_at,updated_at) " +
            "VALUES (#{username},#{email},#{passwordHash},#{phone},#{role},#{isAdmin},#{isApproved},0,1,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)")
    @Options(useGeneratedKeys=true,keyProperty="id")
    int insert(User user);

    @Update("UPDATE users SET username=#{username},email=#{email},password_hash=#{passwordHash},phone=#{phone},role=#{role}, " +
            "is_admin=#{isAdmin},updated_at=CURRENT_TIMESTAMP WHERE id=#{id}")
    int update(User user);

    @Delete("DELETE FROM users WHERE id=#{id}")
    int delete(Long id);

    @Select("SELECT (SELECT COUNT(*) FROM pets WHERE user_id=#{id}) + (SELECT COUNT(*) FROM orders WHERE user_id=#{id}) + " +
            "(SELECT COUNT(*) FROM reviews WHERE user_id=#{id}) + (SELECT COUNT(*) FROM reminders WHERE user_id=#{id}) + " +
            "(SELECT COUNT(*) FROM cart_items WHERE user_id=#{id}) + " +
            "(SELECT COUNT(*) FROM health_records WHERE user_id=#{id}) + (SELECT COUNT(*) FROM consultations WHERE user_id=#{id}) + " +
            "(SELECT COUNT(*) FROM breed_history WHERE user_id=#{id}) + (SELECT COUNT(*) FROM emotion_history WHERE user_id=#{id}) + " +
            "(SELECT COUNT(*) FROM products WHERE merchant_id=#{id}) + (SELECT COUNT(*) FROM rental_records WHERE merchant_id=#{id})")
    long relatedCount(Long id);
}
