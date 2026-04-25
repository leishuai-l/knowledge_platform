package com.zhixiang.knowledge_platform.exception;

/**
 * 错误代码常量类
 * 定义系统中所有的错误代码
 *
 * @author ZhiXiang Team
 * @version 0.0.1-SNAPSHOT
 */
public final class ErrorCode {

    // 通用错误 (1000-1999)
    public static final int SUCCESS = 0;
    public static final int INTERNAL_SERVER_ERROR = 1000;
    public static final int BUSINESS_ERROR = 1001;
    public static final int UNKNOWN_ERROR = 1999;

    // 请求错误 (2000-2999)
    public static final int VALIDATION_FAILED = 2000;
    public static final int MISSING_PARAMETER = 2001;
    public static final int INVALID_PARAMETER = 2002;
    public static final int INVALID_REQUEST_BODY = 2003;
    public static final int METHOD_NOT_ALLOWED = 2004;
    public static final int ENDPOINT_NOT_FOUND = 2005;
    public static final int ILLEGAL_STATE = 2006;

    // 认证授权错误 (3000-3999)
    public static final int AUTHENTICATION_FAILED = 3000;
    public static final int INVALID_CREDENTIALS = 3001;
    public static final int ACCESS_DENIED = 3002;
    public static final int TOKEN_INVALID = 3003;
    public static final int TOKEN_EXPIRED = 3004;
    public static final int ACCOUNT_LOCKED = 3005;
    public static final int ACCOUNT_DISABLED = 3006;

    // 资源错误 (4000-4999)
    public static final int RESOURCE_NOT_FOUND = 4000;
    public static final int RESOURCE_ALREADY_EXISTS = 4001;
    public static final int RESOURCE_CONFLICT = 4002;
    public static final int RESOURCE_LOCKED = 4003;

    // 用户相关错误 (5000-5999)
    public static final int USER_NOT_FOUND = 5000;
    public static final int USER_ALREADY_EXISTS = 5001;
    public static final int INSUFFICIENT_PERMISSIONS = 5002;
    public static final int INSUFFICIENT_POINTS = 5003;
    public static final int USER_BANNED = 5004;

    // 文档相关错误 (6000-6999)
    public static final int DOCUMENT_NOT_FOUND = 6000;
    public static final int DOCUMENT_UPLOAD_FAILED = 6001;
    public static final int DOCUMENT_PROCESSING_FAILED = 6002;
    public static final int DOCUMENT_ACCESS_DENIED = 6003;
    public static final int DOCUMENT_ALREADY_RATED = 6004;
    public static final int DOCUMENT_NOT_APPROVED = 6005;

    // 文件相关错误 (7000-7999)
    public static final int FILE_TOO_LARGE = 7000;
    public static final int FILE_TYPE_NOT_SUPPORTED = 7001;
    public static final int FILE_UPLOAD_FAILED = 7002;
    public static final int FILE_NOT_FOUND = 7003;
    public static final int FILE_CORRUPTED = 7004;
    public static final int FILE_VIRUS_DETECTED = 7005;

    // 数据库相关错误 (8000-8999)
    public static final int DATA_INTEGRITY_VIOLATION = 8000;
    public static final int DATABASE_CONNECTION_FAILED = 8001;
    public static final int TRANSACTION_FAILED = 8002;

    // 外部服务错误 (9000-9999)
    public static final int EMAIL_SEND_FAILED = 9000;
    public static final int SMS_SEND_FAILED = 9001;
    public static final int THIRD_PARTY_SERVICE_ERROR = 9002;

    private ErrorCode() {
        // 防止实例化
    }
}