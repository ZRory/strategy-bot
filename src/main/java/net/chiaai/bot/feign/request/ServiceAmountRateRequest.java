package net.chiaai.bot.feign.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceAmountRateRequest extends BaseRequest {

    private String symbol = "BTCUSDT";

}
