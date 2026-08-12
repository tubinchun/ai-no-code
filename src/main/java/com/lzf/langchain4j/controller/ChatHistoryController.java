package com.lzf.langchain4j.controller;

import com.lzf.langchain4j.annotation.AuthCheck;
import com.lzf.langchain4j.common.BaseResponse;
import com.lzf.langchain4j.common.ResultUtils;
import com.lzf.langchain4j.constant.UserConstant;
import com.lzf.langchain4j.exception.BusinessException;
import com.lzf.langchain4j.exception.ErrorCode;
import com.lzf.langchain4j.exception.ThrowUtils;
import com.lzf.langchain4j.model.dto.chathistory.ChatHistoryQueryRequest;
import com.lzf.langchain4j.model.entity.App;
import com.lzf.langchain4j.model.entity.ChatHistory;
import com.lzf.langchain4j.model.entity.User;
import com.lzf.langchain4j.model.vo.ChatHistoryVO;
import com.lzf.langchain4j.service.AppService;
import com.lzf.langchain4j.service.ChatHistoryService;
import com.lzf.langchain4j.service.UserService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/chatHistory")
public class ChatHistoryController {

    private static final int APP_CHAT_HISTORY_PAGE_SIZE = 10;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private AppService appService;

    @Resource
    private UserService userService;

    /**
     * 分页获取某个应用的对话历史，仅应用创建者和管理员可见。
     */
    @PostMapping("/app/list/page/vo")
    public BaseResponse<Page<ChatHistoryVO>> listAppChatHistoryVOByPage(@RequestBody ChatHistoryQueryRequest chatHistoryQueryRequest,
                                                                        HttpServletRequest request) {
        ThrowUtils.throwIf(chatHistoryQueryRequest == null, ErrorCode.PARAMS_ERROR);
        Long appId = chatHistoryQueryRequest.getAppId();
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        User loginUser = userService.getLoginUser(request);
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        if (!app.getUserId().equals(loginUser.getId()) && !UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        chatHistoryQueryRequest.setPageNum(1);
        chatHistoryQueryRequest.setPageSize(APP_CHAT_HISTORY_PAGE_SIZE);
        chatHistoryQueryRequest.setSortField("createTime");
        chatHistoryQueryRequest.setSortOrder("descend");
        QueryWrapper queryWrapper = chatHistoryService.getQueryWrapper(chatHistoryQueryRequest);
        Page<ChatHistory> chatHistoryPage = chatHistoryService.page(Page.of(1, APP_CHAT_HISTORY_PAGE_SIZE), queryWrapper);
        return ResultUtils.success(buildChatHistoryVOPage(chatHistoryPage, 1, APP_CHAT_HISTORY_PAGE_SIZE));
    }

    private Page<ChatHistoryVO> buildChatHistoryVOPage(Page<ChatHistory> chatHistoryPage, long pageNum, long pageSize) {
        Page<ChatHistoryVO> chatHistoryVOPage = new Page<>(pageNum, pageSize, chatHistoryPage.getTotalRow());
        List<ChatHistoryVO> chatHistoryVOList = chatHistoryPage.getRecords().stream()
                .map(chatHistoryService::getChatHistoryVO)
                .toList();
        chatHistoryVOPage.setRecords(chatHistoryVOList);
        return chatHistoryVOPage;
    }


    /**
     * 分页查询某个应用的对话历史（游标查询）
     *
     * @param appId          应用ID
     * @param pageSize       页面大小
     * @param lastCreateTime 最后一条记录的创建时间
     * @param request        请求
     * @return 对话历史分页
     */
    @GetMapping("/app/{appId}")
    public BaseResponse<Page<ChatHistory>> listAppChatHistory(@PathVariable Long appId,
                                                              @RequestParam(defaultValue = "10") int pageSize,
                                                              @RequestParam(required = false) LocalDateTime lastCreateTime,
                                                              HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Page<ChatHistory> result = chatHistoryService.listAppChatHistoryByPage(appId, pageSize, lastCreateTime, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 管理员分页查询所有对话历史
     *
     * @param chatHistoryQueryRequest 查询请求
     * @return 对话历史分页
     */
    @PostMapping("/admin/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<ChatHistory>> listAllChatHistoryByPageForAdmin(@RequestBody ChatHistoryQueryRequest chatHistoryQueryRequest) {
        ThrowUtils.throwIf(chatHistoryQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageNum = chatHistoryQueryRequest.getPageNum();
        long pageSize = chatHistoryQueryRequest.getPageSize();
        // 查询数据
        QueryWrapper queryWrapper = chatHistoryService.getQueryWrapper(chatHistoryQueryRequest);
        Page<ChatHistory> result = chatHistoryService.page(Page.of(pageNum, pageSize), queryWrapper);
        return ResultUtils.success(result);
    }


}
