package com.walmart.products.util;

/**
 * TODO: Look into replacing with Java 8 lambda expressions, but not all android support Java 8 :(
 *
 * Convention: NodeJS inspired callback
 * if args[0] is not null, then args[0] is the error message,
 * otherwise args[1->n] are the success data
 *
 * Known Limitation:  Not able to have compile time checks of arg data types
 *                    Try to fix this...
 *
 * I got the idea from Socket.IO Java code- this is how they implement callbacks on their EventEmitter.
 * they call their interface 'Listener'...
 *
 * source: https://github.com/socketio/engine.io-client-java/blob/master/src/main/java/io/socket/emitter/Emitter.java
 */
public interface Function {
    public void call(Object... args);
}