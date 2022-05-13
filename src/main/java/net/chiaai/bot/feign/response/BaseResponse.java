package net.chiaai.bot.feign.response;

import lombok.Data;

@Data
public class BaseResponse {

    private Integer code = 200;

    private String msg = "success";

}
