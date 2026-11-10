package org.example.common.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPrincipal {
    private Long id;
    private String email;
    private String phone;
    private String role;
    private String fullName;
}

