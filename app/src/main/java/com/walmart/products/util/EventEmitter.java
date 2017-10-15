package com.walmart.products.util;

import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

/**
 * The event emitter which is ported from the JavaScript module. This class is thread-safe.
 *
 * @see <a href="https://github.com/component/emitter">https://github.com/component/emitter</a>
 */
public class EventEmitter {

    protected final String TAG = getClass().getCanonicalName();

    private ConcurrentMap<String, ConcurrentLinkedQueue<Function>> callbacks
            = new ConcurrentHashMap<String, ConcurrentLinkedQueue<Function>>();

    /**
     * Listens on the event.
     * @param event event name.
     * @param fn
     * @return a reference to this object.
     */
    public EventEmitter on(String event, Function fn) {
        ConcurrentLinkedQueue<Function> callbacks = this.callbacks.get(event);
        if (callbacks == null) {
            callbacks = new ConcurrentLinkedQueue <Function>();
            ConcurrentLinkedQueue<Function> tempCallbacks = this.callbacks.putIfAbsent(event, callbacks);
            if (tempCallbacks != null) {
                callbacks = tempCallbacks;
            }
        }
        callbacks.add(fn);
        return this;
    }

    /**
     * Adds a one time listener for the event.
     *
     * @param event an event name.
     * @param fn
     * @return a reference to this object.
     */
    public EventEmitter once(final String event, final Function fn) {
        this.on(event, new OnceListener(event, fn));
        return this;
    }

    /**
     * Removes all registered listeners.
     *
     * @return a reference to this object.
     */
    public EventEmitter off() {
        this.callbacks.clear();
        return this;
    }

    /**
     * Removes all listeners of the specified event.
     *
     * @param event an event name.
     * @return a reference to this object.
     */
    public EventEmitter off(String event) {
        this.callbacks.remove(event);
        return this;
    }

    /**
     * Removes the listener.
     *
     * @param event an event name.
     * @param fn
     * @return a reference to this object.
     */
    public EventEmitter off(String event, Function fn) {
        ConcurrentLinkedQueue<Function> callbacks = this.callbacks.get(event);
        if (callbacks != null) {
            Iterator<Function> it = callbacks.iterator();
            while (it.hasNext()) {
                Function internal = it.next();
                if (EventEmitter.sameAs(fn, internal)) {
                    it.remove();
                    break;
                }
            }
        }
        return this;
    }

    private static boolean sameAs(Function fn, Function internal) {
        if (fn.equals(internal)) {
            return true;
        } else if (internal instanceof OnceListener) {
            return fn.equals(((OnceListener) internal).fn);
        } else {
            return false;
        }
    }

    /**
     * Executes each of listeners with the given args.
     *
     * @param event an event name.
     * @param args
     * @return a reference to this object.
     */
    public EventEmitter emit(String event, Object... args) {
        ConcurrentLinkedQueue<Function> callbacks = this.callbacks.get(event);
        if (callbacks != null) {
            for (Function fn : callbacks) {
                if (args.length == 0) fn.call(null, null);
                else fn.call(args);
            }
        }
        return this;
    }

    /**
     * Returns a list of listeners for the specified event.
     *
     * @param event an event name.
     * @return a reference to this object.
     */
    public List<Function> listeners(String event) {
        ConcurrentLinkedQueue<Function> callbacks = this.callbacks.get(event);
        return callbacks != null ?
                new ArrayList<Function>(callbacks) : new ArrayList<Function>(0);
    }

    /**
     * Check if this emitter has listeners for the specified event.
     *
     * @param event an event name.
     * @return a reference to this object.
     */
    public boolean hasListeners(String event) {
        ConcurrentLinkedQueue<Function> callbacks = this.callbacks.get(event);
        return callbacks != null && !callbacks.isEmpty();
    }

    private class OnceListener implements Function {

        public final String event;
        public final Function fn;

        public OnceListener(String event, Function fn) {
            this.event = event;
            this.fn = fn;
        }

        @Override
        public void call(Object... args) {
            EventEmitter.this.off(this.event, this);
            this.fn.call(args);
        }
    }
}