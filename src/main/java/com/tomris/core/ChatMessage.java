package com.tomris.core;

/**
 * Sohbet geçmişindeki tek bir mesajı temsil eder.
 */
public record ChatMessage(Sender sender, String text) {

    /**
     * Mesajın kimden geldiğini belirtir.
     */
    public enum Sender {
        USER,
        TOMRIS,
        SYSTEM
    }
}
