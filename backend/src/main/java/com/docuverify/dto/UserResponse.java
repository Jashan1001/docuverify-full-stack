package com.docuverify.dto;

import com.docuverify.enums.Role;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class UserResponse {
    private UUID id;
    private String fullName;
    private String email;
    private Role role;
    private boolean enabled;
    private String institutionName;
    private UUID institutionId;
    private LocalDateTime createdAt;
}
