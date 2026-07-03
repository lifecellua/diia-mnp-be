package com.lifecell.diia.be.model.dto.lifecell.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString(callSuper = true)
public class CheckIccidResponse extends Response {
    private Map<String, BalanceInfo> balances;
    private List<Product> products;
    private Boolean isFirstMonth;

    @Getter
    @Setter
    @ToString
    public static class BalanceInfo {
        @JsonProperty("NAME_UK")
        private String nameUk;
        @JsonProperty("NAME_RU")
        private String nameRu;
        @JsonProperty("MEASURE")
        private String measure;
    }

    @Getter
    @Setter
    @ToString
    public static class Product {
        private String id;
        @JsonProperty("PERIOD")
        private int period;
        @JsonProperty("TARIFF_PRICE")
        private int tariffPrice;
        @JsonProperty("IUI_RM")
        private String iuiRm;
        @JsonProperty("IUI_UG")
        private String iui_Ug;
        @JsonProperty("PARTNER_UK")
        private String partnerUk;
        @JsonProperty("PARTNER_EN")
        private String partnerEn;
        @JsonProperty("PARTNER_RU")
        private String partnerRu;
        @JsonProperty("RETRYABLE")
        private String retryable;
        @JsonProperty("INFOLIFE_TARIFF_IUI")
        private String infolifeTariffIui;
        @JsonProperty("SSI_LINK_UK")
        private String ssiLinkUk;
        @JsonProperty("SSI_LINK_RU")
        private String ssiLinkRu;
        @JsonProperty("DESCRIPTION_UK")
        private String descriptionUk;
        @JsonProperty("DESCRIPTION_RU")
        private String descriptionRu;
        @JsonProperty("FEE_28D_MNP")
        private int fee28dMNP;
        @JsonProperty("FEE_28D")
        private int fee28d;
        @JsonProperty("FEE_01D")
        private int fee01d;
        @JsonProperty("FEE_28D_PERSONALIZED")
        private int fee28dPersonalized;
        @JsonProperty("BUNDLE_1D")
        private List<Bundle> bundle1d;
        @JsonProperty("BUNDLE_28D")
        private List<Bundle> bundle28d;
    }

    @Getter
    @Setter
    @ToString
    public static class Bundle {
        private String id;
        @JsonProperty("AMOUNT")
        private long amount;
        @JsonProperty("UNLIM")
        private Boolean unLim;
    }
}
