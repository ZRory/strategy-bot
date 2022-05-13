package net.chiaai.bot.entity.dao;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName(value = "user")
public class User {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField(value = "phone")
    private String phone;

    @TableField(value = "nick_name")
    private String nickName;

    @TableField(value = "third_user_id")
    private Long thirdUserId;

    @TableField(value = "invite_user")
    private Boolean inviteUser;

    @TableField(value = "email")
    private String email;

    @TableField(value = "avatar")
    private String avatar;

    @TableField(value = "password")
    private String password;

    @TableField(value = "api_key")
    private String apiKey;

    @TableField(value = "api_secret")
    private String apiSecret;

    @TableField(value = "access_token")
    private String accessToken;

    @TableField(value = "disabled")
    private Boolean disabled;

    @TableField(value = "service_amount_rate")
    private BigDecimal serviceAmountRate;

    @TableField(value = "create_time")
    private LocalDateTime createTime;

    @TableField(value = "update_time")
    private LocalDateTime updateTime;

}
