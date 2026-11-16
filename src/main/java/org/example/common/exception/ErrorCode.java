package org.example.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    SUCCESS(200, "Thao tác thành công", HttpStatus.OK),
    BAD_REQUEST(400, "Yêu cầu không hợp lệ", HttpStatus.BAD_REQUEST),
    VALIDATION_ERROR(422, "Dữ liệu không thỏa mãn điều kiện kiểm tra", HttpStatus.UNPROCESSABLE_ENTITY),
    UNAUTHORIZED(401, "Chưa xác thực danh tính", HttpStatus.UNAUTHORIZED),
    FORBIDDEN(403, "Bạn không có quyền truy cập tài nguyên này", HttpStatus.FORBIDDEN),
    NOT_FOUND(404, "Không tìm thấy tài nguyên yêu cầu", HttpStatus.NOT_FOUND),
    METHOD_NOT_ALLOWED(405, "Phương thức HTTP không được hỗ trợ", HttpStatus.METHOD_NOT_ALLOWED),
    CONFLICT(409, "Dữ liệu đã tồn tại hoặc xảy ra xung đột", HttpStatus.CONFLICT),
    INTERNAL_SERVER_ERROR(500, "Lỗi hệ thống nội bộ, vui lòng thử lại sau", HttpStatus.INTERNAL_SERVER_ERROR),

    // Business domain errors
    RESOURCE_NOT_FOUND(1001, "Tài nguyên không tồn tại trong hệ thống", HttpStatus.NOT_FOUND),
    RESOURCE_ALREADY_EXISTS(1002, "Tài nguyên đã tồn tại", HttpStatus.CONFLICT),
    USER_NOT_FOUND(2001, "Không tìm thấy thông tin người dùng", HttpStatus.NOT_FOUND),
    INVALID_CREDENTIALS(2002, "Email/Số điện thoại hoặc mật khẩu không chính xác", HttpStatus.UNAUTHORIZED),
    EMAIL_ALREADY_EXISTS(2003, "Email đã được đăng ký trên hệ thống", HttpStatus.CONFLICT),
    PHONE_ALREADY_EXISTS(2004, "Số điện thoại đã được đăng ký trên hệ thống", HttpStatus.CONFLICT),
    USER_INACTIVE(2005, "Tài khoản của bạn hiện đang bị tạm khóa", HttpStatus.FORBIDDEN),
    INVALID_TOKEN(2006, "Token không hợp lệ hoặc phiên đăng nhập đã hết hạn", HttpStatus.UNAUTHORIZED),
    ROLE_NOT_FOUND(2007, "Vai trò người dùng không tồn tại trong hệ thống", HttpStatus.NOT_FOUND),
    OLD_PASSWORD_INCORRECT(2008, "Mật khẩu hiện tại không chính xác", HttpStatus.BAD_REQUEST),
    PASSWORD_CONFIRMATION_MISMATCH(2009, "Xác nhận mật khẩu mới không trùng khớp", HttpStatus.BAD_REQUEST),
    CANNOT_DELETE_USER_WITH_DEPENDENCIES(2010, "Không thể xóa người dùng do đã có dữ liệu giao dịch hoặc hồ sơ liên quan", HttpStatus.CONFLICT),

    // Specialty domain errors
    SPECIALTY_NOT_FOUND(3001, "Không tìm thấy thông tin chuyên khoa yêu cầu", HttpStatus.NOT_FOUND),
    SPECIALTY_NAME_EXISTS(3002, "Tên chuyên khoa đã tồn tại trên hệ thống", HttpStatus.CONFLICT),
    SPECIALTY_SLUG_EXISTS(3003, "Đường dẫn slug chuyên khoa đã được sử dụng", HttpStatus.CONFLICT),
    CANNOT_DELETE_SPECIALTY_HAS_DOCTORS(3004, "Không thể xóa chuyên khoa do đang có bác sĩ trực thuộc. Vui lòng tắt hoạt động thay vì xóa.", HttpStatus.CONFLICT);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
