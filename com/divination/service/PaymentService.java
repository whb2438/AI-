package com.divination.service;

import com.divination.entity.PaymentOrder;

import java.util.List;
import java.util.Map;

/**
 * 支付服务接口 — 管理订单创建、支付、回调、退款等全生命周期
 */
public interface PaymentService {

    /**
     * 创建支付订单
     * @param userId 用户ID
     * @param amount 金额（分）
     * @param payType 支付方式: WECHAT_NATIVE / ALIPAY_WEB
     * @param remark 备注
     * @return 包含支付参数（二维码URL/跳转链接等）的Map
     */
    Map<String, Object> createPayment(Long userId, Integer amount, String payType, String remark);

    /**
     * 处理微信支付回调通知
     * @param notifyData 微信回调原始数据
     * @param signature 签名
     * @return 给微信的响应（success/fail）
     */
    String handleWechatNotify(String notifyData, String signature);

    /**
     * 处理支付宝回调通知
     * @param params 支付宝回调参数Map
     * @return true if validated and processed
     */
    boolean handleAlipayNotify(Map<String, String> params);

    /**
     * 查询订单状态
     */
    Map<String, Object> queryOrder(String orderNo);

    /**
     * 获取用户最近支付记录
     */
    List<PaymentOrder> getRecentOrders(Long userId, int limit);

    /**
     * 关闭过期订单
     */
    int expirePendingOrders();
}
