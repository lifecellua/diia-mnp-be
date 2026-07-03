package com.lifecell.diia.be.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifecell.diia.be.model.dto.lifecell.response.TariffInfo;
import com.lifecell.diia.be.service.TemplateService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TemplateFormsTest {

    public List<TariffInfo> getTariffs() {
        var t1 = new TariffInfo();
        t1.setId("tariff_id_1");
        t1.setName("Турбота");
        t1.setPrice("100 грн./30 дн.");
        t1.setDescription("Тариф доступний лише за наявності пенсійного посвідчення");
        t1.setBenefits(List.of("200_смс", "100_дзвінків", "100_гб"));
        t1.setIcon("https://www.lifecell.ua/cms/uploads/medialibrary/2025/11/turbota.png");
        t1.setUrl("s.lifecell.ua/sendia");
        t1.setHasPc(true);

        var t2 =new TariffInfo();
        t2.setId("tariff_id_2");
        t2.setName("Максі");
        t2.setPrice("190грн/ 4 тижні");
        t2.setDescription("Безліміт на застосунки (YouTube, Instagram, TikTok, Telegram, WhatsApp, Facebook, Messenger, X та інші)");
        t2.setBenefits(List.of("2100_смс", "1100_дзвінків", "1001_гб"));
        t2.setIcon("https://www.lifecell.ua/cms/uploads/medialibrary/2025/11/maxi.png");
        t2.setUrl("s.lifecell.ua/msdia");
        t2.setHasPc(false);

        return List.of(t1, t2);
    }

    @Test
    public void testTariffs() {
        assertDoesNotThrow(() -> {
            var tariffs = getTariffs();
            var m = new ObjectMapper();
            var ts = new TemplateService();
            var result = ts.generate("tariffs",
                    Map.of(
                            "TARIFFS", tariffs.stream()
                                    .map((tariff) -> Map.of(
                                            "INFO", Map.of(
                                                    "STATUS", tariff.getHasPc()
                                                            ? TemplateService.Operation.KEEP
                                                            : TemplateService.Operation.REMOVE,
                                                    "ID", tariff.getId(),
                                                    "NAME", TemplateService.mapOf(
                                                            "NAME", tariff.getName(),
                                                            "PRICE", tariff.getPrice()),
                                                    "DESCRIPTION", List.of(tariff.getDescription()),
                                                    "BENEFITS", tariff.getBenefits().stream()
                                                            .map((benefit) -> Map.of(
                                                                    "CODE", benefit,
                                                                    "NAME", benefit)
                                                            )
                                                            .toList(),
                                                    "ICON", tariff.getIcon()
                                            ),
                                                    "REF", TemplateService.mapOf(
                                                            "DESCRIPTION", TemplateService.mapOf(
                                                                    "NAME", tariff.getName()),
                                                            "LABEL", TemplateService.mapOf(
                                                                    "NAME", tariff.getName()),
                                                            "URL", tariff.getUrl()
                                            )
                                    ))
                                    .toList()
                    )
            );
            log.info("Tariffs: {}", result);
        });
    }

    @Test
    public void testAgreement1() {
        assertDoesNotThrow(() -> {
            var m = new ObjectMapper();
            var ts = new TemplateService();
            var result = ts.generate("agreement",
                    Map.of(
                            "PC", Map.of("DOCUMENT_TYPE_AND_ID", "1111"),
                            "PORTING_DATE", Map.of("DATE", "2020-01-01"),
                            "CANCELING_DATE", Map.of("DATE", "2020-02-02")
                    ));
            log.info("Agreement1: {}", result);
        });
    }

    @Test
    public void testAgreement2() {
        assertDoesNotThrow(() -> {
            var m = new ObjectMapper();
            var ts = new TemplateService();
            var result = ts.generate("agreement",
                    Map.of(
                            "PC", TemplateService.Operation.REMOVE,
                            "PORTING_DATE", Map.of("DATE", "2020-01-01"),
                            "CANCELING_DATE", Map.of("DATE", "2020-02-02")
                    ));
            log.info("Agreement1: {}", result);
        });
    }

    @Test
    public void testAgreement3() {
        assertDoesNotThrow(() -> {
            var m = new ObjectMapper();
            var ts = new TemplateService();
            var result = ts.generate("agreement",
                    Map.of(
                            "PC", TemplateService.Operation.REMOVE,
                            "PORTING_DATE", TemplateService.Operation.REMOVE,
                            "CANCELING_DATE", Map.of("DATE", "2020-01-01")
                    ));
            log.info("Agreement1: {}", result);
        });
    }

    @Test
    public void testAgreement4() {
        assertDoesNotThrow(() -> {
            var m = new ObjectMapper();
            var ts = new TemplateService();
            var result = ts.generate("agreement",
                    Map.of(
                            "PC", TemplateService.Operation.REMOVE,
                            "PORTING_DATE", TemplateService.Operation.REMOVE,
                            "CANCELING_DATE", TemplateService.Operation.REMOVE
                    ));
            log.info("Agreement1: {}", result);
        });
    }
}
