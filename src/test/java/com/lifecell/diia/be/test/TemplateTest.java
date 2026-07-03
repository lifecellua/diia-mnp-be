package com.lifecell.diia.be.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.lifecell.diia.be.service.TemplateService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TemplateTest {
    private static String NAME = "form";

    @Test
    public void testFieldReplace() {
        assertDoesNotThrow(() -> {
            var m = new ObjectMapper();
            var ts = new TemplateService();
            var result = ts.generate(NAME,
                    Map.of("VALUE", "new_value")
            );
            var value = m.readTree(result).at("/value").asText();
            assertEquals("new_value", value, "Wrong value");
        });
    }

    @Test
    public void testFieldKeep() {
        assertDoesNotThrow(() -> {
            var m = new ObjectMapper();
            var ts = new TemplateService();
            var result = ts.generate(NAME,
                    Map.of("VALUE", TemplateService.Operation.KEEP)
            );
            var value = m.readTree(result).at("/value").asText();
            assertEquals("value", value, "Wrong value");
        });
    }

    @Test
    public void testFieldRemove() {
        assertDoesNotThrow(() -> {
            var m = new ObjectMapper();
            var ts = new TemplateService();
            var result = ts.generate(NAME,
                    Map.of("VALUE", TemplateService.Operation.REMOVE)
            );
            var value = m.readTree(result).at("/value") instanceof MissingNode;
            assertTrue(value, "Wrong value");
        });
    }


    @Test
    public void testValueLocal() {
        assertDoesNotThrow(() -> {
            var m = new ObjectMapper();
            var ts = new TemplateService();
            var result = ts.generate(NAME,
                    Map.of("VALUE_LOCAL", Map.of("VALUE", "value"))
            );
            var value = m.readTree(result).at("/value_local").asText();
            assertEquals("test value test", value, "Wrong value");
        });
    }

    @Test
    public void testObjectFiledReplace() {
        assertDoesNotThrow(() -> {
            var m = new ObjectMapper();
            var ts = new TemplateService();
            var result = ts.generate(NAME,
                    Map.of("OBJECT", Map.of("FIELD", "new_value"))
            );
            var value = m.readTree(result).at("/object/field").asText();
            assertEquals("new_value", value, "Wrong value");
        });
    }

    @Test
    public void testObjectFiledKeep() {
        assertDoesNotThrow(() -> {
            var m = new ObjectMapper();
            var ts = new TemplateService();
            var result = ts.generate(NAME,
                    Map.of("OBJECT", Map.of("FIELD", TemplateService.Operation.KEEP))
            );
            var value = m.readTree(result).at("/object/field").asText();
            assertEquals("value", value, "Wrong value");
        });
    }

    @Test
    public void testObjectFieldRemove() {
        assertDoesNotThrow(() -> {
            var m = new ObjectMapper();
            var ts = new TemplateService();
            var result = ts.generate(NAME,
                    Map.of("OBJECT", Map.of("FIELD", TemplateService.Operation.REMOVE))
            );
            var value = m.readTree(result).at("/object/field") instanceof MissingNode;
            assertTrue(value, "Wrong value");
        });
    }

    @Test
    public void testListReplace() {
        assertDoesNotThrow(() -> {
            var m = new ObjectMapper();
            var ts = new TemplateService();
            var result = ts.generate(NAME,
                    Map.of("VALUES010", List.of("new_value"))
            );
            var value1 = m.readTree(result).at("/values/0").asText();
            assertEquals("new_value", value1, "Wrong value");
            var value2 = m.readTree(result).at("/values/1") instanceof MissingNode;
            assertTrue(value2, "Wrong value");
            var value3 = m.readTree(result).at("/values/2") instanceof MissingNode;
            assertTrue(value3, "Wrong value");
        });
    }

    @Test
    public void testListKeep() {
        assertDoesNotThrow(() -> {
            var m = new ObjectMapper();
            var ts = new TemplateService();
            var result = ts.generate(NAME,
                    Map.of("VALUES010", TemplateService.Operation.KEEP)
            );
            var value = m.readTree(result).at("/values/0").asText()
                    + m.readTree(result).at("/values/1").asText()
                    + m.readTree(result).at("/values/2").asText();
            assertEquals("value1value2value3", value, "Wrong value");
        });
    }

    @Test
    public void testListRemove() {
        assertDoesNotThrow(() -> {
            var m = new ObjectMapper();
            var ts = new TemplateService();
            var result = ts.generate(NAME,
                    Map.of("VALUES010", TemplateService.Operation.REMOVE)
            );
            var value = m.readTree(result).at("/values") instanceof MissingNode;
            assertTrue(value, "Wrong value");
        });
    }

    @Test
    public void testList1Replace() {
        assertDoesNotThrow(() -> {
            var m = new ObjectMapper();
            var ts = new TemplateService();
            var result = ts.generate(NAME,
                    Map.of("VALUES110", List.of("new_value"))
            );
            var value1 = m.readTree(result).at("/values/0").asText();
            assertEquals("value1", value1, "Wrong value");
            var value2 = m.readTree(result).at("/values/1").asText();
            assertEquals("new_value", value2, "Wrong value");
            var value3 = m.readTree(result).at("/values/2") instanceof MissingNode;
            assertTrue(value3, "Wrong value");
        });
    }

    @Test
    public void testList1Clear() {
        assertDoesNotThrow(() -> {
            var m = new ObjectMapper();
            var ts = new TemplateService();
            var result = ts.generate(NAME,
                    Map.of("VALUES110", List.of())
            );
            var value1 = m.readTree(result).at("/values/0").asText();
            assertEquals("value1", value1, "Wrong value");
            var value2 = m.readTree(result).at("/values/2") instanceof MissingNode;
            assertTrue(value2, "Wrong value");
            var value3 = m.readTree(result).at("/values/3") instanceof MissingNode;
            assertTrue(value3, "Wrong value");

        });
    }

    @Test
    public void testList111Replace() {
        assertDoesNotThrow(() -> {
            var m = new ObjectMapper();
            var ts = new TemplateService();
            var result = ts.generate(NAME,
                    Map.of("VALUES111", List.of("new_value"))
            );
            var value1 = m.readTree(result).at("/values/0").asText();
            assertEquals("value1", value1, "Wrong value");
            var value2 = m.readTree(result).at("/values/1").asText();
            assertEquals("new_value", value2, "Wrong value");
            var value3 = m.readTree(result).at("/values/2").asText();
            assertEquals("value3", value3, "Wrong value");
        });
    }

    @Test
    public void testList11Clear() {
        assertDoesNotThrow(() -> {
            var m = new ObjectMapper();
            var ts = new TemplateService();
            var result = ts.generate(NAME,
                    Map.of("VALUES111", List.of())
            );
            var value1 = m.readTree(result).at("/values/0").asText();
            assertEquals("value1", value1, "Wrong value");
            var value2 = m.readTree(result).at("/values/1").asText();
            assertEquals("value3", value2, "Wrong value");
            var value3 = m.readTree(result).at("/values/2") instanceof MissingNode;
            assertTrue(value3, "Wrong value");
        });
    }

    @Test
    public void testObjects010Replace() {
        assertDoesNotThrow(() -> {
            var m = new ObjectMapper();
            var ts = new TemplateService();
            var result = ts.generate(NAME,
                    Map.of("OBJECTS010", List.of(Map.of("FIELD", "new_value")))
            );
            var value1 = m.readTree(result).at("/objects010/0/field").asText();
            assertEquals("new_value", value1, "Wrong value");
            var value2 = m.readTree(result).at("/objects010/1/field") instanceof MissingNode;
            assertTrue(value2, "Wrong value");
        });
    }

    @Test
    public void testObjects110Replace() {
        assertDoesNotThrow(() -> {
            var m = new ObjectMapper();
            var ts = new TemplateService();
            var result = ts.generate(NAME,
                    Map.of("OBJECTS110", List.of(Map.of("FIELD", "new_value")))
            );
            var value1 = m.readTree(result).at("/objects110/0/header").asText();
            assertEquals("header", value1, "Wrong value");
            var value2 = m.readTree(result).at("/objects110/1/field").asText();
            assertEquals("new_value", value2, "Wrong value");
            var value3 = m.readTree(result).at("/objects110/2/field") instanceof MissingNode;
            assertTrue(value3, "Wrong value");
        });
    }

    @Test
    public void testObjects111Replace() {
        assertDoesNotThrow(() -> {
            var m = new ObjectMapper();
            var ts = new TemplateService();
            var result = ts.generate(NAME,
                    Map.of("OBJECTS111", List.of(Map.of("FIELD", "new_value")))
            );
            var value1 = m.readTree(result).at("/objects111/0/header").asText();
            assertEquals("header", value1, "Wrong value");
            var value2 = m.readTree(result).at("/objects111/1/field").asText();
            assertEquals("new_value", value2, "Wrong value");
            var value3 = m.readTree(result).at("/objects111/2/footer").asText();
            assertEquals("footer", value3, "Wrong value");
        });
    }

    @Test
    public void testObjects020Replace() {
        assertDoesNotThrow(() -> {
            var m = new ObjectMapper();
            var ts = new TemplateService();
            var result = ts.generate(NAME,
                    Map.of("OBJECTS020", List.of(
                            Map.of(
                                "OBJECT1", Map.of("FIELD1", "new_value_1"),
                                "OBJECT2", Map.of("FIELD2", "new_value_2"))
                    ))
            );
            var value1 = m.readTree(result).at("/objects020/0/field1").asText();
            assertEquals("new_value_1", value1, "Wrong value");
            var value2 = m.readTree(result).at("/objects020/1/field2").asText();
            assertEquals("new_value_2", value2, "Wrong value");
        });
    }

    @Test
    public void testObjects020X2Replace() {
        assertDoesNotThrow(() -> {
            var m = new ObjectMapper();
            var ts = new TemplateService();
            var result = ts.generate(NAME,
                    Map.of("OBJECTS020", List.of(
                            Map.of(
                                    "OBJECT1", Map.of("FIELD1", "new_value_11"),
                                    "OBJECT2", Map.of("FIELD2", "new_value_12")),
                           Map.of(
                                    "OBJECT1", Map.of("FIELD1", "new_value_21"),
                                    "OBJECT2", Map.of("FIELD2", "new_value_22"))
                    ))
            );
            var value11 = m.readTree(result).at("/objects020/0/field1").asText();
            assertEquals("new_value_11", value11, "Wrong value");
            var value12 = m.readTree(result).at("/objects020/1/field2").asText();
            assertEquals("new_value_12", value12, "Wrong value");
            var value21 = m.readTree(result).at("/objects020/2/field1").asText();
            assertEquals("new_value_21", value21, "Wrong value");
            var value22 = m.readTree(result).at("/objects020/3/field2").asText();
            assertEquals("new_value_22", value22, "Wrong value");
        });
    }

    @Test
    public void testObjects121Replace() {
        assertDoesNotThrow(() -> {
            var m = new ObjectMapper();
            var ts = new TemplateService();
            var result = ts.generate(NAME,
                    Map.of("OBJECTS121", List.of(
                            Map.of(
                                    "OBJECT1", Map.of("FIELD1", "new_value_1"),
                                    "OBJECT2", Map.of("FIELD2", "new_value_2"))
                    ))
            );
            var value2 = m.readTree(result).at("/objects121/1/field1").asText();
            assertEquals("new_value_1", value2, "Wrong value");
            var value3 = m.readTree(result).at("/objects121/2/field2").asText();
            assertEquals("new_value_2", value3, "Wrong value");
            var value4 = m.readTree(result).at("/objects121/3/footer").asText();
            assertEquals("footer", value4, "Wrong value");
        });
    }

    @Test
    public void testObjects121X2Replace() {
        assertDoesNotThrow(() -> {
            var m = new ObjectMapper();
            var ts = new TemplateService();
            var result = ts.generate(NAME,
                    Map.of("OBJECTS121", List.of(
                            Map.of(
                                    "OBJECT1", Map.of("FIELD1", "new_value_11"),
                                    "OBJECT2", Map.of("FIELD2", "new_value_12")),
                            Map.of(
                                    "OBJECT1", Map.of("FIELD1", "new_value_21"),
                                    "OBJECT2", Map.of("FIELD2", "new_value_22"))
                    ))
            );
            var value01 = m.readTree(result).at("/objects121/0/header").asText();
            assertEquals("header", value01, "Wrong value");
            var value11 = m.readTree(result).at("/objects121/1/field1").asText();
            assertEquals("new_value_11", value11, "Wrong value");
            var value12 = m.readTree(result).at("/objects121/2/field2").asText();
            assertEquals("new_value_12", value12, "Wrong value");
            var value21 = m.readTree(result).at("/objects121/3/field1").asText();
            assertEquals("new_value_21", value21, "Wrong value");
            var value22 = m.readTree(result).at("/objects121/4/field2").asText();
            assertEquals("new_value_22", value22, "Wrong value");
            var value02 = m.readTree(result).at("/objects121/5/footer").asText();
            assertEquals("footer", value02, "Wrong value");
        });
    }
}
