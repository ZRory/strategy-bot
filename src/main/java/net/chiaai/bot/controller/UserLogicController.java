package net.chiaai.bot.controller;

import lombok.RequiredArgsConstructor;
import net.chiaai.bot.common.utils.SessionUtils;
import net.chiaai.bot.common.web.BaseUser;
import net.chiaai.bot.common.web.BizResponse;
import net.chiaai.bot.entity.dao.User;
import net.chiaai.bot.entity.request.*;
import net.chiaai.bot.entity.response.GraphCodeResponse;
import net.chiaai.bot.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequiredArgsConstructor
//@Api(tags = "用户逻辑处理")
@RequestMapping("/user")
public class UserLogicController {

    @Resource
    private UserService userService;

    //@ApiOperation("注册")
    @PostMapping("/register")
    public BizResponse<?> register(@Validated({UserLoginRequest.RegistGroup.class}) @RequestBody UserLoginRequest request) {
        userService.register(request);
        return BizResponse.success();
    }

    //@ApiOperation("登录")
    @PostMapping("/login")
    public BizResponse login(@Validated({UserLoginRequest.LoginGroup.class}) @RequestBody UserLoginRequest request) {
        return BizResponse.success(userService.login(request));
    }

    //@ApiOperation("获取登录用户信息")
    @PostMapping("/userInfo")
    public BizResponse<BaseUser> userInfo() {
        return BizResponse.success(userService.flushUserInfo());
    }

    //@ApiOperation("登出")
    @PostMapping("/logout")
    public BizResponse logout() {
        SessionUtils.removeSessionAttribute("loginUser");
        return BizResponse.success();
    }

    //@ApiOperation("修改密码")
    @PostMapping("/modifyPassword")
    public BizResponse modifyPassword(@Validated @RequestBody UserModifyRequest request) {
        if (request == null || StringUtils.isAnyBlank(request.getOldPassword(), request.getPassword())) {
            return BizResponse.paramIsNull("必传参数为空！");
        }
        userService.modifyPassword(request);
        return BizResponse.success();
    }

    //@ApiOperation("找回密码")
    @PostMapping("/findPassword")
    public BizResponse findBackPassword(@Validated({UserLoginRequest.FindBackPassword.class}) @RequestBody UserLoginRequest request) {
        if (request == null || StringUtils.isAnyBlank(request.getPhone(), request.getAuthCode(), request.getPassword())) {
            return BizResponse.paramIsNull("必传参数为空！");
        }
        userService.findBackPassword(request);
        return BizResponse.success();
    }

    //@ApiOperation("生成图形验证码")
    @PostMapping("/getGraphCode")
    public BizResponse<GraphCodeResponse> getGraphCode() {
        BizResponse<GraphCodeResponse> response = BizResponse.success();
        response.setData(userService.generateGraphCode());
        return response;
    }

    //@ApiOperation("发送短信验证码")
    @PostMapping("/getSmsCode")
    public BizResponse<?> getSmsCode(@Validated @RequestBody GetMsgCodeRequest request) {
        userService.getSmsCode(request);
        return BizResponse.success();
    }

    //@ApiOperation("更新/插入API信息")
    @PostMapping("/updateAPI")
    public BizResponse<?> updateKey(@Validated @RequestBody UserKeyRequest request) throws Exception {
        userService.updateAPI(request);
        return BizResponse.success();
    }

    @PostMapping("/edit")
    public BizResponse<?> updateKey(@Validated @RequestBody UpdateUserRequest user) {
        userService.updateUser(user);
        return BizResponse.success();
    }

}
