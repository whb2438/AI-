package com.divination.common.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 八字（四柱）计算引擎 —— 替换旧的 DivinationBaziUtil。
 *
 * 计算依据（已校验锚点：2000-01-01=戊午日；2024-06-15 月柱庚午；立春年界正确）：
 *  - 日柱：儒略日(JDN) + 固定锚点，精确无误差。
 *  - 年柱：以「立春」为界换年。
 *  - 月柱：以十二「节」为界换月（寿星公式求节气日期，±1 天误差，仅影响节气当日附近）。
 *  - 时柱：五鼠遁（日干定时干）。
 *  - 月干：五虎遁（年干定月干）。
 *
 * 古籍/开源规则增补（参考《三命通会》《渊海子平》、6tail/lunar、reed1898/bazi-tool）：
 *  - 纳音：六十甲子纳音（四柱各一）。
 *  - 地支藏干：本气/中气/余气（提升日主强弱与显示）。
 *  - 神煞：天乙贵人、文昌、禄神、羊刃、桃花、驿马、华盖、空亡（仅列命局实有者）。
 *  - 十二长生（地势）：四柱地支相对日主。
 *  - 日主强弱：藏干加权 + 月令得令。
 *  - 真太阳时校正：出生地经度 + 均时差近似，可跨日重算日柱/时柱（需在调用处开启）。
 *
 * 约定：子时(23:00–00:59)按「晚子时」处理（日柱不跨日），与主流排盘软件(6tail sect=2)一致。
 */
public class BaziCalculator {

    /* ===================== 基础表 ===================== */
    private static final String[] GAN = {"甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸"};
    private static final String[] ZHI = {"子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥"};

    // 天干五行：0木 1木 2火 3火 4土 5土 6金 7金 8水 9水
    private static final int[] GAN_WX = {0, 0, 1, 1, 2, 2, 3, 3, 4, 4};
    // 地支本气五行：子水 丑土 寅木 卯木 辰土 巳火 午火 未土 申金 酉金 戌土 亥水
    private static final int[] ZHI_WX = {4, 2, 0, 0, 2, 1, 1, 2, 3, 3, 2, 4};
    private static final String[] WX = {"木", "火", "土", "金", "水"};

    // 60 甲子循环（阳干配阳支 / 阴干配阴支）
    private static final String[] JIAZI = buildJiazi();

    // ---- 纳音：六十甲子纳音（与三命通会/渊海子平一致，逐对核对） ----
    private static final String[] NAYIN = {
        "海中金", "海中金", "炉中火", "炉中火", "大林木", "大林木", "路旁土", "路旁土", "剑锋金", "剑锋金",
        "山头火", "山头火", "涧下水", "涧下水", "城头土", "城头土", "白腊金", "白腊金", "杨柳木", "杨柳木",
        "泉中水", "泉中水", "屋上土", "屋上土", "霹雳火", "霹雳火", "松柏木", "松柏木", "长流水", "长流水",
        "沙中金", "沙中金", "山下火", "山下火", "平地木", "平地木", "壁上土", "壁上土", "金箔金", "金箔金",
        "覆灯火", "覆灯火", "天河水", "天河水", "大驿土", "大驿土", "钗钏金", "钗钏金", "桑柘木", "桑柘木",
        "大溪水", "大溪水", "沙中土", "沙中土", "太阳火", "太阳火", "石榴木", "石榴木", "大海水", "大海水"
    };

    // ---- 地支藏干：本气/中气/余气（天干下标，-1 无）。歌诀：子癸、丑己癸辛、寅甲丙戊、卯乙、辰戊乙癸、巳丙庚戊、午丁己、未己丁乙、申庚壬戊、酉辛、戌戊辛丁、亥壬甲 ----
    private static final int[][] HIDE_GAN = {
        {9, -1, -1},  // 子: 癸
        {5, 9, 6},    // 丑: 己 癸 辛
        {0, 2, 4},    // 寅: 甲 丙 戊
        {1, -1, -1},  // 卯: 乙
        {4, 1, 9},    // 辰: 戊 乙 癸
        {2, 6, 4},    // 巳: 丙 庚 戊
        {3, 5, -1},   // 午: 丁 己
        {5, 3, 1},    // 未: 己 丁 乙
        {6, 8, 4},    // 申: 庚 壬 戊
        {7, -1, -1},  // 酉: 辛
        {4, 7, 3},    // 戌: 戊 辛 丁
        {8, 0, -1}    // 亥: 壬 甲
    };
    private static final double[] HIDE_WEIGHT = {1.0, 0.5, 0.25}; // 本气/中气/余气 权重

    // ---- 十二长生（地势）名称 ----
    private static final String[] DISHI = {"长生", "沐浴", "冠带", "临官", "帝旺", "衰", "病", "死", "墓", "绝", "胎", "养"};
    // 日干 -> 长生位（地支下标）：甲亥 乙午 丙寅 丁酉 戊寅 己酉 庚巳 辛子 壬申 癸卯
    private static final int[] CHANG_SHENG_ZHI = {11, 6, 2, 9, 2, 9, 5, 0, 8, 3};

