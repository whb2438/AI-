package com.divination.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 支付配置 — 从 application.yml 读取微信/支付宝参数
 */
@Component
@ConfigurationProperties(prefix = "payment")
public class PaymentConfig {

    private Wechat wechat = new Wechat();
    private Alipay alipay = new Alipay();

    public Wechat getWechat() { return wechat; }
    public void setWechat(Wechat wechat) { this.wechat = wechat; }
    public Alipay getAlipay() { return alipay; }
    public void setAlipay(Alipay alipay) { this.alipay = alipay; }

    public static class Wechat {
        private String appId;
        private String mchId;       // 商户号
        private String apiKey;      // API v3 密钥
        private String serialNo;     // 证书序列号
        private String notifyUrl;    // 回调地址
        private boolean enabled = false;

        public String getAppId() { return appId; }
        public void setAppId(String appId) { this.appId = appId; }
        public String getMchId() { return mchId; }
        public void setMchId(String mchId) { this.mchId = mchId; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getSerialNo() { return serialNo; }
        public void setSerialNo(String serialNo) { this.serialNo = serialNo; }
        public String getNotifyUrl() { return notifyUrl; }
        public void setNotifyUrl(String notifyUrl) { this.notifyUrl = notifyUrl; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }

    public static class Alipay {
        private String appId;
        private String privateKey;
        private String alipayPublicKey;
        private String notifyUrl;
        private boolean enabled = false;

        public String getAppId() { return appId; }
        public void setAppId(String appId) { this.appId = appId; }
        public String getPrivateKey() { return privateKey; }
        public void setPrivateKey(String privateKey) { this.privateKey = privateKey; }
        public String getAlipayPublicKey() { return alipayPublicKey; }
        public void setAlipayPublicKey(String alipayPublicKey) { this.alipayPublicKey = alipayPublicKey; }
        public String getNotifyUrl() { return notifyUrl; }
        public void setNotifyUrl(String notifyUrl) { this.notifyUrl = notifyUrl; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
}
