package com.sareekart.dto.admin;

import com.sareekart.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Customer record with lifetime purchase aggregates for admin user management.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserResponse {
    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private long ordersCount;
    private BigDecimal totalSpent;
    private LocalDateTime joinedAt;
    private boolean enabled;

    /**
     * Maps a user together with its aggregate row produced by
     * OrderRepository.aggregateOrderStatsPerUser: [userId, orderCount, totalSpent].
     */
    public static AdminUserResponse from(User user, Object[] aggregate) {
        long ordersCount = aggregate != null && aggregate[1] != null ? ((Number) aggregate[1]).longValue() : 0L;
        BigDecimal totalSpent = aggregate != null && aggregate[2] != null
            ? new BigDecimal(aggregate[2].toString())
            : BigDecimal.ZERO;

        return AdminUserResponse.builder()
            .id(user.getId())
            .fullName(user.getFullName())
            .email(user.getEmail())
            .phone(user.getPhone())
            .ordersCount(ordersCount)
            .totalSpent(totalSpent)
            .joinedAt(user.getCreatedAt())
            .enabled(Boolean.TRUE.equals(user.getEnabled()))
            .build();
    }

    public static List<AdminUserResponse> fromList(List<User> users, java.util.Map<Long, Object[]> aggregates) {
        return users.stream()
            .map(u -> from(u, aggregates.get(u.getId())))
            .collect(Collectors.toList());
    }
}