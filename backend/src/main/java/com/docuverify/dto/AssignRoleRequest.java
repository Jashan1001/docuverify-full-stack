package com.docuverify.dto;

import com.docuverify.enums.Role;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class AssignRoleRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotNull(message = "Role is required")
    private Role role;

    // Required when assigning ROLE_VERIFIER or ROLE_INSTITUTION_ADMIN
    private UUID institutionId;
}
