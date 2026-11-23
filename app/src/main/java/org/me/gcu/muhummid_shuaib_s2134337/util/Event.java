package org.me.gcu.muhummid_shuaib_s2134337.util;

public class Event<T> {
    private final T content;
    private boolean handled = false;

    public Event(T content) { this.content = content; }

    public T getContentIfNotHandled() {
        if (handled) return null;
        handled = true;
        return content;
    }
}
