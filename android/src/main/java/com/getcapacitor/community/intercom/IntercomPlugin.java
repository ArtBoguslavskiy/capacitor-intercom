package com.getcapacitor.community.intercom;

import android.util.Log;

import com.getcapacitor.Bridge;
import com.getcapacitor.Logger;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.CapConfig;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import io.intercom.android.sdk.Intercom;
import io.intercom.android.sdk.UserAttributes;
import io.intercom.android.sdk.identity.Registration;
import io.intercom.android.sdk.push.IntercomPushClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

@CapacitorPlugin(name = "IntercomPlugin")
public class IntercomPlugin extends Plugin {
    public static Bridge staticBridge = null;

    @Override
    public void load() {
        staticBridge = this.bridge;

        //
        // Set up Intercom
        this.setUpIntercom();

        //
        // load parent
        super.load();
    }

    @PluginMethod
    public void registerIdentifiedUser(PluginCall call) {
        String email = call.getString("email");
        String userId = call.getString("userId");

        Registration registration = new Registration();

        if (email != null && email.length() > 0) {
            registration = registration.withEmail(email);
        }
        if (userId != null && userId.length() > 0) {
            registration = registration.withUserId(userId);
        }
        Intercom.client().registerIdentifiedUser(registration);
        call.resolve();
    }

    @PluginMethod
    public void registerUnidentifiedUser(PluginCall call) {
        Intercom.client().registerUnidentifiedUser();
        call.resolve();
    }

    @PluginMethod
    public void updateUser(PluginCall call) {
        UserAttributes.Builder builder = new UserAttributes.Builder();
        String userId = call.getString("userId");
        if (userId != null && userId.length() > 0) {
            builder.withUserId(userId);
        }
        String email = call.getString("email");
        if (email != null && email.length() > 0) {
            builder.withEmail(email);
        }
        String name = call.getString("name");
        if (name != null && name.length() > 0) {
            builder.withName(name);
        }
        String phone = call.getString("phone");
        if (phone != null && phone.length() > 0) {
            builder.withPhone(phone);
        }
        String languageOverride = call.getString("languageOverride");
        if (languageOverride != null && languageOverride.length() > 0) {
            builder.withLanguageOverride(languageOverride);
        }
        Map<String, Object> customAttributes = mapFromJSON(call.getObject("customAttributes"));
        builder.withCustomAttributes(customAttributes);
        Intercom.client().updateUser(builder.build());
        call.resolve();
    }

    @PluginMethod
    public void logout(PluginCall call) {
        Intercom.client().logout();
        call.resolve();
    }

    @PluginMethod
    public void logEvent(PluginCall call) {
        String eventName = call.getString("name");
        Map<String, Object> metaData = mapFromJSON(call.getObject("data"));

        if (metaData == null) {
            Intercom.client().logEvent(eventName);
        } else {
            Intercom.client().logEvent(eventName, metaData);
        }

        call.resolve();
    }

    @PluginMethod
    public void displayMessenger(PluginCall call) {
        Intercom.client().displayMessenger();
        call.resolve();
    }

    @PluginMethod
    public void displayMessageComposer(PluginCall call) {
        Intercom.client().displayMessageComposer();
        call.resolve();
    }

    @PluginMethod
    public void displayHelpCenter(PluginCall call) {
        Intercom.client().displayHelpCenter();
        call.resolve();
    }

    @PluginMethod
    public void hideMessenger(PluginCall call) {
        call.reject("Not implemented on android.");
    }

    @PluginMethod
    public void displayLauncher(PluginCall call) {
        Intercom.client().setLauncherVisibility(Intercom.VISIBLE);
        call.resolve();
    }

    @PluginMethod
    public void hideLauncher(PluginCall call) {
        Intercom.client().setLauncherVisibility(Intercom.GONE);
        call.resolve();
    }

    @PluginMethod
    public void hideIntercom(PluginCall call) {
        Intercom.client().hideIntercom();
        call.resolve();
    }

    @PluginMethod
    public void displayInAppMessages(PluginCall call) {
        Intercom.client().setInAppMessageVisibility(Intercom.VISIBLE);
        call.resolve();
    }

    @PluginMethod
    public void hideInAppMessages(PluginCall call) {
        Intercom.client().setLauncherVisibility(Intercom.GONE);
        call.resolve();
    }

    @PluginMethod
    public void setUserHash(PluginCall call) {
        String hmac = call.getString("hmac");
        Intercom.client().setUserHash(hmac);
        call.resolve();
    }

    @PluginMethod
    public void setBottomPadding(PluginCall call) {
        String stringValue = call.getString("value");
        int value = Integer.parseInt(stringValue);
        Intercom.client().setBottomPadding(value);
        call.resolve();
    }

    public static void onNewToken(String refreshedToken, IntercomPushClient client) {
        client.sendTokenToIntercom(staticBridge.getActivity().getApplication(), refreshedToken);
    }

    public static void sendRemoteMessage(Map message, IntercomPushClient client) {
        client.handlePush(staticBridge.getActivity().getApplication(), message);
    }

    private void setUpIntercom() {
        try {
            // get config
            CapConfig config = this.bridge.getConfig();
            String apiKey = config.getPluginConfiguration("plugins.IntercomPlugin").getString("android-apiKey");
            String appId = config.getPluginConfiguration("plugins.IntercomPlugin").getString("android-appId");

            // init intercom sdk
            Intercom.initialize(this.getActivity().getApplication(), apiKey, appId);
        } catch (Exception e) {
            Logger.error("IntercomPlugin", "ERROR: Something went wrong when initializing Intercom. Check your configurations", e);
        }
    }

    private static Map<String, Object> mapFromJSON(JSONObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }
        Map<String, Object> map = new HashMap<>();
        Iterator<String> keysIter = jsonObject.keys();
        while (keysIter.hasNext()) {
            String key = keysIter.next();
            Object value = getObject(jsonObject.opt(key));
            if (value != null) {
                map.put(key, value);
            }
        }
        return map;
    }

    private static Object getObject(Object value) {
        if (value instanceof JSONObject) {
            value = mapFromJSON((JSONObject) value);
        } else if (value instanceof JSONArray) {
            value = listFromJSON((JSONArray) value);
        }
        return value;
    }

    private static List<Object> listFromJSON(JSONArray jsonArray) {
        List<Object> list = new ArrayList<>();
        for (int i = 0, count = jsonArray.length(); i < count; i++) {
            Object value = getObject(jsonArray.opt(i));
            if (value != null) {
                list.add(value);
            }
        }
        return list;
    }
}
