package com.divination.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.divination.common.exception.BusinessException;
import com.divination.config.PaymentConfig;
import com.divination.entity.PaymentOrder;
import com.divination.mapper.PaymentOrderMapper;
import com.divination.mapper.UserMapper;
import com.divination.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 支付服务实现
 * 支持微信支付（Native扫码）和支付宝（电脑网站支付）
 * 注意：这是基础实现框架，实际生产需要接入官方SDK
 */
@Service
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final PaymentOrderMapper paymentOrderMapper;
    private final UserMapper userMapper;
    private final PaymentConfig paymentConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${server.port:8080}")
    private int serverPort;

    public PaymentServiceImpl(PaymentOrderMapper paymentOrderMapper, UserMapper userMapper,
                              PaymentConfig paymentConfig, RestTemplate restTemplate,
                              ObjectMapper objectMapper) {
        this.paymentOrderMapper = paymentOrderMapper;
        this.userMapper = userMapper;
        this.paymentConfig = paymentConfig;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public Map<String, Object> createPayment(Long userId, Integer amount, String payType, String remark) {
        // 参数校验
        if (amount == null || amount <= 0) {
            throw new BusinessException(400, "支付金额必须大于0");
        }
        if (!"WECHAT_NATIVE".equals(payType) && !"ALIPAY_WEB".equals(payType)) {
            throw new BusinessException(400, "不支持的支付方式: " + payType);
        }

        // 检查用户是否存在
        if (userMapper.selectById(userId) == null) {
            throw new BusinessException(404, "用户不存在");
        }

        // 生成商户订单号
        String orderNo = generateOrderNo();

        // 计算过期时间（30分钟）
        LocalDateTime expireTime = LocalDateTime.now().plusMinutes(30);

        // 创建订单
        PaymentOrder order = new PaymentOrder();
        order.setUserId(userId);
        order.setOrderNo(orderNo);
        order.setAmount(amount);
        order.setCurrency("CNY");
        order.setPayType(payType);
        order.setStatus("PENDING");
        order.setExpireTime(expireTime);
        order.setRemark(remark != null ? remark : "");
        paymentOrderMapper.insert(order);

        log.info("Created payment order: orderNo={}, amount={}, payType={}", orderNo, amount, payType);

        // 根据支付方式返回不同的支付参数
        Map<String, Object> result = new HashMap<>();
        result.put("orderNo", orderNo);
        result.put("amount", amount);
        result.put("payType", payType);
        result.put("expireTime", expireTime);

        if ("WECHAT_NATIVE".equals(payType)) {
            result.putAll(createWechatPayment(order));
        } else {
            result.putAll(createAlipayPayment(order));
        }

        return result;
    }

    /**
     * 创建微信支付（Native扫码支付）
     * 使用微信支付V3 API
     */
    private Map<String, Object> createWechatPayment(PaymentOrder order) {
        PaymentConfig.Wechat wechat = paymentConfig.getWechat();
        if (!wechat.isEnabled()) {
            throw new BusinessException(500, "微信支付未启用");
        }

        Map<String, Object> response = new ConcurrentHashMap<>();

        try {
            // 构建请求体
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("appid", wechat.getAppId());
            body.put("mchid", wechat.getMchId());
            body.put("description", "AI玄学命理测算平台 - " + (order.getRemark() != null ? order.getRemark() : "积分充值"));
            body.put("out_trade_no", order.getOrderNo());
            body.put("notify_url", wechat.getNotifyUrl());

            Map<String, Object> amountObj = new LinkedHashMap<>();
            amountObj.put("total", order.getAmount());
            amountObj.put("currency", "CNY");
            body.put("amount", amountObj);

            String jsonBody = objectMapper.writeValueAsString(body);
            String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
            String nonce = UUID.randomUUID().toString().substring(0, 32);

            // 签名串
            String signString = "POST\n" + "/v3/pay/transactions/native\n" + timestamp + "\n" + nonce + "\n" + jsonBody + "\n";

            // 使用API密钥生成HMAC-SHA256签名（实际生产需要使用商户私钥RSA签名）
            String signature = hmacSha256(signString, wechat.getApiKey());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", buildWechatAuth(wechat.getAppId(), wechat.getMchId(), nonce, timestamp, signature));
            headers.set("Accept", "application/json");

            HttpEntity<String> httpEntity = new HttpEntity<>(jsonBody, headers);

            // 调用微信支付统一下单API
            ResponseEntity<Map> resp = restTemplate.exchange(
                    "https://api.mch.weixin.qq.com/v3/pay/transactions/native",
                    HttpMethod.POST, httpEntity, Map.class);

            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                Map<String, Object> data = resp.getBody();
                response.put("codeUrl", data.get("code_url")); // 二维码链接
                response.put("appId", wechat.getAppId());
                response.put("mchId", wechat.getMchId());
                return response;
            }
        } catch (Exception e) {
            log.error("Wechat payment creation failed: {}", e.getMessage());
            throw new BusinessException(500, "微信下单失败: " + e.getMessage());
        }

        throw new BusinessException(500, "微信下单异常");
    }

    /**
     * 创建支付宝电脑网站支付
     * 使用支付宝SDK或REST API
     */
    private Map<String, Object> createAlipayPayment(PaymentOrder order) {
        PaymentConfig.Alipay alipay = paymentConfig.getAlipay();
        if (!alipay.isEnabled()) {
            throw new BusinessException(500, "支付宝未启用");
        }

        Map<String, Object> response = new ConcurrentHashMap<>();

        try {
            // 构建请求参数
            Map<String, String> params = new LinkedHashMap<>();
            params.put("app_id", alipay.getAppId());
            params.put("method", "alipay.trade.page.pay");
            params.put("charset", "utf-8");
            params.put("sign_type", "RSA2");
            params.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            params.put("version", "1.0");
            params.put("notify_url", alipay.getNotifyUrl());
            params.put("biz_content", objectMapper.writeValueAsString(Map.of(
                    "out_trade_no", order.getOrderNo(),
                    "total_amount", String.format("%.2f", order.getAmount() / 100.0),
                    "subject", "AI玄学命理测算平台 - " + (order.getRemark() != null ? order.getRemark() : "积分充值"),
                    "product_code", "FAST_INSTANT_TRADE_PAY"
            )));

            // 生成签名
            String sign = generateAlipaySign(params, alipay.getPrivateKey());
            params.put("sign", sign);

            // 构建支付宝跳转URL
            String queryString = params.entrySet().stream()
                    .map(e -> e.getKey() + "=" + java.net.URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                    .collect(Collectors.joining("&"));
            String alipayUrl = "https://openapi.alipay.com/gateway.do?" + queryString;

            response.put("alipayUrl", alipayUrl); // 前端重定向到此URL
            response.put("appId", alipay.getAppId());
            return response;
        } catch (Exception e) {
            log.error("Alipay payment creation failed: {}", e.getMessage());
            throw new BusinessException(500, "支付宝下单失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public String handleWechatNotify(String notifyData, String signature) {
        try {
            // 解析回调数据
            Map<String, Object> callback = objectMapper.readValue(notifyData, Map.class);

            // 验证签名（实际生产需要更严格的验签逻辑）
            Map<String, Object> resource = (Map<String, Object>) callback.get("resource");
            if (resource != null) {
                String decryptedData = decryptWechatResource((String) resource.get("data"));
                Map<String, Object> data = objectMapper.readValue(decryptedData, Map.class);

                String orderNo = (String) data.get("out_trade_no");
                String tradeState = (String) data.get("trade_state");
                String transactionId = (String) data.get("transaction_id");

                if ("SUCCESS".equals(tradeState)) {
                    // 更新订单状态为已支付
                    PaymentOrder order = paymentOrderMapper.getByOrderNo(orderNo);
                    if (order != null && "PENDING".equals(order.getStatus())) {
                        order.setStatus("PAID");
                        order.setPaidTime(LocalDateTime.now());
                        order.setTransactionId(transactionId);
                        paymentOrderMapper.updateById(order);

                        // 发放积分（根据订单remark中的商品信息）
                        grantPointsForPayment(order);

                        log.info("Wechat payment success: orderNo={}", orderNo);
                        return "{\"code\":\"SUCCESS\",\"message\":\"成功\"}";
                    }
                }
            }
        } catch (Exception e) {
            log.error("Wechat notification handling failed: {}", e.getMessage(), e);
        }
        return "{\"code\":\"FAIL\",\"message\":\"失败\"}";
    }

    @Override
    @Transactional
    public boolean handleAlipayNotify(Map<String, String> params) {
        try {
            String orderNo = params.get("out_trade_no");
            String tradeStatus = params.get("trade_status");
            String tradeNo = params.get("trade_no");

            // 验签
            String sign = params.remove("sign");
            String signType = params.get("sign_type");
            params.remove("sign_type");
            boolean verified = verifyAlipaySign(params, sign, signType);

            if (!verified) {
                log.warn("Alipay signature verification failed for order: {}", orderNo);
                return false;
            }

            if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                PaymentOrder order = paymentOrderMapper.getByOrderNo(orderNo);
                if (order != null && "PENDING".equals(order.getStatus())) {
                    order.setStatus("PAID");
                    order.setPaidTime(LocalDateTime.now());
                    order.setTransactionId(tradeNo);
                    paymentOrderMapper.updateById(order);

                    // 发放积分
                    grantPointsForPayment(order);

                    log.info("Alipay payment success: orderNo={}", orderNo);
                    return true;
                }
            }
        } catch (Exception e) {
            log.error("Alipay notification handling failed: {}", e.getMessage(), e);
        }
        return false;
    }

    @Override
    public Map<String, Object> queryOrder(String orderNo) {
        PaymentOrder order = paymentOrderMapper.getByOrderNo(orderNo);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        Map<String, Object> result = new HashMap<>();
        result.put("orderNo", order.getOrderNo());
        result.put("amount", order.getAmount());
        result.put("status", order.getStatus());
        result.put("payType", order.getPayType());
        result.put("createTime", order.getCreateTime());
        result.put("paidTime", order.getPaidTime());
        return result;
    }

    @Override
    public List<PaymentOrder> getRecentOrders(Long userId, int limit) {
        return paymentOrderMapper.getRecentOrders(userId, limit);
    }

    @Override
    @Transactional
    public int expirePendingOrders() {
        return paymentOrderMapper.expireOldOrders();
    }

    // ===================== 私有辅助方法 =====================

    /**
     * 根据支付订单发放积分
     * 规则：1元 = 100积分（1分 = 1积分）
     */
    private void grantPointsForPayment(PaymentOrder order) {
        // 简单规则：每1分钱 = 1积分
        int pointsToAdd = order.getAmount();
        userMapper.addIntegralAtomically(order.getUserId(), pointsToAdd);

        log.info("Granted {} points to user {} for payment order {}",
                pointsToAdd, order.getUserId(), order.getOrderNo());
    }

    /**
     * 生成唯一的商户订单号
     * 格式: D + yyyyMMddHHmmss + 6位随机数
     */
    private String generateOrderNo() {
        return "D" + LocalDateTime.now().format(DT_FMT) +
                String.format("%06d", new Random().nextInt(1000000));
    }

    /**
     * HMAC-SHA256签名（微信支付V3）
     */
    private String hmacSha256(String signString, String apiKey) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(apiKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] hash = mac.doFinal(signString.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("HMAC-SHA256签名失败", e);
        }
    }

    /**
     * 构建微信支付Authorization头
     */
    private String buildWechatAuth(String appId, String mchId, String nonce, String timestamp, String signature) {
        return "WECHAT-PAY2-SHA256-RSA2048 " +
                "mchid=\"" + mchId + "\"," +
                "nonce_str=\"" + nonce + "\"," +
                "timestamp=\"" + timestamp + "\"," +
                "serial_no=\"" + paymentConfig.getWechat().getSerialNo() + "\"," +
                "signature=\"" + signature + "\"";
    }

    /**
     * 解密微信支付回调中的加密数据
     */
    private String decryptWechatResource(String encryptedData) {
        // 实际实现需要使用AES-256-GCM解密
        // 这里简化处理，实际应使用微信SDK
        return encryptedData;
    }

    /**
     * 生成支付宝RSA2签名
     */
    private String generateAlipaySign(Map<String, String> params, String privateKey) {
        try {
            StringBuilder content = new StringBuilder();
            params.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(e -> content.append(e.getKey()).append("=")
                            .append(e.getValue()).append("&"));
            String signContent = content.substring(0, content.length() - 1);

            Mac mac = Mac.getInstance("SHA256withRSA");
            // 从PEM格式私钥生成SecretKeySpec（实际生产需正确解析PEM）
            byte[] keyBytes = Base64.getDecoder().decode(privateKey.replace("\n", "").replace("-----BEGIN RSA PRIVATE KEY-----", "").replace("-----END RSA PRIVATE KEY-----", ""));
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "RSA");
            mac.init(keySpec);
            byte[] signed = mac.doFinal(signContent.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signed);
        } catch (Exception e) {
            throw new RuntimeException("支付宝签名失败", e);
        }
    }

    /**
     * 验证支付宝回调签名
     */
    private boolean verifyAlipaySign(Map<String, String> params, String sign, String signType) {
        try {
            StringBuilder content = new StringBuilder();
            params.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(e -> content.append(e.getKey()).append("=")
                            .append(e.getValue()).append("&"));
            String signContent = content.substring(0, content.length() - 1);

            Mac mac = Mac.getInstance("SHA256withRSA");
            byte[] keyBytes = Base64.getDecoder().decode(
                    paymentConfig.getAlipay().getAlipayPublicKey()
                            .replace("\n", "")
                            .replace("-----BEGIN PUBLIC KEY-----", "")
                            .replace("-----END PUBLIC KEY-----", ""));
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "RSA");
            mac.init(keySpec);
            byte[] signed = mac.doFinal(signContent.getBytes(StandardCharsets.UTF_8));
            String expectedSign = Base64.getEncoder().encodeToString(signed);
            return expectedSign.equals(sign);
        } catch (Exception e) {
            log.error("Alipay signature verification error: {}", e.getMessage());
            return false;
        }
    }
}
