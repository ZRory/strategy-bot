package net.chiaai.bot.entity.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;


@Data
public class UserLoginRequest {

    //@ApiModelProperty(value = "手机号码")
    @NotBlank(message = "手机号不能为空")
    //@Length(min = 11, max = 11, message = "手机号格式不正确")
    private String phone;

    //@ApiModelProperty(value = "登录密码")
    @NotBlank(message = "登录密码不能为空")
    private String password;

    //@ApiModelProperty(value = "短信验证码")
    @NotBlank(message = "短信验证码不能为空", groups = {RegistGroup.class, FindBackPassword.class})
    private String authCode;

    //@ApiModelProperty(value = "图形验证码uuid")
    @NotBlank(message = "uuid不能为空", groups = {LoginGroup.class})
    private String uuid;

    //@ApiModelProperty(value = "图形验证码")
    @NotBlank(message = "图形验证码不能为空", groups = {LoginGroup.class})
    private String graphCode;

    public interface LoginGroup {
    }

    public interface RegistGroup {
    }

    public interface FindBackPassword {
    }

}
