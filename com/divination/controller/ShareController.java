package com.divination.controller;

import com.divination.common.result.Result;
import com.divination.entity.InviteRecord;
import com.divination.entity.User;
import com.divination.mapper.InviteRecordMapper;
import com.divination.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 邀请/分享控制器
 */
@RestController
@RequestMapping("/api/share")
public class ShareController {

    private final UserMapper userMapper;
    private final InviteRecordMapper inviteRecordMapper;

    public ShareController(UserMapper userMapper, InviteRecordMapper inviteRecordMapper) {
        this.userMapper = userMapper;
        this.inviteRecordMapper = inviteRecordMapper;
    }

    /**
     * 获取当前用户的邀请码和分享链接
     */
    @GetMapping("/my-invite")
    public Result<Map<String, Object>> getMyInvite(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.error(401, "未登录");

        User user = userMapper.selectById(userId);
        String inviteCode = user.getInviteCode();

        Map<String, Object> data = new HashMap<>();
        data.put("inviteCode", inviteCode);
        data.put("inviteUrl", "/register?code=" + inviteCode);
        data.put("invitationCount", inviteRecordMapper.getInvitationCount(userId));

        Integer totalReward = inviteRecordMapper.getTotalRewardIntegral(userId);
        data.put("totalRewardIntegral", totalReward != null ? totalReward : 0);
        data.put("rewardPerInvite", 20);

        return Result.success(data);
    }

    /**
     * 注册时通过邀请码注册，奖励双方积分
     */
    @PostMapping("/register-with-code")
    public Result<Void> registerWithInviteCode(HttpServletRequest request,
                                                @RequestParam String inviteCode) {
        Long inviteeId = (Long) request.getAttribute("userId");
        if (inviteeId == null) return Result.error(401, "未登录");

        // 查找邀请人
        var wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>();
        wrapper.eq(User::getInviteCode, inviteCode);
        User inviter = userMapper.selectOne(wrapper);

        if (inviter != null && !inviter.getId().equals(inviteeId)) {
            // 记录邀请关系
            InviteRecord record = new InviteRecord();
            record.setInviterId(inviter.getId());
            record.setInviteeId(inviteeId);
            record.setInviteCode(inviteCode);
            record.setRewardIntegral(20);
            inviteRecordMapper.insert(record);

            // 邀请人获得积分
            userMapper.addIntegralAtomically(inviter.getId(), 20);

            // 被邀请人也获得额外积分
            userMapper.addIntegralAtomically(inviteeId, 10);
        }

        return Result.success();
    }
}
