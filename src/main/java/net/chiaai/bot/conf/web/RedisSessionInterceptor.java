package net.chiaai.bot.conf.web;

import lombok.extern.slf4j.Slf4j;
import net.chiaai.bot.common.enums.BizCodeEnum;
import net.chiaai.bot.common.web.BizResponse;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@Slf4j
public class RedisSessionInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        //无论访问的地址是不是正确的，都进行登录验证，登录成功后的访问再进行分发，404的访问自然会进入到错误控制器中
        HttpSession session = request.getSession();
        Object user = session.getAttribute("loginUser");
        if (user != null) {
            //单点登录逻辑
//            try {
//                BaseUser loginUser = (BaseUser) user;
//                //验证当前请求的session是否是已登录的session
//                String loginSessionId = redisUtils.getString("clientUser:" + loginUser.getUserId());
//                if (loginSessionId != null && loginSessionId.equals(session.getId())) {
//                    return true;
//                }
//            } catch (Exception e) {
//                log.error("Session前置处理异常", e);
//                e.printStackTrace();
//            }
            return true;
        }
        authFailed(response);
        return false;
    }

    private void authFailed(HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json; charset=utf-8");

        try {
            response.getWriter().print(new BizResponse(BizCodeEnum.USER_NOT_LOGIN));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {

    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {

    }
}