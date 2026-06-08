package io.lumen.core;

/**
 * Marker for LumenInitializer lights that must run after the WebSocket SCI
 * (WsSci) so the JSR-356 ServerContainer is present in the ServletContext.
 *
 * Execution order: LumenSCI (filters) → WsSci → DeferredSCI (these lights).
 */
public interface DeferredLumenInitializer extends LumenInitializer {
}