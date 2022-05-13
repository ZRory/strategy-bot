package net.chiaai.bot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wf.captcha.base.Captcha;
import lombok.RequiredArgsConstructor;
import net.chiaai.bot.common.enums.BizCodeEnum;
import net.chiaai.bot.common.enums.VerifyCodeEnum;
import net.chiaai.bot.common.exception.BizException;
import net.chiaai.bot.common.utils.*;
import net.chiaai.bot.common.web.BaseUser;
import net.chiaai.bot.conf.GlobalConfig;
import net.chiaai.bot.entity.dao.User;
import net.chiaai.bot.entity.request.*;
import net.chiaai.bot.entity.response.GraphCodeResponse;
import net.chiaai.bot.feign.BinanceFeignClient;
import net.chiaai.bot.feign.request.BaseRequest;
import net.chiaai.bot.mapper.UserMapper;
import net.chiaai.bot.service.DingTalkService;
import net.chiaai.bot.service.UserService;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.crypto.Cipher;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    @Resource
    private RedisUtils redisUtils;
    @Resource
    private UserMapper userMapper;
    @Resource
    private VerifyCodeUtils verifyCodeUtils;

    @Resource
    private BinanceFeignClient binanceClient;

    @Resource
    private DingTalkService dingTalkService;

    @Override
    public void register(UserLoginRequest request) {
        //检验短信验证码
        this.checkPhoneKV(request.getPhone(), request.getAuthCode());
        LambdaQueryWrapper<User> userQueryWrapper = new LambdaQueryWrapper<>();
        userQueryWrapper.eq(User::getPhone, request.getPhone());
        User user = userMapper.selectOne(userQueryWrapper);
        if (!ObjectUtils.isEmpty(user)) {
            throw new BizException(BizCodeEnum.SUCCESS_EXIST, "该手机号已经注册");
        }
        user = new User();
        user.setInviteUser(false);
        user.setNickName(request.getPhone());
        user.setPhone(request.getPhone());
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        user.setDisabled(false);
        user.setPassword(DigestUtils.md5Hex(request.getPassword()));
        if (userMapper.insert(user) != 1) {
            throw new BizException(BizCodeEnum.OPERATION_FAILED, "注册失败！");
        }
    }

    @Override
    public void modifyPassword(UserModifyRequest request) {
        User user = getUserByPhone(SessionUtils.getLoginUser().getPhone());

        String newPassword = DigestUtils.md5Hex(request.getPassword());
        String oldPassword = DigestUtils.md5Hex(request.getOldPassword());

        if (!user.getPassword().equals(oldPassword)) {
            throw new BizException(BizCodeEnum.OPERATION_FAILED, "旧密码错误！");
        }
        if (user.getPassword().equals(newPassword)) {
            throw new BizException(BizCodeEnum.OPERATION_FAILED, "新密码不能和原来密码相同！");
        }
        modifyPassword(user.getId(), newPassword, "修改密码失败！");
        dingTalkService.sendMessage(user,
                "操作：修改密码" +
                        "\r\n操作时间：" + DateUtils.format(LocalDateTime.now()));
    }

    private void modifyPassword(Long userId, String password, String message) {
        User entity = new User();
        entity.setId(userId);
        entity.setPassword(password);
        entity.setUpdateTime(LocalDateTime.now());
        if (userMapper.updateById(entity) != 1) {
            throw new BizException(BizCodeEnum.OPERATION_FAILED, message);
        }
    }

    @Override
    public User getUserByPhone(String phone) {
        LambdaQueryWrapper<User> userQueryWrapper = new LambdaQueryWrapper<>();
        userQueryWrapper.eq(User::getPhone, phone);
        User user = userMapper.selectOne(userQueryWrapper);
        if (ObjectUtils.isEmpty(user)) {
            throw new BizException(BizCodeEnum.SUCCESS_NOT_EXIST, "用户不存在");
        }
        return user;
    }

    @Override
    public void updateUser(UpdateUserRequest user) {
        User oldUser = getUserByPhone(SessionUtils.getLoginUser().getPhone());
        if (oldUser.getInviteUser() && oldUser.getThirdUserId() != null && !oldUser.getThirdUserId().equals(user.getThirdUserId())) {
            throw new BizException(BizCodeEnum.ILLEGAL_OPERATION, "已验证用户禁止修改交易所ID");
        }
        BeanUtils.copyProperties(user, oldUser);
        oldUser.setUpdateTime(LocalDateTime.now());
        if (StringUtils.isNotBlank(user.getAccessToken())) {
            oldUser.setAccessToken(oldUser.getAccessToken().replaceAll("https://oapi.dingtalk.com/robot/send\\?access_token=", ""));
            //发送验证消息
            dingTalkService.sendMessage(oldUser,
                    "操作：配置钉钉机器人" +
                            "\r\n操作结果：配置成功" +
                            "\r\n操作时间：" + DateUtils.format(LocalDateTime.now()));
        }
        userMapper.updateById(oldUser);
    }

    @Override
    public BaseUser flushUserInfo() {
        User user = getUserByPhone(SessionUtils.getLoginUser().getPhone());
        BaseUser baseUser = new BaseUser();
        BeanUtils.copyProperties(user, baseUser);
        SessionUtils.setLoginUser(baseUser);
        return baseUser;
    }

    @Override
    public void findBackPassword(UserLoginRequest request) {
        User user = getUserByPhone(request.getPhone());
        this.checkPhoneKV(request.getPhone(), request.getAuthCode());
        String newPassword = DigestUtils.md5Hex(request.getPassword());
        modifyPassword(user.getId(), newPassword, "找回密码失败！");
    }

    @Override
    public BaseUser login(UserLoginRequest request) {
        this.checkGraphCode(request.getUuid(), request.getGraphCode());
        User user = getUserByPhone(request.getPhone());
        String decryptPassword = DigestUtils.md5Hex(request.getPassword());
        if (!decryptPassword.equals(user.getPassword())) {
            throw new BizException(BizCodeEnum.USERNAME_OR_PASSWORD_ERROR, "密码错误");
        }
        if (user.getDisabled()) {
            throw new BizException(BizCodeEnum.AUTH_FAILED, "账户已禁用");
        }
        BaseUser baseUser = new BaseUser();
        BeanUtils.copyProperties(user, baseUser);
        SessionUtils.setLoginUser(baseUser);
        dingTalkService.sendMessage(user,
                "操作：账户登录" +
                        "\r\n操作时间：" + DateUtils.format(LocalDateTime.now()));
        return baseUser;
    }


    @Override
    public GraphCodeResponse generateGraphCode() {
        Captcha captcha = verifyCodeUtils.getCaptcha();
        String uuid = UUID.randomUUID().toString();
        // 当验证码类型为 arithmetic时且长度 >= 2 时，captcha.text()的结果有几率为浮点型
        String captchaValue = captcha.text();
        if (captcha.getCharType() - 1 == VerifyCodeEnum.arithmetic.ordinal() && captchaValue.contains(".")) {
            captchaValue = captchaValue.split("\\.")[0];
        }
        redisUtils.set(uuid, captchaValue, verifyCodeUtils.getConfig().getExpiration(), TimeUnit.MINUTES);
        GraphCodeResponse result = new GraphCodeResponse();
        result.setUuid(uuid);
        result.setGraphCode(captcha.toBase64());
        return result;
    }

    @Override
    public void getSmsCode(GetMsgCodeRequest request) {
        if (redisUtils.get(request.getPhone()) != null) {
            throw new BizException(BizCodeEnum.TOO_MANY_REQUEST, "发送过于频繁,请稍后重试");
        }
        checkGraphCode(request.getUuid(), request.getGraphCode());
        String smsCode = RandomStringUtils.randomNumeric(6);
        SmsService.sendAuthCode(request.getPhone(), smsCode);
        redisUtils.set(request.getPhone(), smsCode, 10, TimeUnit.MINUTES);
    }

    @Override
    public void updateAPI(UserKeyRequest request) throws Exception {
        User user = getUserByPhone(SessionUtils.getLoginUser().getPhone());
        String paramPwd = DigestUtils.md5Hex(request.getPassword());
        if (!user.getPassword().equals(paramPwd)) {
            throw new BizException(BizCodeEnum.AUTH_FAILED, "密码错误！");
        }
        //更新api信息
        user.setUpdateTime(LocalDateTime.now());
        user.setApiKey(request.getApiKey());
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding"); // 确定算法
        cipher.init(Cipher.ENCRYPT_MODE, GlobalConfig.key);    // 确定密钥
        byte[] result = cipher.doFinal(request.getApiSecret().getBytes());  // 加密
        user.setApiSecret(Base64.encodeBase64String(result));
        user.setInviteUser(false);
        binanceClient.balance(user.getApiKey(), BinanceUtil.encodeParams(new BaseRequest(), user));
        userMapper.updateById(user);
        dingTalkService.sendMessage(user,
                "操作：更新API信息" +
                        "\r\n操作时间：" + DateUtils.format(LocalDateTime.now()));
    }

    private void checkGraphCode(String uuid, String graphCodeValue) {
        String code = redisUtils.getString(uuid);
        if (StringUtils.isBlank(code) || !code.equals(graphCodeValue)) {
            throw new BizException(BizCodeEnum.PARAM_ERROR, "图形验证码错误");
        }
        redisUtils.del(uuid);
    }

    private void checkPhoneKV(String phone, String authCode) {
        String val = redisUtils.getString(phone);
        if (StringUtils.isBlank(val)) {
            throw new BizException(BizCodeEnum.AUTHENTICATION_EXPIRED, "短信验证码已失效");
        }
        if (!val.equals(authCode)) {
            throw new BizException(BizCodeEnum.PARAM_ERROR, "短信验证码错误");
        }
        redisUtils.del(phone);
    }
}