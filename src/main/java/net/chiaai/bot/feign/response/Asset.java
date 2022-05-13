package net.chiaai.bot.feign.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Asset {

    private String asset;
    private Boolean marginAvailable;
    private String autoAssetExchange;

}
