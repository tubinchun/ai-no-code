package com.lzf.langchain4j.service;

import com.lzf.langchain4j.model.dto.app.AppAddRequest;
import com.lzf.langchain4j.model.dto.app.AppQueryRequest;
import com.lzf.langchain4j.model.entity.User;
import com.mybatisflex.core.service.IService;
import com.lzf.langchain4j.model.entity.App;
import com.lzf.langchain4j.model.vo.AppVO;
import com.mybatisflex.core.query.QueryWrapper;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 应用 服务层。
 *
 * @author lizhifu
 */
public interface AppService extends IService<App> {

    void validApp(App app, boolean add);

    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);

    AppVO getAppVO(App app);

    List<AppVO> getAppVOList(List<App> appList);

    void checkAppOwner(App app, Long loginUserId);

    Flux<String> chatToGenCode(Long appId, String message, User loginUser);

    String deployApp(Long appId, User loginUser);

    void generateAppScreenshotAsync(Long appId, String appUrl);

    Long createApp(AppAddRequest appAddRequest, User loginUser);

}
