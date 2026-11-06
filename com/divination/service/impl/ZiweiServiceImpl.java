package com.divination.service.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.divination.common.exception.BusinessException;
import com.divination.common.result.ResultCode;
import com.divination.common.util.AiRequestUtil;
import com.divination.config.AiConfig;
import com.divination.entity.User;
import com.divination.entity.ZiweiRecord;
import com.divination.mapper.UserMapper;
import com.divination.mapper.ZiweiRecordMapper;
import com.divination.service.UserService;
import com.divination.service.ZiweiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

/**
 * 紫微斗数测算服务实现
 * 流程：调用排盘引擎（Node 微服务）获取十二宫命盘 → 命盘数据注入 Prompt → AI 生成专业解读
 */
@Slf4j
@Service
public class ZiweiServiceImpl extends ServiceImpl<ZiweiRecordMapper, ZiweiRecord> implements ZiweiService {

    private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
    private static final String[] VALID_ZHI = {"子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥"};

    private final RestTemplate restTemplate;
    private final AiRequestUtil aiRequestUtil;
    private final AiConfig aiConfig;
    private final UserService userService;
    private final UserMapper userMapper;

    private String engineUrl;
    private int onceCostIntegral;
    private int dailyLimitCount;
    private int vipExtraDailyLimit;

    public ZiweiServiceImpl(RestTemplate restTemplate, AiRequestUtil aiRequestUtil,
                            AiConfig aiConfig, UserService userService, UserMapper userMapper) {
        this.restTemplate = restTemplate;
        this.aiRequestUtil = aiRequestUtil;
        this.aiConfig = aiConfig;
        this.userService = userService;
        this.userMapper = userMapper;
    }

    @Value("${ziwei.engine-url:http://localhost:8090}")
    public void setEngineUrl(String engineUrl) {
        this.engineUrl = engineUrl;
    }

    @Value("${divination.once-cost-integral:10}")
    public void setOnceCostIntegral(int onceCostIntegral) {
        this.onceCostIntegral = onceCostIntegral;
    }

    @Value("${divination.daily-limit-count:30}")
    public void setDailyLimitCount(int dailyLimitCount) {
        this.dailyLimitCount = dailyLimitCount;
    }

    @Value("${divination.vip-extra-daily-limit:20}")
    public void setVipExtraDailyLimit(int vipExtraDailyLimit) {
        this.vipExtraDailyLimit = vipExtraDailyLimit;
    }

    @Override
    @Transactional
    public ZiweiRecord createZiweiDivination(Long userId, String birthDate, String birthTimeZhi,
                                             String gender, String name) {
        // ── 1. 参数校验 ──
        validateInput(birthDate, birthTimeZhi, gender);
        LocalDate date = LocalDate.parse(birthDate, DateTimeFormatter.ISO_LOCAL_DATE);

        // ── 2. 每日限额检查（VIP 享有额外额度）──
        int todayCount = getTodayZiweiCount(userId);
        User user = userService.getUserById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        int effectiveLimit = user.isVipActive() ? dailyLimitCount + vipExtraDailyLimit : dailyLimitCount;
        if (todayCount >= effectiveLimit) {
            throw new BusinessException(ResultCode.DAILY_LIMIT_EXCEEDED);
        }

        // ── 3. 调用排盘引擎获取命盘 ──
        JSONObject engineData = callChartEngine(date, birthTimeZhi, gender);

        // ── 4. 组装 Prompt，调用 AI 生成解读 ──
        String chartJson = engineData.toJSONString();
        String result = generateAiResult(chartJson, name, gender, birthDate, birthTimeZhi);

        // ── 5. 原子扣积分 ──
        int affected = userMapper.deductIntegralAtomically(userId, onceCostIntegral);
        if (affected == 0) {
            throw new BusinessException(ResultCode.INTEGRAL_INSUFFICIENT);
        }

        // ── 6. 保存记录 ──
        ZiweiRecord record = new ZiweiRecord();
        record.setUserId(userId);
        record.setBirthDate(birthDate);
        record.setBirthTimeZhi(birthTimeZhi);
        record.setGender(gender);
        record.setPalaceData(chartJson);
        record.setResult(result);
        record.setCostIntegral(onceCostIntegral);
        record.setCreateTime(LocalDateTime.now());
        this.save(record);

        // 更新用户等级
        userService.updateLevelAfterDivination(userId, onceCostIntegral);

        return record;
    }

