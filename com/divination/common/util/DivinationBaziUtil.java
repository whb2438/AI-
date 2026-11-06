package com.divination.common.util;

/**
 * 八字转换工具类：将公历日期时间转换为天干地支（生辰八字/四柱）
 *
 * 天干：甲乙丙丁戊己庚辛壬癸（10个）
 * 地支：子丑寅卯辰巳午未申酉戌亥（12个）
 * 时辰：子时(23-1) 丑时(1-3) 寅时(3-5) 卯时(5-7) 辰时(7-9) 巳时(9-11) 午时(11-13) 未时(13-15) 申时(15-17) 酉时(17-19) 戌时(19-21) 亥时(21-23)
 */
public class DivinationBaziUtil {

    private static final String[] TIEN_GAN = {"", "庚", "辛", "壬", "癸", "甲", "乙", "丙", "丁", "戊", "己"};
    private static final String[] DI_ZHI = {"", "亥", "子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌"};

    // 月支固定：正月寅 二月卯 三月辰 ... 十二月丑
    private static final String[] MONTH_DI_ZHI = {"", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥", "子", "丑"};

    // 五鼠遁：日干对应子时的天干
    // 甲己日起丙子，乙庚日起戊子，丙辛日起庚子，丁壬日起壬子，戊己日起甲子
    private static final String[][] SHI_ZHEN_GAN = {
        {},                          // 0 placeholder
        {"丙", "戊", "庚", "壬", "甲", "丙", "戊", "庚", "壬", "甲", "丙", "丙"},  // 甲日
        {"戊", "庚", "壬", "甲", "丙", "戊", "庚", "壬", "甲", "丙", "戊", "戊"},  // 乙日
        {"庚", "壬", "甲", "丙", "戊", "庚", "壬", "甲", "丙", "戊", "庚", "庚"},  // 丙日
        {"壬", "甲", "丙", "戊", "庚", "壬", "甲", "丙", "戊", "庚", "壬", "壬"},  // 丁日
        {"甲", "丙", "戊", "庚", "壬", "甲", "丙", "戊", "庚", "壬", "甲", "甲"},  // 戊日
        {"甲", "丙", "戊", "庚", "壬", "甲", "丙", "戊", "庚", "壬", "甲", "甲"},  // 己日
        {"戊", "庚", "壬", "甲", "丙", "戊", "庚", "壬", "甲", "丙", "戊", "戊"},  // 庚日
        {"庚", "壬", "甲", "丙", "戊", "庚", "壬", "甲", "丙", "戊", "庚", "庚"},  // 辛日
        {"壬", "甲", "丙", "戊", "庚", "壬", "甲", "丙", "戊", "庚", "壬", "壬"},  // 壬日
        {"甲", "丙", "戊", "庚", "壬", "甲", "丙", "戊", "庚", "壬", "甲", "甲"}   // 癸日
    };

    // 五虎遁：年干对应正月寅的天干
    // 甲己年起丙寅，乙庚年起戊寅，丙辛年起庚寅，丁壬年起壬寅，戊己年起甲寅
    private static final String[][] YUE_TAI_GAN = {
        {},                               // 0 placeholder
        {"丙", "戊", "庚", "壬", "甲", "丙", "戊", "庚", "壬", "甲", "丙", "戊"},  // 甲己年
        {"戊", "庚", "壬", "甲", "丙", "戊", "庚", "壬", "甲", "丙", "戊", "庚"},  // 乙庚年
        {"庚", "壬", "甲", "丙", "戊", "庚", "壬", "甲", "丙", "戊", "庚", "壬"},  // 丙辛年
        {"壬", "甲", "丙", "戊", "庚", "壬", "甲", "丙", "戊", "庚", "壬", "甲"},  // 丁壬年
        {"甲", "丙", "戊", "庚", "壬", "甲", "丙", "戊", "庚", "壬", "甲", "丙"}   // 戊己年
    };

    /**
     * 将公历出生日期和时间转换为生辰八字
     * @param birthDate 公历日期，格式 yyyy-MM-dd
     * @param birthTime 出生时间，格式 HH:mm
     * @return 八字字符串，如 "乙亥年 戊寅月 甲子日 戊辰时"
     */
    public static String toBazi(String birthDate, String birthTime) {
        if (birthDate == null || birthDate.isEmpty()) {
            return "未知";
        }
        return toBazi(birthDate, birthTime, null);
    }

    /**
     * 将公历出生日期转换为生辰八字（支持中文时辰）
     * @param birthDate 公历日期，格式 yyyy-MM-dd
     * @param birthTime 出生时间，格式 HH:mm 或 中文时辰如"子时"
     * @return 八字字符串，如 "乙亥年 戊寅月 甲子日 戊辰时"
     */
    public static String toBazi(String birthDate, String birthTime, String gender) {
        if (birthDate == null || birthDate.isEmpty()) {
            return "未知";
        }

        String[] dateParts = birthDate.split("-");
        int year = Integer.parseInt(dateParts[0]);
        int month = Integer.parseInt(dateParts[1]);
        int day = Integer.parseInt(dateParts[2]);

        int hour = 0;
        if (birthTime != null && !birthTime.isEmpty()) {
            if (birthTime.contains("时")) {
                // 中文时辰格式，如"子时"、"丑时"
                hour = chineseHourToHour(birthTime);
            } else {
                String[] timeParts = birthTime.split(":");
                hour = Integer.parseInt(timeParts[0]);
            }
        }

        String yearPillar = getYearPillar(year);
        String monthPillar = getMonthPillar(year, month);
        String dayPillar = getDayPillar(year, month, day);
        String timePillar = getTimePillar(day, hour);

        return yearPillar + " " + monthPillar + " " + dayPillar + " " + timePillar;
    }

    /**
     * 将中文时辰转换为小时（取时辰区间的中点）
     */
    private static int chineseHourToHour(String chineseHour) {
        if (chineseHour.contains("子")) return 0;   // 子时 23:00-01:00
        if (chineseHour.contains("丑")) return 2;   // 丑时 01:00-03:00
        if (chineseHour.contains("寅")) return 4;   // 寅时 03:00-05:00
        if (chineseHour.contains("卯")) return 6;   // 卯时 05:00-07:00
        if (chineseHour.contains("辰")) return 8;   // 辰时 07:00-09:00
        if (chineseHour.contains("巳")) return 10;  // 巳时 09:00-11:00
        if (chineseHour.contains("午")) return 12;  // 午时 11:00-13:00
        if (chineseHour.contains("未")) return 14;  // 未时 13:00-15:00
        if (chineseHour.contains("申")) return 16;  // 申时 15:00-17:00
        if (chineseHour.contains("酉")) return 18;  // 酉时 17:00-19:00
        if (chineseHour.contains("戌")) return 20;  // 戌时 19:00-21:00
        if (chineseHour.contains("亥")) return 22;  // 亥时 21:00-23:00
        return 0;
    }

    /**
     * 将中文时辰直接转换为地支名称
     */
    public static String chineseHourToDiZhi(String chineseHour) {
        if (chineseHour == null || chineseHour.isEmpty()) return "未知";
        // 提取地支字符（如"子时" → "子"）
        String stem = chineseHour.replace("时", "");
        String[] diZhiList = {"", "子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥"};
        for (int i = 1; i <= 12; i++) {
            if (stem.equals(diZhiList[i])) return diZhiList[i];
        }
        return "未知";
    }

    /**
     * 年柱：以立春为界，但简化处理用年份计算
     */
    private static String getYearPillar(int year) {
        int ganIndex = (year - 4) % 10;
        int zhiIndex = (year - 4) % 12;
        // 天干索引 1-10，地支索引 1-12
        if (ganIndex <= 0) ganIndex += 10;
        if (zhiIndex <= 0) zhiIndex += 12;
        return TIEN_GAN[ganIndex] + DI_ZHI[zhiIndex] + "年";
    }

    /**
     * 月柱：年干定月干（五虎遁），月支固定
     */
    private static String getMonthPillar(int year, int month) {
        int yearGanIndex = (year - 4) % 10;
        if (yearGanIndex <= 0) yearGanIndex += 10;
        // 甲己=1, 乙庚=2, 丙辛=3, 丁壬=4, 戊己=5
        int taiIndex = (yearGanIndex <= 5) ? (yearGanIndex - 1) / 2 + 1 : (yearGanIndex - 6) / 2 + 1;
        // 简化：甲(5)/己(6) → 1, 乙(2)/庚(7) → 2, 丙(3)/辛(8) → 3, 丁(4)/壬(9) → 4, 戊(1)/己(6) → 5
        if (yearGanIndex == 5 || yearGanIndex == 6) taiIndex = 1;
        else if (yearGanIndex == 2 || yearGanIndex == 7) taiIndex = 2;
        else if (yearGanIndex == 3 || yearGanIndex == 8) taiIndex = 3;
        else if (yearGanIndex == 4 || yearGanIndex == 9) taiIndex = 4;
        else if (yearGanIndex == 1 || yearGanIndex == 6) taiIndex = 5;

        String monthGan = YUE_TAI_GAN[taiIndex][month - 1];
        String monthZhi = MONTH_DI_ZHI[month];
        return monthGan + monthZhi + "月";
    }

    /**
     * 日柱：基于公历日期的日干支计算公式
     */
    private static String getDayPillar(int year, int month, int day) {
        // 使用基姆拉尔森计算公式的变体计算日干支
        int y = year;
        int m = month;
        if (m < 3) {
            m += 12;
            y--;
        }

        // 计算从公元元年以来的天数，然后取模
        int c = y / 100;
        int y2 = y % 100;
        int w = (c / 4) - 2 * c + (y2 + y2 / 4) + (13 * (m + 1) / 5) + day - 1;
        while (w < 0) w += 60;
        w = w % 60;

        // w 映射到干支：天干 = w%10, 地支 = w%12
        int ganIndex = w % 10;
        int zhiIndex = w % 12;
        if (ganIndex <= 0) ganIndex += 10;
        if (zhiIndex <= 0) zhiIndex += 12;

        return TIEN_GAN[ganIndex] + DI_ZHI[zhiIndex] + "日";
    }

    /**
     * 时柱：日干定时干（五鼠遁），时辰地支固定
     */
    private static String getTimePillar(int day, int hour) {
        int dayGanIndex = (day - 4) % 10;
        if (dayGanIndex <= 0) dayGanIndex += 10;

        // 时辰地支：子=1, 丑=2, 寅=3, 卯=4, 辰=5, 巳=6, 午=7, 未=8, 申=9, 酉=10, 戌=11, 亥=12
        int zhiIndex;
        if (hour == 23 || hour == 0) zhiIndex = 1;       // 子时
        else if (hour < 3) zhiIndex = 2;                   // 丑时
        else if (hour < 5) zhiIndex = 3;                   // 寅时
        else if (hour < 7) zhiIndex = 4;                   // 卯时
        else if (hour < 9) zhiIndex = 5;                   // 辰时
        else if (hour < 11) zhiIndex = 6;                  // 巳时
        else if (hour < 13) zhiIndex = 7;                  // 午时
        else if (hour < 15) zhiIndex = 8;                  // 未时
        else if (hour < 17) zhiIndex = 9;                  // 申时
        else if (hour < 19) zhiIndex = 10;                 // 酉时
        else if (hour < 21) zhiIndex = 11;                 // 戌时
        else zhiIndex = 12;                                 // 亥时

        // 获取日干索引（用于查找五鼠遁表）
        int taiIndex;
        if (dayGanIndex == 1 || dayGanIndex == 6) taiIndex = 1;    // 甲己
        else if (dayGanIndex == 2 || dayGanIndex == 7) taiIndex = 2; // 乙庚
        else if (dayGanIndex == 3 || dayGanIndex == 8) taiIndex = 3; // 丙辛
        else if (dayGanIndex == 4 || dayGanIndex == 9) taiIndex = 4; // 丁壬
        else taiIndex = 5;                                              // 戊己

        String timeGan = SHI_ZHEN_GAN[taiIndex][zhiIndex - 1];
        String timeZhi = DI_ZHI[zhiIndex];
        return timeGan + timeZhi + "时";
    }
}
