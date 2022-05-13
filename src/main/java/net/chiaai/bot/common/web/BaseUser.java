package net.chiaai.bot.common.web;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class BaseUser implements Serializable {

    /**
     * ID
     */
    private Long id;

    /**
     * 手机号码
     */
    private String phone;

    private Long thirdUserId;

    private Boolean inviteUser;

    /**
     * 昵称
     */
    private String nickName;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 头像地址
     */
    private String avatar;

    /**
     * apikey
     */
    private String apiKey;

}
