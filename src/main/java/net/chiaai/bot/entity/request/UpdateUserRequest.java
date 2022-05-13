package net.chiaai.bot.entity.request;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
public class UpdateUserRequest {

    @TableField(value = "nick_name")
    private String nickName;

    @TableField(value = "third_user_id")
    private Long thirdUserId;

    @TableField(value = "email")
    private String email;

    @TableField(value = "avatar")
    private String avatar;

    @TableField(value = "access_token")
    private String accessToken;

}
