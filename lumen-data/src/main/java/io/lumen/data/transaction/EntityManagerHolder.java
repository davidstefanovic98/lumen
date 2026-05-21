package io.lumen.data.transaction;

import jakarta.persistence.EntityManager;

public final class EntityManagerHolder {

    private static final ThreadLocal<EntityManager> HOLDER = new ThreadLocal<>();

    private EntityManagerHolder() {}

    public static void set(EntityManager em)  { HOLDER.set(em); }
    public static EntityManager get()         { return HOLDER.get(); }
    public static void clear()                { HOLDER.remove(); }
    public static boolean hasActive()         { return HOLDER.get() != null; }
}