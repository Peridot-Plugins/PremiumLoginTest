package me.peridot.premiumlogintest.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

public class PremiumLoginEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final String nickname;
    private final UUID uuid;
    private final Result result;

    public PremiumLoginEvent(String nickname, UUID uuid, Result result) {
        this.nickname = nickname;
        this.uuid = uuid;
        this.result = result;
    }

    public String getNickname() {
        return nickname;
    }

    public UUID getUuid() {
        return uuid;
    }

    public Result getResult() {
        return result;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public enum Result {
        PREMIUM,
        CRACKED;

        Result() {
        }
    }

}
