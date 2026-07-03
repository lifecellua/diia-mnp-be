package com.lifecell.diia.be.configuration;

import com.fasterxml.uuid.Generators;
import com.lifecell.diia.be.exception.GenericException;
import com.lifecell.diia.be.model.db.ext.EEntity;
import com.lifecell.diia.be.model.db.ext.Jsonb;
import org.postgresql.util.PGobject;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.event.BeforeConvertCallback;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@Configuration(proxyBeanMethods = false)
@EntityScan("com.lifecell.diia.be.model.db")
@EnableJdbcRepositories("com.lifecell.diia.be.repository")
@EnableTransactionManagement
//@EnableJdbcAuditing
public class JdbcRepositoryConfiguration {

    public JdbcRepositoryConfiguration() {
    }

    /*
    @Bean
    public AuditorAware<UUID> auditorProvider(SecurityService securityService) {
        return () -> Optional.ofNullable(securityService.getCurrentAccountId());
    }
    */

    @Component
    public static class JdbcCustomIdGenerator implements BeforeConvertCallback<EEntity> {

        public JdbcCustomIdGenerator() {
        }

        private static UUID generateUUID() {
            return Generators.timeBasedEpochGenerator().generate();
        }

        private String constructFileName(UUID id, String postfix) {
            return id.toString().toLowerCase() + postfix;
        }

        @Override
        public EEntity onBeforeConvert(EEntity aggregate) {
            touchId(aggregate);
            return  aggregate;
        }

        private void touchId(Object entity) {
            if (entity != null) {
                if (entity instanceof EEntity base) {
                    if (base.getId() == null) {
                        var id = generateUUID();
                        base.setId(id);
                    }
                }
                touchFields(entity);
            }
        }

        private void touchFields(Object entity) {
            if (entity != null) {
                var clz = entity.getClass();
                while (clz != null) {
                    for (var field : entity.getClass().getDeclaredFields()) {
                        if (field.isAnnotationPresent(MappedCollection.class)) {
                            try {
                                var isAccessible = field.canAccess(entity);
                                if (!isAccessible) {
                                    field.setAccessible(true);
                                }
                                var val = field.get(entity);
                                if (val instanceof Iterable) {
                                    for (var itm : (Iterable<?>) val) {
                                        touchId(itm);
                                    }
                                }
                                if (!isAccessible) {
                                    field.setAccessible(false);
                                }
                            } catch (IllegalAccessException e) {
                                throw new GenericException("Can't generate entity id", e);
                            }
                        }
                    }
                    clz = clz.getSuperclass();
                }
            }
        }

    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnExpression("'${spring.datasource.url}'.startsWith('jdbc:postgresql:')")
    public static class CustomPostgresJdbcConfiguration extends AbstractJdbcConfiguration {

        public CustomPostgresJdbcConfiguration() {
        }

        @Override
        protected List<?> userConverters() {
            return List.of(
                    new JsonbReadingConverter(),
                    new JsonbWritingConverter()
            );
        }

        @ReadingConverter
        public static class JsonbReadingConverter implements Converter<PGobject, Jsonb> {

            public JsonbReadingConverter() {
            }

            @Override
            public Jsonb convert(PGobject source) {
                return new Jsonb(source.getValue());
            }
        }

        @WritingConverter
        public static class JsonbWritingConverter implements Converter<Jsonb, PGobject> {

            public JsonbWritingConverter() {
            }

            @Override
            public PGobject convert(Jsonb source) {
                try {
                    var result = new PGobject();
                    result.setType("json");
                    result.setValue(source.getValue());
                    return result;
                } catch (SQLException e) {
                    throw new GenericException("Can't convert to jsonb value", e);
                }
            }
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnExpression("'${spring.datasource.url}'.startsWith('jdbc:h2:')")
    public static class CustomH2JdbcConfiguration extends AbstractJdbcConfiguration {

        public CustomH2JdbcConfiguration() {
        }

        @Override
        protected List<?> userConverters() {
            return List.of(
                    new JsonbReadingConverter(),
                    new JsonbWritingConverter()
            );
        }

        @ReadingConverter
        public static class JsonbReadingConverter implements Converter<byte[], Jsonb> {

            public JsonbReadingConverter() {
            }

            @Override
            public Jsonb convert(byte[] source) {
                return Jsonb.of(new String(source, StandardCharsets.UTF_8));
            }
        }

        @WritingConverter
        public static class JsonbWritingConverter implements Converter<Jsonb, byte[]> {

            public JsonbWritingConverter() {
            }

            @Override
            public byte[] convert(Jsonb source) {
                return source.getValue().getBytes(StandardCharsets.UTF_8);
            }
        }
    }
}
