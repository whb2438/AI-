package com.divination.controller;
import com.divination.common.result.Result;
import com.divination.service.CardService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;
@RestController
@RequestMapping("/api/card")
public class CardController {
    private final CardService cardService;
    public CardController(CardService cardService) {
        this.cardService = cardService;
    }
    @PostMapping("/exchange")
    public Result<Map<String, Object>> exchange(@Valid @RequestBody ExchangeRequest request,
                                    HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        int newIntegral = cardService.exchangeCardKey(request.getCardKey(), userId);
        Map<String, Object> data = new HashMap<>();
        data.put("integral", newIntegral);
        return Result.success("兑换成功，积分已到账", data);
    }
    public static class ExchangeRequest {
        @NotBlank(message = "卡号不能为空")
        @Size(max = 100, message = "卡号长度不能超过100")
        private String cardKey;
        public String getCardKey() { return cardKey; }
        public void setCardKey(String cardKey) { this.cardKey = cardKey; }
    }
}
