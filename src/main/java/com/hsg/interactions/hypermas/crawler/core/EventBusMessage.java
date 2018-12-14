package com.hsg.interactions.hypermas.crawler.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.google.gson.Gson;

public class EventBusMessage {

    public enum MessageType {
        ADD_REGISTRATION, ADD_REGISTRATION_DATA, REMOVE_REGISTRATION,
        ADD_LINK_CRAWLER, GET_LINKS_CRAWLER, DELETE_LINK_CRAWLER,
    }

    public enum Headers {
        SUBSCRIPTION_URL, REPLY_STATUS
    }

    public enum ReplyStatus {
        SUCCEEDED, ENTITY_NOT_FOUND, FAILED
    }

    private MessageType messageType;
    private Map<Headers,String> headers;
    private String payload;

    public EventBusMessage(MessageType messageType) {
        this.messageType = messageType;
        this.headers = new HashMap<Headers,String>();
    }

    public MessageType getMessageType() {
        return this.messageType;
    }

    public Optional<String> getHeader(Headers key) {
        return headers.get(key) == null ? Optional.empty() : Optional.of(headers.get(key));
    }

    public Map<Headers,String> getHeaders() {
        return this.headers;
    }

    public EventBusMessage setHeader(Headers key, String value) {
        this.headers.put(key, value);
        return this;
    }

    public Optional<String> getPayload() {
        if (payload == null) {
            return Optional.empty();
        }

        return Optional.of(payload);
    }

    public EventBusMessage setPayload(String payload) {
        this.payload = payload;
        return this;
    }

    public Optional<String> getReplyStatus() {
        return getHeader(Headers.REPLY_STATUS);
    }

    public boolean succeded() {
        if (!getHeader(Headers.REPLY_STATUS).isPresent()) {
            return false;
        }

        return getHeader(Headers.REPLY_STATUS).get().equalsIgnoreCase(ReplyStatus.SUCCEEDED.name());
    }

    public String toJson() {
        return new Gson().toJson(this, this.getClass());
    }
}

