package com.eldercare.order.controller;

import com.eldercare.api.vo.CursorPageVO;
import com.eldercare.api.vo.OrderEvaluationVO;
import com.eldercare.api.vo.VolunteerScoreVO;
import com.eldercare.common.response.Result;
import com.eldercare.order.service.OrderAppService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * 志愿者评价公开查询接口。
 * 说明：评价数据归属订单域，但小程序 URL 使用 /volunteers/{id}/reviews 更符合页面语义。
 */
@RestController
@RequestMapping("/volunteers")
@Tag(name = "志愿者评价接口", description = "查看志愿者评价和评分")
public class VolunteerReviewController {
    /** 订单业务服务。 */
    private final OrderAppService orderAppService;

    public VolunteerReviewController(OrderAppService orderAppService) {
        // 保存订单业务服务。
        this.orderAppService = orderAppService;
    }

    /** 查询志愿者评价。 */
    @GetMapping("/{volunteerId}/reviews")
    @Operation(summary = "志愿者评价分页", description = "按游标分页查看志愿者评价列表")
    public Result<CursorPageVO<OrderEvaluationVO>> reviews(@PathVariable Long volunteerId,
                                                           @RequestParam(required = false) Long lastId,
                                                           @RequestParam(required = false) Integer size) {
        return Result.success(orderAppService.volunteerReviews(volunteerId, lastId, size));
    }

    /** 查询志愿者评分。 */
    @GetMapping("/{volunteerId}/score")
    @Operation(summary = "志愿者评分", description = "查看志愿者平均评分、服务次数和评价数量")
    public Result<VolunteerScoreVO> score(@PathVariable Long volunteerId) {
        return Result.success(orderAppService.volunteerScore(volunteerId));
    }
}
