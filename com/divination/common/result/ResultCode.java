package com.divination.common.result;

public enum ResultCode {
    SUCCESS(200, "操作成功"),
    ERROR(500, "服务器异常"),
    BAD_REQUEST(400, "参数错误"),
    UNAUTHORIZED(401, "未登录或Token过期"),
    FORBIDDEN(403, "权限不足"),
    USER_DISABLED(400, "账号已被禁用"),
    INTEGRAL_INSUFFICIENT(400, "积分不足"),
    DAILY_LIMIT_EXCEEDED(400, "今日测算次数已达上限"),
    CARD_KEY_NOT_FOUND(400, "卡密不存在"),
    CARD_KEY_USED(400, "卡密已被兑换"),
    CARD_KEY_INVALID(400, "卡密已作废"),
    CARD_KEY_EXCHANGE_SUCCESS(200, "兑换成功"),
    AI_CALL_FAILED(500, "AI接口调用失败"),
    AI_TIMEOUT(500, "AI接口调用超时"),
    USERNAME_EXISTS(400, "账号已存在"),
    PASSWORD_MISMATCH(400, "两次密码输入不一致"),
    INVALID_CREDENTIALS(401, "账号或密码错误"),
    TOO_MANY_ATTEMPTS(429, "请求过于频繁，请稍后再试"),
    ACCOUNT_LOCKED(429, "账号已锁定"),
    CAN_NOT_DELETE_SELF(400, "不能操作自己"),
    RECORD_NOT_FOUND(400, "记录不存在"),
    USER_NOT_FOUND(400, "用户不存在"),
    PARAM_ERROR(400, "参数错误");

    private final int code;
    private final String msg;

    ResultCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public int getCode() { return code; }
    public String getMsg() { return msg; }
}
