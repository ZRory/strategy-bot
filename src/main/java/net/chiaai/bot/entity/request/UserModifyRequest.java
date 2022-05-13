package net.chiaai.bot.entity.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;


@Data
public class UserModifyRequest {

    //@ApiModelProperty(value = "手机号码")
    //@Length(min = 11, max = 11, message = "手机号格式不正确")
    //    @NotBlank(message = "手机号码不能为空")
    //    private String phone;

    //@ApiModelProperty(value = "旧登录密码")
    @NotBlank(message = "登录密码不能为空")
    private String password;

    //@ApiModelProperty(value = "新登录密码")
    @NotBlank(message = "新密码不能为空")
    private String oldPassword;

}
