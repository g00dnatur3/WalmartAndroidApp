package com.walmart.products.util;

/**
 *
 * Convention: NodeJS inspired callback
 * if args[0] is not null, then args[0] is the error message,
 * otherwise args[1->n] are the success data
 *
 * Known Limitation:  Not able to have compile time checks of arg data types
 *                    Try to fix this...
 *
 * source: https://github.com/socketio/engine.io-client-java/blob/master/src/main/java/io/socket/emitter/Emitter.java
 */
public interface Function {
    public void call(Object... args);
}