    @Override
    public IPage<ZiweiRecord> listRecords(Long userId, int page, int size) {
        Page<ZiweiRecord> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<ZiweiRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ZiweiRecord::getUserId, userId)
               .orderByDesc(ZiweiRecord::getCreateTime);
        return this.page(pageParam, wrapper);
    }

    @Override
    public ZiweiRecord getRecord(Long userId, Long id) {
        ZiweiRecord record = this.getById(id);
        if (record == null) {
            throw new BusinessException(ResultCode.RECORD_NOT_FOUND);
        }
        if (!record.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        return record;
    }

    // ===================== 内部方法 =====================

    private void validateInput(String birthDate, String birthTimeZhi, String gender) {
        if (birthDate == null || !DATE_PATTERN.matcher(birthDate).matches()) {
            throw new BusinessException("出生日期格式必须为 yyyy-MM-dd");
        }
        if (birthTimeZhi == null || !isValidZhi(birthTimeZhi)) {
            throw new BusinessException("出生时辰必须为十二地支之一（子丑寅卯辰巳午未申酉戌亥）");
        }
        if (!"男".equals(gender) && !"女".equals(gender)) {
            throw new BusinessException("性别必须为 男 或 女");
        }
    }

    private boolean isValidZhi(String zhi) {
        for (String valid : VALID_ZHI) {
            if (valid.equals(zhi)) return true;
        }
        return false;
    }

    private int getTodayZiweiCount(Long userId) {
        LambdaQueryWrapper<ZiweiRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ZiweiRecord::getUserId, userId)
               .ge(ZiweiRecord::getCreateTime, LocalDate.now().atStartOfDay());
        return (int) this.count(wrapper);
    }

    /**
     * 调用排盘引擎微服务 POST /chart
     */
    private JSONObject callChartEngine(LocalDate date, String birthTimeZhi, String gender) {
        // 直接传 Map，让 Jackson 转换器序列化一次（避免 String body 被二次 JSON 编码）
        java.util.Map<String, Object> requestBody = new java.util.HashMap<>();
        requestBody.put("year", date.getYear());
        requestBody.put("month", date.getMonthValue());
        requestBody.put("day", date.getDayOfMonth());
        requestBody.put("hourZhi", birthTimeZhi);
        requestBody.put("gender", gender);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<java.util.Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    engineUrl + "/chart", HttpMethod.POST, entity, String.class);
            JSONObject body = JSONObject.parseObject(response.getBody());
            if (body != null && body.getIntValue("code") == 200 && body.getJSONObject("data") != null) {
                return body.getJSONObject("data");
            }
            String msg = body != null ? body.getString("msg") : "空响应";
            throw new BusinessException("排盘失败：" + msg);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("紫微排盘引擎调用失败: {}", e.getMessage());
            throw new BusinessException(500, "排盘引擎服务不可用，请稍后再试");
        }
    }

    /**
     * 将命盘 JSON 转换为结构化文本注入 Prompt，调用 AI 生成解读
     */
    private String generateAiResult(String chartJson, String name, String gender,
                                    String birthDate, String birthTimeZhi) {
        JSONObject data = JSONObject.parseObject(chartJson);
        String chartText = buildChartPrompt(data);

        String systemPrompt = aiConfig.getAiPrompt() +
                "\n你现在是资深紫微斗数命理师，精通《紫微斗数全书》与十二宫星曜论命体系。" +
                "用户会提供一份由专业排盘引擎生成的完整命盘数据（含十二宫、星曜、亮度、四化、大限、格局）。" +
                "请严格依据命盘数据进行分析，不要凭空编造星曜或宫位信息。" +
                "解读结构：【命盘总览】【性格与天赋（命宫）】【事业与财运】【感情与婚姻】【健康提示】【大限运势走向】【综合建议】。" +
                "文风古风典雅，内容详实，条理清晰。";

        String displayName = (name != null && !name.isBlank()) ? name : "缘主";
        String userPrompt = String.format(
                "请为以下命盘进行专业解读。\n\n【缘主信息】\n称呼：%s\n性别：%s\n公历生辰：%s %s时\n\n%s",
                displayName, gender, birthDate, birthTimeZhi, chartText);

        try {
            return aiRequestUtil.callAi(systemPrompt, userPrompt);
        } catch (Exception e) {
            log.warn("紫微AI解读生成失败，使用降级结果: {}", e.getMessage());
            return "【紫微斗数测算结果】\n\n缘主 " + displayName + "（" + birthDate + " " + birthTimeZhi + "时）\n\n"
                    + chartText
                    + "\n\n由于AI解读服务暂时不可用，以上为引擎排盘结果。完整解读请稍后重新测算或通过AI对话追问。";
        }
    }

    /**
     * 命盘 JSON → 人类可读结构化文本（供 AI prompt 使用）
     */
    private String buildChartPrompt(JSONObject data) {
        StringBuilder sb = new StringBuilder();
        JSONObject chart = data.getJSONObject("chart");
        if (chart == null) {
            return "（命盘数据缺失）";
        }

        // 基本信息
        sb.append("【命盘基本信息】\n");
        JSONObject lunar = chart.getJSONObject("lunarInfo");
        if (lunar != null) {
            sb.append("农历：").append(lunar.getIntValue("lunarYear")).append("年")
              .append(lunar.getIntValue("lunarMonth")).append("月")
              .append(lunar.getIntValue("lunarDay")).append("日\n");
            JSONArray pillars = lunar.getJSONArray("fourPillars");
            if (pillars != null) {
                sb.append("四柱：");
                for (int i = 0; i < pillars.size(); i++) {
                    sb.append(pillars.getString(i)).append(" ");
                }
                sb.append("\n");
            }
        }
        sb.append("五行局：").append(chart.getString("wuxingJuName")).append("\n");
        sb.append("命主：").append(chart.getString("mingZhu"))
          .append("，身主：").append(chart.getString("shenZhu"))
          .append("，阴阳性别：").append(chart.getString("yinYangGender")).append("\n");
        sb.append("当前虚岁：").append(chart.getIntValue("currentAge")).append("\n\n");

        // 十二宫
        sb.append("【十二宫排盘】\n");
        JSONArray palaces = chart.getJSONArray("palaces");
        if (palaces != null) {
            for (int i = 0; i < palaces.size(); i++) {
                JSONObject palace = palaces.getJSONObject(i);
                String palaceName = palace.getString("name");
                StringBuilder starLine = new StringBuilder();
                JSONArray stars = palace.getJSONArray("stars");
                if (stars != null) {
                    for (int j = 0; j < stars.size(); j++) {
                        JSONObject star = stars.getJSONObject(j);
                        String starName = star.getString("name");
                        String brightness = star.getString("brightnessRaw");
                        String siHua = star.getString("siHua");
                        starLine.append(starName);
                        if (brightness != null && !brightness.isEmpty()) {
                            starLine.append("(").append(brightness).append(")");
                        }
                        if (siHua != null && !siHua.isEmpty()) {
                            starLine.append("【化").append(siHua).append("】");
                        }
                        starLine.append(" ");
                    }
                }
                String daxian = "";
                JSONArray daXianAge = palace.getJSONArray("daXianAge");
                if (daXianAge != null && daXianAge.size() == 2) {
                    daxian = "（大限 " + daXianAge.getIntValue(0) + "-" + daXianAge.getIntValue(1) + "岁）";
                }
                sb.append(palaceName).append(daxian).append("：")
                  .append(starLine.length() > 0 ? starLine.toString().trim() : "空宫（借对宫）")
                  .append("\n");
            }
        }
        sb.append("\n");

        // 格局
        JSONArray patterns = data.getJSONArray("patterns");
        if (patterns != null && !patterns.isEmpty()) {
            sb.append("【命中格局】\n");
            for (int i = 0; i < patterns.size(); i++) {
                JSONObject pattern = patterns.getJSONObject(i);
                sb.append("- ").append(pattern.getString("name"))
                  .append("：").append(pattern.getString("description")).append("\n");
            }
            sb.append("\n");
        }

        return sb.toString();
    }
}
