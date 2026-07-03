package com.lifecell.diia.be;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.time.ZoneId;
import java.util.List;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "com.lifecell.diia.be")
public class Properties {

    @Getter
    @Setter
    public static class Bus {

        @Getter
        @Setter
        public static class Exchange {

            public enum ExchangeType {
                FANOUT, DIRECT, TOPIC;
            }
            @NotBlank
            private String name;
            @NotNull
            private ExchangeType type;
            @NotBlank
            private String routingKey;
        }

        @Getter
        @Setter
        public static class Box {
            @NotNull
            private Exchange exchange;
        }

        @Valid
        @NotNull
        private Box external;
    }

    @Getter
    @Setter
    public static class Flow {

        @Getter
        @Setter
        public static class Session {
            @NotNull
            private Duration timeout;
        }

        @Getter
        @Setter
        public static class Stage {

            @Getter
            @Setter
            public static class Agreement {

                @Getter
                @Setter
                public static class File {
                    @NotNull
                    private Duration timeout;
                    @NotNull
                    private Duration wait;
                }

                @Valid
                @NotNull
                private File file;
            }

            @Getter
            @Setter
            public static class Commit {
                @NotNull
                private Duration timeout;
                @NotNull
                private Duration wait;
            }

            @Valid
            @NotNull
            private Agreement agreement;
            @Valid
            @NotNull
            private Commit commit;
        }

        @Valid
        @NotNull
        private Session session;
        @Valid
        @NotNull
        private Stage stage;
    }

    @Getter
    @Setter
    public static class Local {
        @NotNull
        private ZoneId zoneId;
    }

    @Getter
    @Setter
    public static class Service {

        @Getter
        @Setter
        public static class Diia {

            @Getter
            @Setter
            public static class Document {
                //@Any
                List<@NotNull @Positive Integer> states;
            }

            @Valid
            @NotNull
            private Document document;
        }

        @Getter
        @Setter
        public static class Orders {
            @NotNull
            private Duration timeout;
            @NotNull
            private Duration missingTimeout;
            @NotNull
            private Integer maxSize;
            @NotNull
            private String refreshOrdersActiveCron;
            @NotNull
            private Long refreshOrdersActiveHoursLimit;
            @NotNull
            private Duration refreshOrdersActiveLockAtLeastFor;
            @NotNull
            private Duration refreshOrdersActiveLockAtMostFor;
        }

        @Getter
        @Setter
        public static class PushNotifications {
            @NotNull
            private String donorAcceptWaitSimCard;
            @NotNull
            private String donorAccept;
            @NotNull
            private String activatedMsisdn;
            @NotNull
            private String broadcast;
        }

        @Valid
        @NotNull
        private Diia diia;
        @Valid
        @NotNull
        private Orders orders;
        @Valid
        @NotNull
        private PushNotifications pushNotifications;
    }

    @Valid
    @NotNull
    private Bus bus;
    @Valid
    @NotNull
    private Flow flow;
    @Valid
    @NotNull
    private Local local;
    @Valid
    @NotNull
    private Service service;
}
