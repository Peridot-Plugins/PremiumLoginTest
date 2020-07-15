package me.peridot.premiumlogintest.handlers;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import io.netty.channel.Channel;
import me.peridot.premiumlogintest.PremiumLoginTest;
import me.peridot.premiumlogintest.events.PremiumLoginEvent;
import me.peridot.premiumlogintest.storage.PremiumNicknameCache;
import me.peridot.premiumlogintest.tinyprotocol.Reflection;
import me.peridot.premiumlogintest.tinyprotocol.TinyProtocol;
import org.bukkit.Server;
import org.bukkit.entity.Player;

import javax.crypto.SecretKey;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PremiumLoginHandler {

    private final PremiumLoginTest plugin;
    private final Logger logger;

    private final PremiumNicknameCache premiumNicknameCache;

    private final Random random = new Random();

    private final Object server;
    private final byte[] token = new byte[4];

    private final Class<?> packetLoginInStartClass = Reflection.getMinecraftClass("PacketLoginInStart");
    private final Class<?> packetLoginInEncryptionBeginClass = Reflection.getMinecraftClass("PacketLoginInEncryptionBegin");

    private final Class<?> craftServerClass = Reflection.getCraftBukkitClass("CraftServer");
    private final Class<?> minecraftServerClass = Reflection.getMinecraftClass("MinecraftServer");
    private final Class<?> networkManagerClass = Reflection.getMinecraftClass("NetworkManager");
    private final Class<?> loginListenerClass = Reflection.getMinecraftClass("LoginListener");
    private final Class<?> loginHandlerClass = Reflection.getMinecraftClass("LoginListener$LoginHandler");
    private final Class<?> minecraftEncryptionClass = Reflection.getMinecraftClass("MinecraftEncryption");

    private final Class enumProtocolStateClass = Reflection.getMinecraftClass("LoginListener$EnumProtocolState");

    private final Reflection.ConstructorInvoker packetLoginInEncryptionBeginConstructor = Reflection.getConstructor(Reflection.getMinecraftClass("PacketLoginOutEncryptionBegin"), String.class, PublicKey.class, byte[].class);
    private final Reflection.ConstructorInvoker loginHandlerConstructor = Reflection.getConstructor(Reflection.getMinecraftClass("LoginListener$LoginHandler"), loginListenerClass);

    private final Reflection.MethodInvoker getMinecraftServer = Reflection.getMethod(craftServerClass, "getServer");
    private final Reflection.MethodInvoker getPacketListener = Reflection.getMethod(networkManagerClass, "getPacketListener");

    private final Reflection.MethodInvoker minecraftEncryptiona = Reflection.getMethod(minecraftEncryptionClass, "a", String.class, PublicKey.class, SecretKey.class);
    private final Reflection.MethodInvoker loginListenerDisconnect = Reflection.getMethod(loginListenerClass, "disconnect", String.class);
    private final Reflection.MethodInvoker loginListenerInitUUID = Reflection.getMethod(loginListenerClass, "initUUID");
    private final Reflection.MethodInvoker loginListenerFireEvents = Reflection.getMethod(loginHandlerClass, "fireEvents");

    private final Reflection.MethodInvoker getAFromPacketLoginInStart = Reflection.getMethod(packetLoginInStartClass, "a");
    private final Reflection.MethodInvoker getAFromPacketLoginInEncryptionBegin = Reflection.getMethod(packetLoginInEncryptionBeginClass, "a", PrivateKey.class);

    private final Reflection.MethodInvoker getaDFromMinecraftServer = Reflection.getMethod(minecraftServerClass, "aD");
    private final Reflection.MethodInvoker getQFromMinecraftServer = Reflection.getMethod(minecraftServerClass, "Q");
    private final Reflection.MethodInvoker getTFromMinecraftServer = Reflection.getMethod(minecraftServerClass, "T");

    private final Reflection.MethodInvoker getgFromNetworkManager = Reflection.getMethod(networkManagerClass, "g");

    private final Reflection.MethodInvoker setaInNetworkManager = Reflection.getMethod(networkManagerClass, "a", SecretKey.class);

    private final Reflection.FieldAccessor loginListeneri = Reflection.getField(loginListenerClass, "i", GameProfile.class);
    private final Reflection.FieldAccessor loginListenerg = Reflection.getField(loginListenerClass, "g", enumProtocolStateClass);

    private final TinyProtocol tinyProtocol;

    public PremiumLoginHandler(PremiumLoginTest plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();

        this.premiumNicknameCache = plugin.getPremiumNicknameCache();

        server = getMinecraftServer(plugin.getServer());
        random.nextBytes(token);

        this.tinyProtocol = new TinyProtocol(plugin) {
            @Override
            public Object onPacketInAsync(Player sender, Channel channel, Object packet) {
                Object networkManager = channel.pipeline().get("packet_handler");
                KeyPair keyPair = (KeyPair) getQFromMinecraftServer.invoke(server);

                if (packetLoginInStartClass.isInstance(packet)) {
                    Object loginListener = getPacketListener.invoke(networkManager);

                    GameProfile gameProfile = (GameProfile) getAFromPacketLoginInStart.invoke(packet);

                    loginListeneri.set(loginListener, gameProfile);
                    loginListenerInitUUID.invoke(loginListener);

                    if (premiumNicknameCache.isPremium(gameProfile.getName())) {
                        loginListenerg.set(loginListener, Enum.valueOf(enumProtocolStateClass, "KEY"));
                        channel.pipeline().writeAndFlush(packetLoginInEncryptionBeginConstructor.invoke("", keyPair.getPublic(), token));
                    } else {
                        gameProfile = (GameProfile) loginListeneri.get(loginListener);
                        loginListenerFireEvents.invoke(loginHandlerConstructor.invoke(loginListener));
                        callEvent(gameProfile.getName(), gameProfile.getId(), PremiumLoginEvent.Result.CRACKED);
                    }

                    return null;
                } else if (packetLoginInEncryptionBeginClass.isInstance(packet)) {
                    Object loginListener = getPacketListener.invoke(networkManager);

                    SecretKey loginKey = (SecretKey) getAFromPacketLoginInEncryptionBegin.invoke(packet, keyPair.getPrivate());
                    loginListenerg.set(loginListener, Enum.valueOf(enumProtocolStateClass, "AUTHENTICATING"));
                    setaInNetworkManager.invoke(networkManager, loginKey);

                    GameProfile i = Reflection.getField(loginListener.getClass(), "i", GameProfile.class).get(loginListener);

                    try {
                        String s = (new BigInteger((byte[]) minecraftEncryptiona.invoke(null, "", keyPair.getPublic(), loginKey))).toString(16);

                        GameProfile gameProfile = ((MinecraftSessionService) getaDFromMinecraftServer.invoke(server)).hasJoinedServer(new GameProfile(null, i.getName()), s);
                        if (gameProfile != null) {
                            if (!((boolean) getgFromNetworkManager.invoke(networkManager))) {
                                return null;
                            }

                            loginListenerFireEvents.invoke(loginHandlerConstructor.invoke(loginListener));
                            callEvent(gameProfile.getName(), gameProfile.getId(), PremiumLoginEvent.Result.PREMIUM);
                        } else if ((boolean) getTFromMinecraftServer.invoke(server)) {
                            logger.warning("Failed to verify username but will let them in anyway!");
                            loginListenerg.set(loginListener, Enum.valueOf(enumProtocolStateClass, "READY_TO_ACCEPT"));
                        } else {
                            loginListenerDisconnect.invoke(loginListener, "Failed to verify username!");
                            logger.severe("Username '" + gameProfile.getName() + "' tried to join with an invalid session");
                        }
                    } catch (AuthenticationUnavailableException ex) {
                        if ((boolean) getTFromMinecraftServer.invoke(server)) {
                            logger.warning("Authentication servers are down but will let them in anyway!");
                            loginListenerg.set(loginListener, Enum.valueOf(enumProtocolStateClass, "READY_TO_ACCEPT"));
                        } else {
                            loginListenerDisconnect.invoke(loginListener, "Authentication servers are down. Please try again later, sorry!");
                            logger.severe("Couldn't verify username because servers are unavailable");
                        }

                    } catch (Exception ex) {
                        loginListenerDisconnect.invoke(loginListener, "Failed to verify username!");
                        logger.log(Level.WARNING, "Exception verifying " + i.getName(), ex);
                    }
                    return null;
                }

                return super.onPacketInAsync(sender, channel, packet);
            }
        };
    }

    private Object getMinecraftServer(Server server) {
        return getMinecraftServer.invoke(server);
    }

    private void callEvent(String nickname, UUID uuid, PremiumLoginEvent.Result result) {
        plugin.getServer().getPluginManager().callEvent(new PremiumLoginEvent(nickname, uuid, result));
    }

}
