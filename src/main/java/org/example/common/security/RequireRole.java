package org.example.common.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation kiểm tra quyền truy cập (Authorization) dựa trên vai trò (Role).
 * Có thể đặt ở cấp Controller (Class) hoặc từng Endpoint (Method).
 *
 * <p>Cách dùng:
 * <pre>
 * // Chỉ cho phép ADMIN hoặc DOCTOR truy cập
 * &#64;RequireRole({RoleConstant.ADMIN, RoleConstant.DOCTOR})
 * &#64;GetMapping("/patients")
 * public ApiResponse&lt;List&lt;PatientResponse&gt;&gt; getPatients() { ... }
 *
 * // Yêu cầu chỉ cần đăng nhập (bất kỳ role nào)
 * &#64;RequireRole
 * &#64;GetMapping("/profile")
 * public ApiResponse&lt;UserProfile&gt; getProfile() { ... }
 * </pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireRole {

    /**
     * Danh sách mã vai trò được phép truy cập (ví dụ: "ADMIN", "DOCTOR", "STAFF", "PATIENT").
     * Áp dụng điều kiện OR: Người dùng chỉ cần có 1 trong các vai trò được liệt kê.
     * Nếu mảng rỗng (mặc định), chỉ yêu cầu đã đăng nhập và có token hợp lệ.
     */
    String[] value() default {};
}

