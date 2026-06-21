package net.pullolo.clientpowers.module;

public interface Module {
    String getName();
    boolean isEnabled();
    void setEnabled(boolean enabled);
    default void tick() {}
}