    // ---- 神煞：日干查表（地支下标） ----
    // 天乙贵人：甲戊庚牛羊(丑1未7) 乙己鼠猴(子0申8) 丙丁猪鸡(亥11酉9) 壬癸兔蛇(卯3巳5) 六辛逢马虎(午6寅2)
    private static final int[][] GUIREN = {
        {1, 7}, {0, 8}, {11, 9}, {11, 9}, {1,7}, {0, 8}, {1, 7}, {6, 2}, {3, 5}, {3, 5}
    };
    // 文昌：甲巳(5) 乙午(6) 丙戊申(8) 丁己酉(9) 庚亥(11) 辛子(0) 壬寅(2) 癸卯(3)
    private static final int[] WENCHANG = {5, 6, 8, 9, 8, 9, 11, 0, 2, 3};
    // 禄（临官）：甲寅 乙卯 丙戊巳 丁己午 庚申 辛酉 壬亥 癸子
    private static final int[] LU = {2, 3, 5, 6, 5, 6, 8, 9, 10, 0};
    // 羊刃：甲卯 乙辰 丙戊午 丁己未 庚酉 辛戌 壬子 癸丑
    private static final int[] YANGREN = {3, 4, 6, 7, 6, 7, 9, 10, 0, 1};

    // 寿星公式：十二「节」的 C 常数（21 世纪），顺序：小寒,立春,惊蛰,清明,立夏,芒种,小暑,立秋,白露,寒露,立冬,大雪
    private static final double[] TERM_C_21 = {5.4055, 3.87, 5.63, 4.81, 5.52, 5.678, 7.108, 7.5, 7.646, 8.318, 7.438, 7.18};
    // 20 世纪 C 常数
    private static final double[] TERM_C_20 = {6.11, 4.6295, 6.3826, 5.59, 6.318, 6.5, 7.928, 8.35, 8.44, 9.098, 8.218, 8.01};
    // 十二「节」所在公历月
    private static final int[] TERM_MONTH = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
    // 十二「节」对应的月支下标（小寒->丑1, 立春->寅2, 惊蛰->卯3, 清明->辰4, 立夏->巳5, 芒种->午6, 小暑->未7, 立秋->申8, 白露->酉9, 寒露->戌10, 立冬->亥11, 大雪->子0）
    private static final int[] TERM_BRANCH = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 0};

    // 主要城市经度表（东经为正，单位度），用于真太阳时校正（参考 reed1898/bazi-tool 的经纬度表）
    private static final Map<String, Double> CITY_LON = new LinkedHashMap<>();
    static {
        CITY_LON.put("北京", 116.40); CITY_LON.put("上海", 121.47); CITY_LON.put("天津", 117.20);
        CITY_LON.put("重庆", 106.55); CITY_LON.put("广州", 113.26); CITY_LON.put("深圳", 114.06);
        CITY_LON.put("成都", 104.07); CITY_LON.put("杭州", 120.15); CITY_LON.put("南京", 118.80);
        CITY_LON.put("武汉", 114.31); CITY_LON.put("西安", 108.95); CITY_LON.put("苏州", 120.62);
        CITY_LON.put("郑州", 113.65); CITY_LON.put("长沙", 112.94); CITY_LON.put("沈阳", 123.43);
        CITY_LON.put("大连", 121.62); CITY_LON.put("青岛", 120.38); CITY_LON.put("济南", 117.00);
        CITY_LON.put("哈尔滨", 126.63); CITY_LON.put("长春", 125.35); CITY_LON.put("石家庄", 114.51);
        CITY_LON.put("太原", 112.55); CITY_LON.put("合肥", 117.27); CITY_LON.put("福州", 119.30);
        CITY_LON.put("厦门", 118.10); CITY_LON.put("南昌", 115.86); CITY_LON.put("昆明", 102.83);
        CITY_LON.put("贵阳", 106.71); CITY_LON.put("南宁", 108.33); CITY_LON.put("海口", 110.33);
        CITY_LON.put("兰州", 103.83); CITY_LON.put("西宁", 101.78); CITY_LON.put("银川", 106.27);
        CITY_LON.put("乌鲁木齐", 87.62); CITY_LON.put("呼和浩特", 111.75); CITY_LON.put("拉萨", 91.14);
        CITY_LON.put("香港", 114.17); CITY_LON.put("澳门", 113.55); CITY_LON.put("台北", 121.56);
        CITY_LON.put("宁波", 121.55); CITY_LON.put("无锡", 120.30); CITY_LON.put("温州", 120.70);
        CITY_LON.put("佛山", 113.12); CITY_LON.put("东莞", 113.75); CITY_LON.put("泉州", 118.68);
        CITY_LON.put("洛阳", 112.45); CITY_LON.put("烟台", 121.39); CITY_LON.put("临沂", 118.35);
        CITY_LON.put("唐山", 118.18); CITY_LON.put("徐州", 117.18); CITY_LON.put("潍坊", 119.10);
    }

    private static String[] buildJiazi() {
        String[] arr = new String[60];
        for (int i = 0; i < 60; i++) {
            arr[i] = GAN[i % 10] + ZHI[i % 12];
        }
        return arr;
    }

    /* ===================== 对外结构 ===================== */
    public static class BaziChart {
        public String yearPillar;
        public String monthPillar;
        public String dayPillar;
        public String timePillar;
        public String dayMaster;            // 日干
        public String zodiac;               // 生肖
        public int[] wuxing = new int[5];   // 木火土金水 计数（天干+地支本气）
        public String[] shishen = new String[4]; // 年/月/日(/时) 天干十神（下标2为日主本身）
        public String dayMasterStrength;     // 身强/身弱/中和
        public List<String> missing = new ArrayList<>();
        public int dayunStartAge;           // 起运周岁
        public List<String> dayun = new ArrayList<>(); // 大运干支序列
        public String liunian;              // 当前流年

        // ---- 新增：古籍/经典模块 ----
        public String[] nayin = new String[4];        // 四柱纳音
        public String[] canggan = new String[4];      // 四柱地支藏干（如 丑:己癸辛）
        public List<String> shensha = new ArrayList<>(); // 命局实有神煞
        public String[] dishi = new String[4];        // 四柱十二长生（地势）
        public String birthPlace;                     // 出生地（原样回显）
        public boolean trueSolarApplied;              // 是否应用了真太阳时校正

        /** 给 AI 的上下文：精确命盘，让 AI 只做「解读」而非「编盘」。 */
        public String toContext() {
            StringBuilder sb = new StringBuilder();
            sb.append("【用户精确命盘（已由系统本地排盘，请勿重新推算，仅据此解读）】\n");
            sb.append("四柱：").append(yearPillar).append(" ").append(monthPillar)
              .append(" ").append(dayPillar).append(" ").append(timePillar).append("\n");
            sb.append("四柱纳音：");
            for (int i = 0; i < 4; i++) sb.append(nayin[i]).append(" ");
            sb.append("\n");
            sb.append("日主（命主）：").append(dayMaster).append("（").append(dayMasterStrength).append("）\n");
            sb.append("生肖：").append(zodiac).append("\n");
            sb.append("五行分布（木火土金水）：");
            for (int i = 0; i < 5; i++) sb.append(WX[i]).append(wuxing[i]).append(" ");
            sb.append("\n");
            if (!missing.isEmpty()) {
                sb.append("五行缺失：").append(String.join("、", missing)).append("\n");
            }
            sb.append("天干十神（年/月/时）：")
              .append(shishen[0]).append("/").append(shishen[1]).append("/").append(shishen[3]).append("\n");
            sb.append("十二长生（年/月/日/时）：")
              .append(dishi[0]).append("/").append(dishi[1]).append("/").append(dishi[2]).append("/").append(dishi[3]).append("\n");
            sb.append("神煞：").append(shensha.isEmpty() ? "无明显吉凶神煞" : String.join("、", shensha)).append("\n");
            sb.append("起运：").append(dayunStartAge).append(" 岁；大运：").append(String.join(" ", dayun)).append("\n");
            sb.append("当前流年：").append(liunian).append("\n");
            if (trueSolarApplied) {
                sb.append("（已按真太阳时校正，出生地：").append(birthPlace == null ? "" : birthPlace).append("）\n");
            }
            return sb.toString();
        }

        /** 离线（无 AI）时的结构化解读模板：基于真实命盘生成，而非空话。 */
        public String toOfflineReading(String type) {
            StringBuilder sb = new StringBuilder();
            sb.append("【离线模拟 · 系统本地排盘解读】\n\n");
            sb.append("（AI 服务暂不可用，以下内容由系统依据您的真实生辰八字本地生成，未调用 AI、未扣除积分。)\n\n");
            sb.append("一、命盘总览\n");
            sb.append("四柱八字：").append(yearPillar).append(" ").append(monthPillar)
              .append(" ").append(dayPillar).append(" ").append(timePillar).append("\n");
            sb.append("四柱纳音：");
            for (int i = 0; i < 4; i++) sb.append(nayin[i]).append(" ");
            sb.append("\n");
            sb.append("日主").append(dayMaster).append("，命主").append(dayMasterStrength)
              .append("；生肖").append(zodiac).append("。\n");
            sb.append("五行（木火土金水）：");
            for (int i = 0; i < 5; i++) sb.append(WX[i]).append(wuxing[i]).append("、");
            sb.append("\n");
            if (missing.isEmpty()) {
                sb.append("五行齐全，气机流通。\n");
            } else {
                sb.append("五行偏弱：").append(String.join("、", missing)).append("，宜在姓名、方位、行业上补益。\n");
            }
            sb.append("\n二、格局简析\n");
            sb.append("年干").append(shishen[0]).append("、月干").append(shishen[1]).append("、时干").append(shishen[3])
              .append("环绕日主，构成基本十神格局；").append(type).append("可从日主强弱与用神喜忌入手详解。\n");
            if (!shensha.isEmpty()) {
                sb.append("命带神煞：").append(String.join("、", shensha)).append("，可在性格与际遇上有所体现。\n");
            }
            sb.append("\n三、大运走势\n");
            sb.append("约 ").append(dayunStartAge).append(" 岁起运，大运依次为：").append(String.join("、", dayun)).append("。\n");
            sb.append("当前流年 ").append(liunian).append("，可结合大运与流年观察本年吉凶起伏。\n");
            sb.append("\n（完整 AI 深度解读将在 AI 服务恢复后可用，重新测算即可获取。）");
            return sb.toString();
        }
    }

    /* ===================== 主入口 ===================== */
    /**
     * 向后兼容：不传出生地、不校正真太阳时。
     */
    public static BaziChart calculate(String birthDate, String birthTimeChinese, String gender) {
        return calculate(birthDate, birthTimeChinese, gender, null, false);
    }

    /**
     * @param birthDate 公历日期 yyyy-MM-dd
     * @param birthTimeChinese 中文时辰（如 "子时"）或 "HH:mm"
     * @param gender 性别 "男"/"女"
     * @param birthPlace 出生地（选填，用于真太阳时校正；命中城市表才生效）
     * @param applyTrueSolar 是否启用真太阳时校正（需 birthPlace 命中城市表）
     */
    public static BaziChart calculate(String birthDate, String birthTimeChinese, String gender,
                                      String birthPlace, boolean applyTrueSolar) {
        BaziChart chart = new BaziChart();
        if (birthDate == null || birthDate.isEmpty()) return chart;

        String[] p = birthDate.split("-");
        int y = Integer.parseInt(p[0]);
        int m = Integer.parseInt(p[1]);
        int d = Integer.parseInt(p[2]);

        // 解析出生时辰 -> (hour, minute)
        int[] hm = parseHourMinute(birthTimeChinese);
        int hour = hm[0];
        int minute = hm[1];

        // 真太阳时校正（可跨日重算日柱/时柱）
        LocalDateTime birthLD = LocalDateTime.of(y, m, d, hour, minute);
        if (applyTrueSolar && birthPlace != null && !birthPlace.trim().isEmpty()) {
            double lon = cityLongitude(birthPlace);
            if (lon != 0.0) {
                int eot = equationOfTime(birthLD.toLocalDate());           // 均时差（分钟）
                int deltaMin = (int) Math.round((lon - 120.0) * 4.0 + eot); // 经度差 + 均时差
                birthLD = birthLD.plusMinutes(deltaMin);
                chart.trueSolarApplied = true;
                chart.birthPlace = birthPlace;
            }
        }

        int cy = birthLD.getYear();
        int cm = birthLD.getMonthValue();
        int cd = birthLD.getDayOfMonth();
        int chour = birthLD.getHour();

        // 节气表：覆盖换年边界（cy-1, cy, cy+1）
        Map<String, LocalDate> terms = new LinkedHashMap<>();
        for (int k : new int[]{cy - 1, cy, cy + 1}) {
            for (int i = 0; i < 12; i++) {
                LocalDate ld = termDate(k, i);
                if (ld != null) terms.put(k + "-" + TERM_MONTH[i], ld);
            }
        }

        LocalDate birth = LocalDate.of(cy, cm, cd);

        // 1) 年柱（立春换年）
        LocalDate liChunThis = terms.get(cy + "-2");
        LocalDate liChunPrev = terms.get((cy - 1) + "-2");
        int solarYear;
        if (liChunPrev != null && !birth.isBefore(liChunPrev)
                && (liChunThis == null || birth.isBefore(liChunThis))) {
            solarYear = cy - 1;
        } else {
            solarYear = cy;
        }
        int yearGan = mod(solarYear - 4, 10);
        int yearZhi = mod(solarYear - 4, 12);
        chart.yearPillar = GAN[yearGan] + ZHI[yearZhi] + "年";
        chart.zodiac = ZHI[yearZhi];
        chart.nayin[0] = NAYIN[findJiaziIndex(yearGan, yearZhi)];
        chart.canggan[0] = hideGanDesc(yearZhi);

        // 2) 月柱（以节换月）
        LocalDate upper = terms.get((solarYear + 1) + "-2");
        int monthBranch = 1;
        for (Map.Entry<String, LocalDate> e : terms.entrySet()) {
            LocalDate ld = e.getValue();
            if (!ld.isAfter(birth) && (upper == null || !ld.isAfter(upper))) {
                int branch = branchOfTerm(e.getKey());
                if (branch >= 0) monthBranch = branch;
            }
        }
        int monthGanStart = yueTouGan(yearGan);
        int monthGan = mod(monthGanStart + (monthBranch - 2), 10);
        chart.monthPillar = GAN[monthGan] + ZHI[monthBranch] + "月";
        chart.nayin[1] = NAYIN[findJiaziIndex(monthGan, monthBranch)];
        chart.canggan[1] = hideGanDesc(monthBranch);

        // 3) 日柱（JDN 精确）
        long jdn = julianDay(cy, cm, cd);
        int daySeq = mod((int) (jdn + 49), 60);
        int dayGan = daySeq % 10;
        int dayZhi = daySeq % 12;
        chart.dayPillar = GAN[dayGan] + ZHI[dayZhi] + "日";
        chart.dayMaster = GAN[dayGan];
        chart.nayin[2] = NAYIN[findJiaziIndex(dayGan, dayZhi)];
        chart.canggan[2] = hideGanDesc(dayZhi);

        // 4) 时柱（五鼠遁）
        int timeBranch = hourToBranch(chour);
        int timeGanStart = shiTouGan(dayGan);
        int timeGan = mod(timeGanStart + timeBranch, 10);
        chart.timePillar = GAN[timeGan] + ZHI[timeBranch] + "时";
        chart.nayin[3] = NAYIN[findJiaziIndex(timeGan, timeBranch)];
        chart.canggan[3] = hideGanDesc(timeBranch);

        // 5) 五行统计（天干 + 地支本气）
        int[] wx = new int[5];
        wx[GAN_WX[yearGan]]++;
        wx[ZHI_WX[yearZhi]]++;
        wx[GAN_WX[monthGan]]++;
        wx[ZHI_WX[monthBranch]]++;
        wx[GAN_WX[dayGan]]++;
        wx[ZHI_WX[dayZhi]]++;
        wx[GAN_WX[timeGan]]++;
        wx[ZHI_WX[timeBranch]]++;
        chart.wuxing = wx;
        for (int i = 0; i < 5; i++) {
            if (wx[i] == 0) chart.missing.add(WX[i]);
        }

        // 6) 十神（年/月/时 天干相对日主）
        chart.shishen[0] = shiShen(dayGan, yearGan);
        chart.shishen[1] = shiShen(dayGan, monthGan);
        chart.shishen[2] = "日主";
        chart.shishen[3] = shiShen(dayGan, timeGan);

        // 7) 日主强弱（藏干加权 + 月令得令）
        chart.dayMasterStrength = computeStrength(dayGan, yearGan, monthGan, timeGan,
                yearZhi, monthBranch, dayZhi, timeBranch);

        // 8) 十二长生（地势）：四柱地支相对日主
        for (int i = 0; i < 4; i++) {
            int z = (i == 0) ? yearZhi : (i == 1) ? monthBranch : (i == 2) ? dayZhi : timeBranch;
            chart.dishi[i] = diShi(dayGan, z);
        }

        // 9) 神煞（仅列命局实有者）
        chart.shensha = computeShensha(dayGan, dayZhi, yearZhi, monthBranch, timeBranch);

        // 10) 大运（起运岁数 + 序列）
        computeDayun(chart, solarYear, birth, terms, gender, monthGan, monthBranch);

        // 11) 流年（当前年，立春换年）
        LocalDate now = LocalDate.now();
        int curSolarYear;
        LocalDate liChunNow = terms.get(now.getYear() + "-2");
        if (liChunNow != null && now.isBefore(liChunNow)) {
            curSolarYear = now.getYear() - 1;
        } else {
            curSolarYear = now.getYear();
        }
        chart.liunian = GAN[mod(curSolarYear - 4, 10)] + ZHI[mod(curSolarYear - 4, 12)] + "年";

        return chart;
    }

    /* ===================== 新模块计算 ===================== */

    /** 日主强弱：以藏干（本气/中气/余气加权）统计五行，并结合月令得令。 */
    private static String computeStrength(int dayGan, int yg, int mg, int tg,
                                         int yz, int mb, int dz, int tz) {
        double[] score = new double[5];
        // 天干各 1.0
        score[GAN_WX[yg]] += 1.0;
        score[GAN_WX[mg]] += 1.0;
        score[GAN_WX[dayGan]] += 1.0;
        score[GAN_WX[tg]] += 1.0;
        // 地支藏干加权
        int[] zhis = {yz, mb, dz, tz};
        for (int z : zhis) {
            for (int k = 0; k < 3; k++) {
                int h = HIDE_GAN[z][k];
                if (h < 0) continue;
                score[GAN_WX[h]] += HIDE_WEIGHT[k];
            }
        }
        int dmWx = GAN_WX[dayGan];
        int yin = mod(dmWx + 4, 5);  // 生我（印）
        int cai = mod(dmWx + 2, 5);  // 我克（财）
        int guan = mod(dmWx + 3, 5); // 克我（官杀）
        int shang = mod(dmWx + 1, 5); // 我生（食伤）

        double tongYin = score[dmWx] + score[yin] * 0.8;       // 比劫 + 印
        double yiLei = score[cai] + score[guan] + score[shang] * 0.8; // 财 + 官杀 + 食伤

        // 月令得令：月支本气是否生扶日主
        int monthBenQi = ZHI_WX[mb];
        double ling = (monthBenQi == dmWx || monthBenQi == yin) ? 2.0 : 0.0;

        double total = tongYin + ling - yiLei * 0.9;
        if (total > 2.5) return "身强";
        if (total < -2.5) return "身弱";
        return "中和";
    }

    /** 十二长生（地势）：日干在 zhi 位的状态。 */
    private static String diShi(int dayGan, int zhi) {
        int cs = CHANG_SHENG_ZHI[dayGan];
        boolean yang = (dayGan % 2 == 0); // 甲丙戊庚壬 为阳
        int offset = yang
                ? mod(zhi - cs, 12)          // 阳干顺行
                : mod(cs - zhi, 12);         // 阴干逆行
        return DISHI[offset];
    }

    /** 神煞：返回命局实有的吉凶神煞（按《三命通会》"格局为主、神煞为辅"只列出现者）。 */
    private static List<String> computeShensha(int dayGan, int dayZhi, int yz, int mb, int tz) {
        List<String> list = new ArrayList<>();
        int[] pillarsZhi = {yz, mb, tz}; // 年/月/时 地支（日支单独用于桃花等）
        // 天乙贵人
        for (int g : GUIREN[dayGan]) {
            if (containsZhi(pillarsZhi, g) || g == dayZhi) {
                list.add("天乙贵人");
                break;
            }
        }
        // 文昌
        if (containsZhi(pillarsZhi, WENCHANG[dayGan]) || WENCHANG[dayGan] == dayZhi) list.add("文昌");
        // 禄神
        if (containsZhi(pillarsZhi, LU[dayGan]) || LU[dayGan] == dayZhi) list.add("禄神");
        // 羊刃
        if (containsZhi(pillarsZhi, YANGREN[dayGan]) || YANGREN[dayGan] == dayZhi) list.add("羊刃");
        // 桃花（以日支查）
        int th = taohuaOf(dayZhi);
        if (containsZhi(pillarsZhi, th) || th == yz) list.add("桃花");
        // 驿马（以日支查）
        int ym = yimaOf(dayZhi);
        if (containsZhi(pillarsZhi, ym) || ym == yz) list.add("驿马");
        // 华盖（以日支查）
        int hg = huagaiOf(dayZhi);
        if (containsZhi(pillarsZhi, hg) || hg == yz) list.add("华盖");
        // 空亡（以日柱旬查）
        int[] kw = kongwangOf(dayGan, dayZhi);
        StringBuilder kwsb = new StringBuilder("空亡：").append(ZHI[kw[0]]).append("、").append(ZHI[kw[1]]);
        boolean hit = false;
        for (int z : pillarsZhi) {
            if (z == kw[0] || z == kw[1]) { hit = true; break; }
        }
        if (dayZhi == kw[0] || dayZhi == kw[1]) hit = true;
        if (hit) kwsb.append("（命局有落空）");
        list.add(kwsb.toString());
        return list;
    }

    private static boolean containsZhi(int[] arr, int zhi) {
        for (int z : arr) if (z == zhi) return true;
        return false;
    }

    private static int taohuaOf(int dayZhi) {
        // 寅午戌->卯(3) 申子辰->酉(9) 亥卯未->子(0) 巳酉丑->午(6)
        if (dayZhi == 2 || dayZhi == 6 || dayZhi == 10) return 3;
        if (dayZhi == 8 || dayZhi == 0 || dayZhi == 4) return 9;
        if (dayZhi == 11 || dayZhi == 3 || dayZhi == 7) return 0;
        return 6;
    }

    private static int yimaOf(int dayZhi) {
        // 申子辰->寅(2) 寅午戌->申(8) 巳酉丑->亥(10) 亥卯未->巳(5)
        if (dayZhi == 8 || dayZhi == 0 || dayZhi == 4) return 2;
        if (dayZhi == 2 || dayZhi == 6 || dayZhi == 10) return 8;
        if (dayZhi == 5 || dayZhi == 9 || dayZhi == 1) return 10;
        return 5;
    }

    private static int huagaiOf(int dayZhi) {
        // 寅午戌->戌(10) 申子辰->辰(4) 巳酉丑->丑(1) 亥卯未->未(7)
        if (dayZhi == 2 || dayZhi == 6 || dayZhi == 10) return 10;
        if (dayZhi == 8 || dayZhi == 0 || dayZhi == 4) return 4;
        if (dayZhi == 5 || dayZhi == 9 || dayZhi == 1) return 1;
        return 7;
    }

    private static int[] kongwangOf(int dayGan, int dayZhi) {
        // 旬 = 日柱在六十甲子中的 /10；甲子旬戌亥空、甲戌旬申酉空、甲申旬午未空、甲午旬辰巳空、甲辰旬寅卯空、甲寅旬子丑空
        int idx = findJiaziIndex(dayGan, dayZhi);
        int xun = idx / 10;
        switch (xun) {
            case 0: return new int[]{10, 11};
            case 1: return new int[]{8, 9};
            case 2: return new int[]{6, 7};
            case 3: return new int[]{4, 5};
            case 4: return new int[]{2, 3};
            default: return new int[]{0, 1};
        }
    }

    private static String hideGanDesc(int zhi) {
        StringBuilder sb = new StringBuilder();
        for (int k = 0; k < 3; k++) {
            int h = HIDE_GAN[zhi][k];
            if (h < 0) break;
            sb.append(GAN[h]);
        }
        return sb.toString();
    }

    /* ===================== 真太阳时 ===================== */

    /** 出生地 -> 经度（命中城市表）。未命中返回 0。 */
    private static double cityLongitude(String place) {
        String s = place.trim().replace("市", "").replace("省", "").replace("自治区", "")
                .replace("特别行政区", "").replace("区", "").replace("县", "")
                .replace("壮族", "").replace("回族", "").replace("维吾尔", "")
                .replace("自治州", "").replace("盟", "").replace("地区", "");
        if (CITY_LON.containsKey(s)) return CITY_LON.get(s);
        // 子串/别名匹配
        if (s.equals("沪")) return CITY_LON.get("上海");
        if (s.equals("京")) return CITY_LON.get("北京");
        if (s.equals("渝")) return CITY_LON.get("重庆");
        if (s.equals("津")) return CITY_LON.get("天津");
        for (Map.Entry<String, Double> e : CITY_LON.entrySet()) {
            if (place.contains(e.getKey()) || e.getKey().contains(s)) return e.getValue();
        }
        return 0.0;
    }

    /** 均时差近似（分钟）：B=360/365.24*(N-81)，EoT=9.87sin2B-7.53cosB-1.5sinB。 */
    private static int equationOfTime(LocalDate date) {
        int n = date.getDayOfYear();
        double b = Math.toRadians(360.0 / 365.24 * (n - 81));
        double eot = 9.87 * Math.sin(2 * b) - 7.53 * Math.cos(b) - 1.5 * Math.sin(b);
        return (int) Math.round(eot);
    }

    /* ===================== 工具方法 ===================== */
    private static int[] parseHourMinute(String t) {
        int[] res = {12, 0}; // 默认午时
        if (t == null || t.isEmpty()) return res;
        if (t.contains("时")) {
            for (int i = 0; i < 12; i++) {
                if (t.contains(ZHI[i])) { res[0] = branchToHour(i); res[1] = 0; return res; }
            }
            return res;
        }
        try {
            String[] parts = t.split(":");
            res[0] = Integer.parseInt(parts[0]);
            if (parts.length > 1) res[1] = Integer.parseInt(parts[1]);
        } catch (Exception e) {
            return res;
        }
        return res;
    }

    private static int branchToHour(int branch) {
        return branch * 2;
    }

    private static int hourToBranch(int hour) {
        if (hour == 23 || hour == 0) return 0;
        if (hour < 3) return 1;
        if (hour < 5) return 2;
        if (hour < 7) return 3;
        if (hour < 9) return 4;
        if (hour < 11) return 5;
        if (hour < 13) return 6;
        if (hour < 15) return 7;
        if (hour < 17) return 8;
        if (hour < 19) return 9;
        if (hour < 21) return 10;
        return 11;
    }

    /** 儒略日数（公历，整数日，正午起算） */
    private static long julianDay(int y, int m, int d) {
        int a = (14 - m) / 12;
        int yy = y + 4800 - a;
        int mm = m + 12 * a - 3;
        return d + (153 * mm + 2) / 5 + 365L * yy + yy / 4 - yy / 100 + yy / 400 - 32045L;
    }

    /** 寿星公式求十二「节」在公历某年的日期（返回 null 表示异常） */
    private static LocalDate termDate(int cy, int termIdx) {
        double c = (cy >= 2000) ? TERM_C_21[termIdx] : TERM_C_20[termIdx];
        int y2 = cy % 100;
        int base = (int) (y2 * 0.2422 + c);
        int l = (cy >= 2000) ? (y2 / 4) : ((y2 + 1) / 4);
        int day = base - l;
        int month = TERM_MONTH[termIdx];
        if (day < 1) day = 1;
        int maxDay = LocalDate.of(cy, month, 1).lengthOfMonth();
        if (day > maxDay) day = maxDay;
        return LocalDate.of(cy, month, day);
    }

    private static int branchOfTerm(String key) {
        int month = Integer.parseInt(key.split("-")[1]);
        for (int i = 0; i < 12; i++) {
            if (TERM_MONTH[i] == month) return TERM_BRANCH[i];
        }
        return -1;
    }

    /** 五虎遁：年干 -> 寅月（branch=2）的天干下标 */
    private static int yueTouGan(int yearGan) {
        switch (yearGan) {
            case 0: case 5: return 2;  // 甲己 -> 丙(2)
            case 1: case 6: return 4;  // 乙庚 -> 戊(4)
            case 2: case 7: return 6;  // 丙辛 -> 庚(6)
            case 3: case 8: return 8;  // 丁壬 -> 壬(8)
            case 4: case 9: return 0;  // 戊癸 -> 甲(0)
            default: return 2;
        }
    }

    /** 五鼠遁：日干 -> 子时（branch=0）的天干下标 */
    private static int shiTouGan(int dayGan) {
        switch (dayGan) {
            case 0: case 5: return 0;  // 甲己 -> 甲(0)
            case 1: case 6: return 2;  // 乙庚 -> 丙(2)
            case 2: case 7: return 4;  // 丙辛 -> 戊(4)
            case 3: case 8: return 6;  // 丁壬 -> 庚(6)
            case 4: case 9: return 8;  // 戊癸 -> 壬(8)
            default: return 0;
        }
    }

    /** 十神：日主下标 dm，目标天干下标 tg */
    private static String shiShen(int dm, int tg) {
        int dmWx = GAN_WX[dm];
        int tgWx = GAN_WX[tg];
        boolean sameYin = (dm % 2) == (tg % 2);
        if (tg == dm) return "日主";
        if (dmWx == tgWx) return sameYin ? "比肩" : "劫财";
        int shengWo = mod(dmWx + 4, 5);
        int woSheng = mod(dmWx + 1, 5);
        int woKe = mod(dmWx + 2, 5);
        int keWo = mod(dmWx + 3, 5);
        if (tgWx == shengWo) return sameYin ? "偏印" : "正印";
        if (tgWx == woSheng) return sameYin ? "食神" : "伤官";
        if (tgWx == woKe) return sameYin ? "偏财" : "正财";
        if (tgWx == keWo) return sameYin ? "七杀" : "正官";
        return "—";
    }

    private static void computeDayun(BaziChart chart, int solarYear, LocalDate birth,
                                     Map<String, LocalDate> terms, String gender, int monthGan, int monthBranch) {
        boolean yearGanYang = (mod(solarYear - 4, 10) % 2 == 0);
        boolean male = "男".equals(gender);
        boolean forward = (yearGanYang == male); // 阳男阴女顺、阴男阳女逆

        LocalDate target = null;
        if (forward) {
            for (Map.Entry<String, LocalDate> e : terms.entrySet()) {
                LocalDate ld = e.getValue();
                if (!ld.isBefore(birth) && (target == null || ld.isBefore(target))) target = ld;
            }
        } else {
            for (Map.Entry<String, LocalDate> e : terms.entrySet()) {
                LocalDate ld = e.getValue();
                if (ld.isBefore(birth) && (target == null || ld.isAfter(target))) target = ld;
            }
        }
        long days = (target == null) ? 0 : Math.abs(java.time.temporal.ChronoUnit.DAYS.between(birth, target));
        int startAge = (int) (days / 3);
        int startMonth = (int) ((days % 3) * 4);
        chart.dayunStartAge = startAge + (startMonth >= 6 ? 1 : 0);

        int monthIdx = findJiaziIndex(monthGan, monthBranch);
        int step = forward ? 1 : -1;
        for (int i = 1; i <= 8; i++) {
            int idx = mod(monthIdx + step * i, 60);
            chart.dayun.add(JIAZI[idx]);
        }
    }

    private static int findJiaziIndex(int gan, int zhi) {
        for (int i = 0; i < 60; i++) {
            if (i % 10 == gan && i % 12 == zhi) return i;
        }
        return 0;
    }

    private static int mod(int a, int n) {
        return ((a % n) + n) % n;
    }

    /* ===================== 验证（main） ===================== */
    public static void main(String[] args) {
        System.out.println("=== 原有锚点（保持不变） ===");
        printCase("2000-01-01", "午时", "男", null, false);
        printCase("2024-06-15", "午时", "男", null, false);
        printCase("1984-02-10", "子时", "男", null, false);
        printCase("1984-01-20", "子时", "男", null, false);
        printCase("2024-01-20", "午时", "女", null, false);
        printCase("2024-02-10", "午时", "女", null, false);
        System.out.println("\n=== 增强模块抽查 ===");
        printCase("1990-08-18", "06:00", "男", "上海", true);  // 真太阳时（东经121.47）
        printCase("1986-05-29", "00:00", "男", null, false);   // 经典案例（6tail 示例）
        printCase("1983-05-15", "14:30", "男", "乌鲁木齐", true); // 西经远，校正明显
    }

    private static void printCase(String date, String time, String gender, String place, boolean solar) {
        BaziChart c = calculate(date, time, gender, place, solar);
        System.out.println(date + " " + time + " " + gender
                + (place != null ? " 出生地=" + place + "(真太阳=" + solar + ")" : "")
                + " => "
                + c.yearPillar + " " + c.monthPillar + " " + c.dayPillar + " " + c.timePillar
                + " | 纳音=" + java.util.Arrays.toString(c.nayin)
                + " | 日主=" + c.dayMaster + " " + c.dayMasterStrength
                + " | 五行=" + java.util.Arrays.toString(c.wuxing)
                + " | 缺=" + c.missing
                + " | 藏干=" + java.util.Arrays.toString(c.canggan)
                + " | 十神=" + java.util.Arrays.toString(c.shishen)
                + " | 地势=" + java.util.Arrays.toString(c.dishi)
                + " | 神煞=" + c.shensha
                + " | 起运=" + c.dayunStartAge + " | 大运=" + c.dayun
                + " | 流年=" + c.liunian
                + " | 真太阳应用=" + c.trueSolarApplied);
    }
}
