package me.peridot.premiumlogintest.storage;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.concurrent.TimeUnit;

public class PremiumNicknameCache {

    private final Cache<String, Boolean> premiumNicknameCache = CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).build();

    public boolean isPremium(String nickname) {
        boolean premium = false;
        Boolean value = premiumNicknameCache.getIfPresent(nickname);
        if (value == null) {
            if (getUUIDOfUsername(nickname) != null) {
                premium = true;
            }
        } else {
            premium = value;
        }
        return premium;
    }

    public String getUUIDOfUsername(String username) {
        try {
            return (String) getJSONObject("https://api.mojang.com/users/profiles/minecraft/" + username).get("id");
        } catch (Exception ex) {
            return null;
        }
    }

    private JSONObject getJSONObject(String url) {
        JSONObject obj;

        try {
            obj = (JSONObject) new JSONParser().parse(Unirest.get(url).asString().getBody());
            String err = (String) (obj.get("error"));
            if (err != null) {
                switch (err) {
                    case "IllegalArgumentException":
                        throw new IllegalArgumentException((String) obj.get("errorMessage"));
                    default:
                        throw new RuntimeException(err);
                }
            }
        } catch (ParseException | UnirestException ex) {
            throw new RuntimeException(ex);
        }

        return obj;
    }

}
