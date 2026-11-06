package com.divination.controller;

import com.divination.common.result.Result;
import com.divination.entity.PaymentOrder;
import com.divination.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 支付控制器 — 创建订单、查询状态、处理回调
 */
@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * POST /api/payment/create — 创建支付订单
     */
    @PostMapping("/create")
    public Result<Map<String, Object>> createPayment(HttpServletRequest request,
            @Valid @RequestBody CreatePaymentRequest req) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.error(401, "未登录");
        Map<String, Object> data = paymentService.createPayment(userId, req.getAmount(), req.getPayType(), req.getRemark());
        return Result.success(data);
    }

    /**
     * GET /api/payment/order/{orderNo} — 查询订单状态
     */
    @GetMapping("/order/{orderNo}")
    public Result<Map<String, Object>> queryOrder(HttpServletRequest request, @PathVariable String orderNo) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.error(401, "未登录");
        return Result.success(paymentService.queryOrder(orderNo));
    }

    /**
     * GET /api/payment/history — 支付记录
     */
    @GetMapping("/history")
    public Result<List<PaymentOrder>> getHistory(HttpServletRequest request,
            @RequestParam(defaultValue = "20") int limit) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.error(401, "未登录");
        return Result.success(paymentService.getRecentOrders(userId, limit));
    }

    // ===================== 微信/支付宝回调（公开，无需认证）=====================

    /**
     * POST /api/payment/wechat/notify — 微信支付回调
     */
    @PostMapping("/wechat/notify")
    public String wechatNotify(@RequestBody String notifyData,
                               @RequestHeader(value = "Wechatpay-Signature", required = false) String signature) {
        return paymentService.handleWechatNotify(notifyData, signature);
    }

    /**
     * POST /api/payment/alipay/notify — 支付宝回调
     */
    @PostMapping("/alipay/notify")
    public String alipayNotify(@RequestParam Map<String, String> params) {
        boolean success = paymentService.handleAlipayNotify(params);
        return success ? "success" : "fail";
    }

    // ===================== DTO =====================

    public static class CreatePaymentRequest {
        @NotNull(message = "支付金额不能为空")
        @Min(value = 1, message = "支付金额必须大于0")
        private Integer amount;       // 单位：分
        @NotBlank(message = "支付方式不能为空")
        private String payType;       // WECHAT_NATIVE / ALIPAY_WEB
        private String remark;
        public Integer getAmount() { return amount; }
        public void setAmount(Integer amount) { this.amount = amount; }
        public String getPayType() { return payType; }
        public void setPayType(String payType) { this.payType = payType; }
        public String getRemark() { return remark; }
        public void setRemark(String remark) { this.remark = remark; }
    }
}
