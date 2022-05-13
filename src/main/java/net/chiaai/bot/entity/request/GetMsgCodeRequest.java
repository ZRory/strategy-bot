package net.chiaai.bot.entity.request;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
public class GetMsgCodeRequest {

    //@ApiModelProperty(value = "手机号码")
    @NotBlank(message = "手机号不能为空")
    //@Length(min = 11, max = 11, message = "手机号格式不正确")
    private String phone;

    //@ApiModelProperty(value = "图形验证码uuid")
    @NotBlank(message = "uuid不能为空")
    private String uuid;

    //@ApiModelProperty(value = "图形验证码")
    @NotBlank(message = "图形验证码不能为空")
    private String graphCode;

}
