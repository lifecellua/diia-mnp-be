package com.lifecell.diia.be.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.lifecell.diia.be.Properties;
import com.lifecell.diia.be.exception.GenericException;
import com.lifecell.diia.be.model.dto.diia.CreateNotificationWithPushesRequest;
import com.lifecell.diia.be.model.dto.diia.QueueMessageData;
import com.lifecell.diia.be.model.dto.lifecell.Request;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessagePropertiesBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.stereotype.Service;
import ua.gov.diia.message.DiiaBus;
import ua.gov.diia.message.data.external.DiiaMessage;
import ua.gov.diia.message.data.external.DiiaPayload;
import ua.gov.diia.message.rabbit.RabbitMessageParams;

import java.util.UUID;
@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {

    private final Properties properties;
    private final DiiaBus<RabbitMessageParams> diiaBus;

    public JsonNode send(final String event, final Request request) {
        var cid = UUID.randomUUID().toString();
        log.info("Sending message with cid={}, event={}", cid, event);

        final var rabbitMessageParams = new RabbitMessageParams();
        rabbitMessageParams.setExchange(properties.getBus().getExternal().getExchange().getName());
        rabbitMessageParams.setRoutingKey(properties.getBus().getExternal().getExchange().getRoutingKey());
        rabbitMessageParams.setCorrelationId(cid);

        final var payload = DiiaPayload.DiiaPayloadBuilder.<Request>diiaPayload()
                .withUuid(cid)
                .withRequest(request)
                .build();

        var jsonNodeDiiaMessage = diiaBus.sendAndReceive(rabbitMessageParams, event, payload);

        log.info("MessageService.send jsonNodeDiiaMessage response: {}", jsonNodeDiiaMessage);

        if (jsonNodeDiiaMessage == null) {
            log.error("No response received from DiiaBus for cid={}, event={}", cid, event);
            throw new GenericException("Failed to send: no response received from DiiaBus");
        }

        if (!jsonNodeDiiaMessage.isSuccessful()) {
            log.error("Failed response from DiiaBus for cid={}, event={}", cid, event);
            throw new GenericException("Failed to send");
        }

        log.info("Successfully received response for cid={}, event={}", cid, event);
        return jsonNodeDiiaMessage.getPayload().getResponse();
    }
}
