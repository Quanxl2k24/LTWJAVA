package org.example.common.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Tiêm thông tin người dùng hiện tại (UserPrincipal) vào tham số của Controller method.
 *
 * <p>Ví dụ:
 * <pre>
 * &#64;GetMapping("/me")
 * &#64;RequireRole
 * public ApiResponse&lt;UserPrincipal&gt; getCurrentUser(&#64;CurrentUser UserPrincipal currentUser) {
 *     return ApiResponse.success("Lấy thông tin thành công", currentUser);
 * }
 * </pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CurrentUser {
}

