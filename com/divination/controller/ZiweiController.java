package com.divination.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.divination.common.result.Result;
import com.divination.entity.ZiweiRecord;
import com.divination.service.ZiweiService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

/**
 * 紫微斗数测算接口 — 排盘引擎 + AI 解读
 */
@RestController
@RequestMapping("/api/ziwei")
public class ZiweiController {

    private final ZiweiService ziweiService;

    public ZiweiController(ZiweiService ziweiService) {
        this.ziweiService = ziweiService;
    }

    /**
     * 创建紫微斗数测算（排盘 + AI 解读 + 积分计费）
     */
    @PostMapping("/create")
    public Result<ZiweiRecord> create(@Valid @RequestBody ZiweiCreateRequest request,
                                      HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        ZiweiRecord record = ziweiService.createZiweiDivination(
                userId,
                request.getBirthDate(),
                request.getBirthTimeZhi(),
                request.getGender(),
                request.getName()
        );
        return Result.success(record);
    }

    /**
     * 分页查询我的紫微测算记录（列表不含命盘与解读大字段，节省流量）
     */
    @GetMapping("/records")
    public Result<IPage<ZiweiRecord>> records(@RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "10") int size,
                                              HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        IPage<ZiweiRecord> records = ziweiService.listRecords(userId, page, Math.min(size, 50));
        records.getRecords().forEach(r -> {
            r.setPalaceData(null);
            r.setResult(null);
        });
        return Result.success(records);
    }

    /**
     * 获取单条紫微测算详情（含命盘JSON与AI解读）
     */
    @GetMapping("/record/{id}")
    public Result<ZiweiRecord> detail(@PathVariable Long id, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        return Result.success(ziweiService.getRecord(userId, id));
    }

    /**
     * 删除我的紫微测算记录
     */
    @DeleteMapping("/record/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        ZiweiRecord record = ziweiService.getRecord(userId, id);
        ziweiService.removeById(record.getId());
        return Result.success();
    }

    public static class ZiweiCreateRequest {
        @NotBlank(message = "出生日期不能为空")
        @Size(max = 20)
        private String birthDate;      // 公历 yyyy-MM-dd

        @NotBlank(message = "出生时辰不能为空")
        @Size(max = 4)
        private String birthTimeZhi;   // 十二地支

        @NotBlank(message = "性别不能为空")
        @Size(max = 4)
        private String gender;         // 男/女

        @Size(max = 32)
        private String name;           // 可选称呼

        public String getBirthDate() { return birthDate; }
        public void setBirthDate(String birthDate) { this.birthDate = birthDate; }
        public String getBirthTimeZhi() { return birthTimeZhi; }
        public void setBirthTimeZhi(String birthTimeZhi) { this.birthTimeZhi = birthTimeZhi; }
        public String getGender() { return gender; }
        public void setGender(String gender) { this.gender = gender; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
}
