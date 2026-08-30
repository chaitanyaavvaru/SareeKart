package com.sareekart.dto.auth;

import com.sareekart.entity.User;
import com.sareekart.entity.enums.RoleName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private UserDto user;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserDto {
        private Long id;
        private String email;
        private String firstName;
        private String lastName;
        private String phone;
        private boolean enabled;
        private boolean emailVerified;
        private Set<String> roles;

        public static UserDto from(User user) {
            return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .enabled(user.getEnabled())
                .emailVerified(user.getEmailVerified())
                .roles(user.getRoles().stream()
                    .map(r -> r.getName().name())
                    .collect(Collectors.toSet()))
                .build();
        }
    }

    public static AuthResponse from(String token, User user) {
        return AuthResponse.builder()
            .token(token)
            .user(UserDto.from(user))
            .build();
    }
}