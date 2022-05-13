package net.chiaai.bot.common.utils;

import com.wf.captcha.*;
import com.wf.captcha.base.Captcha;
import lombok.Data;
import net.chiaai.bot.common.enums.BizCodeEnum;
import net.chiaai.bot.common.enums.VerifyCodeEnum;
import net.chiaai.bot.common.exception.BizException;
import net.chiaai.bot.conf.VerifyCodeConfig;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.util.Objects;

@Data
@Component
@ConfigurationProperties(prefix = "login")
public class VerifyCodeUtils {

    /**
     * 账号单用户 登录
     */
    private boolean singleLogin = false;

    private VerifyCodeConfig config;

    /**
     * 用户登录信息缓存
     */
    private boolean cacheEnable;

    public boolean isSingleLogin() {
        return singleLogin;
    }

    public boolean isCacheEnable() {
        return cacheEnable;
    }

    /**
     * 获取验证码生产类
     *
     * @return /
     */
    public Captcha getCaptcha() {
        if (Objects.isNull(config)) {
            config = new VerifyCodeConfig();
            if (Objects.isNull(config.getCodeType())) {
                config.setCodeType(VerifyCodeEnum.arithmetic);
            }
        }
        return switchCaptcha(config);
    }

    /**
     * 依据配置信息生产验证码
     *
     * @param config 验证码配置信息
     * @return /
     */
    private Captcha switchCaptcha(VerifyCodeConfig config) {
        Captcha captcha;
        synchronized (this) {
            switch (config.getCodeType()) {
                case arithmetic:
                    // 算术类型 https://gitee.com/whvse/EasyCaptcha
                    captcha = new FixedArithmeticCaptcha(config.getWidth(), config.getHeight());
                    // 几位数运算，默认是两位
                    captcha.setLen(config.getLength());
                    break;
                case chinese:
                    captcha = new ChineseCaptcha(config.getWidth(), config.getHeight());
                    captcha.setLen(config.getLength());
                    break;
                case chinese_gif:
                    captcha = new ChineseGifCaptcha(config.getWidth(), config.getHeight());
                    captcha.setLen(config.getLength());
                    break;
                case gif:
                    captcha = new GifCaptcha(config.getWidth(), config.getHeight());
                    captcha.setLen(config.getLength());
                    break;
                case spec:
                    captcha = new SpecCaptcha(config.getWidth(), config.getHeight());
                    captcha.setLen(config.getLength());
                    break;
                default:
                    throw new BizException(BizCodeEnum.ILLEGAL_OPERATION, "验证码配置信息错误！正确配置查看 configEnum ");
            }
        }
        if (StringUtils.isNotBlank(config.getFontName())) {
            captcha.setFont(new Font(config.getFontName(), Font.PLAIN, config.getFontSize()));
        }
        return captcha;
    }

    static class FixedArithmeticCaptcha extends ArithmeticCaptcha {
        public FixedArithmeticCaptcha(int width, int height) {
            super(width, height);
        }

        @Override
        protected char[] alphas() {
            // 生成随机数字和运算符
            int n1 = num(1, 10), n2 = num(1, 10);
            int opt = num(3);

            // 计算结果
            int res = new int[]{n1 + n2, n1 - n2, n1 * n2}[opt];
            // 转换为字符运算符
            char optChar = "+-x".charAt(opt);

            this.setArithmeticString(String.format("%s%c%s=?", n1, optChar, n2));
            this.chars = String.valueOf(res);

            return chars.toCharArray();
        }
    }
}
