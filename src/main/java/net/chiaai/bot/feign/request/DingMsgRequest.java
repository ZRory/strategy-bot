package net.chiaai.bot.feign.request;

import lombok.Getter;
import lombok.Setter;
import net.chiaai.bot.entity.dao.User;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class DingMsgRequest {

    private AtRequest at;

    private DingMsgContent text;

    private String msgtype = "text";

    public DingMsgRequest(String phone, String content) {
        if (StringUtils.isNotBlank(phone)) {
            AtRequest atRequest = new AtRequest();
            List<String> atPhones = new ArrayList<>();
            atPhones.add(phone);
            atRequest.setAtMobiles(atPhones);
            this.at = atRequest;
        }
        this.text = new DingMsgContent(content);
    }

}
