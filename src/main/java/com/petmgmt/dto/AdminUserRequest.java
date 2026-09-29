package com.petmgmt.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminUserRequest(
    @NotBlank @Size(min=3,max=64) String username,
    @NotBlank @Email String email,
    @Size(min=6,max=128) String password,
    @Size(max=20) String phone,
    @NotBlank @Pattern(regexp="user|merchant|admin") String role
) {}
