package net.chiaai.bot.entity.request;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
public class UserKeyRequest {

    //@ApiModelProperty(value = "API-KEY")
    @NotBlank(message = "API-KEY不能为空")
    private String apiKey;

    //@ApiModelProperty(value = "API-SECRET")
    @NotBlank(message = "API-SECRET不能为空")
    private String apiSecret;

    //@ApiModelProperty(value = "登录密码")
    @NotBlank(message = "登录密码不能为空")
    private String password;

}
