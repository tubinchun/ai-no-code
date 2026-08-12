package com.lzf.langchain4j.service;

import com.lzf.langchain4j.model.dto.chathistory.ChatHistoryQueryRequest;
import com.lzf.langchain4j.model.entity.ChatHistory;
import com.lzf.langchain4j.model.entity.User;
import com.lzf.langchain4j.model.enums.ChatHistoryMessageTypeEnum;
import com.lzf.langchain4j.model.vo.ChatHistoryVO;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

import java.time.LocalDateTime;

public interface ChatHistoryService extends IService<ChatHistory> {

    /**
     * 校验对话历史
     */
    void validChatHistory(ChatHistory chatHistory, boolean add);

    /**
     * 获取查询条件
     */
    QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest);

    /**
     * 保存对话消息
     */
    boolean addChatMessage(Long appId, Long userId, String message, ChatHistoryMessageTypeEnum messageTypeEnum);

    boolean addChatMessage(Long appId, String message, String messageType, Long userId);

    /**
     * 删除某应用下的全部对话历史
     */
    boolean deleteByAppId(Long appId);

    /**
     * 获取对话历史封装类
     */
    ChatHistoryVO getChatHistoryVO(ChatHistory chatHistory);

    Page<ChatHistory> listAppChatHistoryByPage(Long appId, int pageSize,
                                               LocalDateTime lastCreateTime,
                                               User loginUser);

    int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount);
}
