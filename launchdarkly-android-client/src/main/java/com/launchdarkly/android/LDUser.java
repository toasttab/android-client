package com.launchdarkly.android;


import android.os.Build;
import android.util.Base64;
import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.annotations.Expose;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static com.launchdarkly.android.LDConfig.GSON;

/**
 * A {@code LDUser} object contains specific attributes of a user browsing your site. The only mandatory property property is the {@code key},
 * which must uniquely identify each user. For authenticated users, this may be a username or e-mail address. For anonymous users,
 * this could be an IP address or session ID.
 * <p/>
 * Besides the mandatory {@code key}, {@code LDUser} supports two kinds of optional attributes: interpreted attributes (e.g. {@code ip} and {@code country})
 * and custom attributes.  LaunchDarkly can parse interpreted attributes and attach meaning to them. For example, from an {@code ip} address, LaunchDarkly can
 * do a geo IP lookup and determine the user's country.
 * <p/>
 * Custom attributes are not parsed by LaunchDarkly. They can be used in custom rules-- for example, a custom attribute such as "customer_ranking" can be used to
 * launch a feature to the top 10% of users on a site.
 */
public class LDUser {
    private static final String TAG = "LDUser";
    private static final UserHasher USER_HASHER = new UserHasher();

    @Expose
    private final JsonPrimitive key;
    @Expose
    private final JsonPrimitive secondary;
    @Expose
    private final JsonPrimitive ip;
    @Expose
    private final JsonPrimitive email;
    @Expose
    private final JsonPrimitive name;
    @Expose
    private final JsonPrimitive avatar;
    @Expose
    private final JsonPrimitive firstName;
    @Expose
    private final JsonPrimitive lastName;
    @Expose
    private final JsonPrimitive anonymous;
    @Expose
    private final JsonPrimitive country;
    @Expose
    private final Map<String, JsonElement> custom;

    @Expose(deserialize = false, serialize = false)
    private final String urlSafeBase64;

    @Expose(deserialize = false, serialize = false)
    private final String sharedPrefsKey;

    protected LDUser(Builder builder) {
        if (builder.key == null || builder.key.equals("")) {
            Log.w(TAG, "User was created with null/empty key. " +
                    "Using device-unique anonymous user key: " + LDClient.getInstanceId());
            this.key = new JsonPrimitive(LDClient.getInstanceId());
            this.anonymous = new JsonPrimitive(true);
        } else {
            this.key = new JsonPrimitive(builder.key);
            this.anonymous = builder.anonymous == null ? null : new JsonPrimitive(builder.anonymous);
        }
        this.ip = builder.ip == null ? null : new JsonPrimitive(builder.ip);
        this.country = builder.country == null ? null : new JsonPrimitive(builder.country.getAlpha2());
        this.secondary = builder.secondary == null ? null : new JsonPrimitive(builder.secondary);
        this.firstName = builder.firstName == null ? null : new JsonPrimitive(builder.firstName);
        this.lastName = builder.lastName == null ? null : new JsonPrimitive(builder.lastName);
        this.email = builder.email == null ? null : new JsonPrimitive(builder.email);
        this.name = builder.name == null ? null : new JsonPrimitive(builder.name);
        this.avatar = builder.avatar == null ? null : new JsonPrimitive(builder.avatar);
        this.custom = new HashMap<>(builder.custom);
        String userJson = GSON.toJson(this);
        this.urlSafeBase64 = Base64.encodeToString(userJson.getBytes(), Base64.URL_SAFE + Base64.NO_WRAP);
        this.sharedPrefsKey = USER_HASHER.hash(userJson);
    }

    String getAsUrlSafeBase64() {
        return urlSafeBase64;
    }

    JsonPrimitive getKey() {
        return key;
    }

    String getKeyAsString() {
        if (key == null) {
            return "";
        } else {
            return key.getAsString();
        }
    }

    JsonPrimitive getIp() {
        return ip;
    }

    JsonPrimitive getCountry() {
        return country;
    }

    JsonPrimitive getSecondary() {
        return secondary;
    }

    JsonPrimitive getName() {
        return name;
    }

    JsonPrimitive getFirstName() {
        return firstName;
    }

    JsonPrimitive getLastName() {
        return lastName;
    }

    JsonPrimitive getEmail() {
        return email;
    }

    JsonPrimitive getAvatar() {
        return avatar;
    }

    JsonPrimitive getAnonymous() {
        return anonymous;
    }

    JsonElement getCustom(String key) {
        if (custom != null) {
            return custom.get(key);
        }
        return null;
    }

    String getSharedPrefsKey() {
        return sharedPrefsKey;
    }

    /**
     * A <a href="http://en.wikipedia.org/wiki/Builder_pattern">builder</a> that helps construct {@link LDUser} objects. Builder
     * calls can be chained, enabling the following pattern:
     * <p/>
     * <pre>
     * LDUser user = new LDUser.Builder("key")
     *      .country("US")
     *      .ip("192.168.0.1")
     *      .build()
     * </pre>
     */
    public static class Builder {
        private String key;
        private String secondary;
        private String ip;
        private String firstName;
        private String lastName;
        private String email;
        private String name;
        private String avatar;
        private Boolean anonymous;
        private LDCountryCode country;
        private Map<String, JsonElement> custom;

        /**
         * Create a builder with the specified key
         *
         * @param key the unique key for this user
         */
        public Builder(String key) {
            this.key = key;
            this.custom = new HashMap<>();
            custom.put("os", new JsonPrimitive(Build.VERSION.SDK_INT));
            custom.put("device", new JsonPrimitive(Build.MODEL + " " + Build.PRODUCT));
        }

        public Builder(LDUser user) {
            JsonPrimitive userKey = user.getKey();
            if (userKey.isJsonNull()) {
                this.key = null;
            } else {
                this.key = user.getKeyAsString();
            }
            this.secondary = user.getSecondary() != null ? user.getSecondary().getAsString() : null;
            this.ip = user.getIp() != null ? user.getIp().getAsString() : null;
            this.firstName = user.getFirstName() != null ? user.getFirstName().getAsString() : null;
            this.lastName = user.getLastName() != null ? user.getLastName().getAsString() : null;
            this.email = user.getEmail() != null ? user.getEmail().getAsString() : null;
            this.name = user.getName() != null ? user.getName().getAsString() : null;
            this.avatar = user.getAvatar() != null ? user.getAvatar().getAsString() : null;
            this.anonymous = user.getAnonymous() != null ? user.getAnonymous().getAsBoolean() : null;
            this.country = user.getCountry() != null ? LDCountryCode.valueOf(user.getCountry().getAsString()) : null;
            this.custom = user.custom;
        }

        /**
         * Set the IP for a user
         *
         * @param s the IP address for the user
         * @return the builder
         */
        public Builder ip(String s) {
            this.ip = s;
            return this;
        }

        public Builder secondary(String s) {
            this.secondary = s;
            return this;
        }

        /**
         * Set the country for a user. The country should be a valid <a href="http://en.wikipedia.org/wiki/ISO_3166-1">ISO 3166-1</a>
         * alpha-2 or alpha-3 code. If it is not a valid ISO-3166-1 code, an attempt will be made to look up the country by its name.
         * If that fails, a warning will be logged, and the country will not be set.
         *
         * @param s the country for the user
         * @return the builder
         */
        public Builder country(String s) {
            country = LDCountryCode.getByCode(s, false);

            if (country == null) {
                List<LDCountryCode> codes = LDCountryCode.findByName("^" + Pattern.quote(s) + ".*");

                if (codes.isEmpty()) {
                    Log.w(TAG, "Invalid country. Expected valid ISO-3166-1 code: " + s);
                } else if (codes.size() > 1) {
                    // See if any of the codes is an exact match
                    for (LDCountryCode c : codes) {
                        if (c.getName().equals(s)) {
                            country = c;
                            return this;
                        }
                    }
                    Log.w(TAG, "Ambiguous country. Provided code matches multiple countries: " + s);
                    country = codes.get(0);
                } else {
                    country = codes.get(0);
                }

            }
            return this;
        }

        /**
         * Set the country for a user.
         *
         * @param country the country for the user
         * @return the builder
         */
        public Builder country(LDCountryCode country) {
            this.country = country;
            return this;
        }

        /**
         * Sets the user's first name
         *
         * @param firstName the user's first name
         * @return the builder
         */
        public Builder firstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        /**
         * Sets whether this user is anonymous
         *
         * @param anonymous whether the user is anonymous
         * @return the builder
         */
        public Builder anonymous(boolean anonymous) {
            this.anonymous = anonymous;
            return this;
        }

        /**
         * Sets the user's last name
         *
         * @param lastName the user's last name
         * @return the builder
         */
        public Builder lastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        /**
         * Sets the user's full name
         *
         * @param name the user's full name
         * @return the builder
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the user's avatar
         *
         * @param avatar the user's avatar
         * @return the builder
         */
        public Builder avatar(String avatar) {
            this.avatar = avatar;
            return this;
        }

        /**
         * Sets the user's e-mail address
         *
         * @param email the e-mail address
         * @return the builder
         */
        public Builder email(String email) {
            this.email = email;
            return this;
        }

        /**
         * Add a {@link java.lang.String}-valued custom attribute. When set to one of the
         * <a href="http://docs.launchdarkly.com/docs/targeting-users#targeting-based-on-user-attributes">
         * built-in user attribute keys</a>, this custom attribute will be ignored.
         *
         * @param k the key for the custom attribute.
         * @param v the value for the custom attribute
         * @return the builder
         */
        public Builder custom(String k, String v) {
            checkCustomAttribute(k);
            if (k != null && v != null) {
                custom.put(k, new JsonPrimitive(v));
            }
            return this;
        }

        /**
         * Add a {@link java.lang.Number}-valued custom attribute. When set to one of the
         * <a href="http://docs.launchdarkly.com/docs/targeting-users#targeting-based-on-user-attributes">
         * built-in user attribute keys</a>, this custom attribute will be ignored.
         *
         * @param k the key for the custom attribute. When set to one of the built-in user attribute keys, this custom attribute will be ignored.
         * @param n the value for the custom attribute
         * @return the builder
         */
        public Builder custom(String k, Number n) {
            checkCustomAttribute(k);
            if (k != null && n != null) {
                custom.put(k, new JsonPrimitive(n));
            }
            return this;
        }

        /**
         * Add a {@link java.lang.Boolean}-valued custom attribute. When set to one of the
         * <a href="http://docs.launchdarkly.com/docs/targeting-users#targeting-based-on-user-attributes">
         * built-in user attribute keys</a>, this custom attribute will be ignored.
         *
         * @param k the key for the custom attribute. When set to one of the built-in user attribute keys, this custom attribute will be ignored.
         * @param b the value for the custom attribute
         * @return the builder
         */
        public Builder custom(String k, Boolean b) {
            checkCustomAttribute(k);
            if (k != null && b != null) {
                custom.put(k, new JsonPrimitive(b));
            }
            return this;
        }

        /**
         * Add a list of {@link java.lang.String}-valued custom attributes. When set to one of the
         * <a href="http://docs.launchdarkly.com/docs/targeting-users#targeting-based-on-user-attributes">
         * built-in user attribute keys</a>, this custom attribute will be ignored.
         *
         * @param k  the key for the list. When set to one of the built-in user attribute keys, this custom attribute will be ignored.
         * @param vs the values for the attribute
         * @return the builder
         * @deprecated As of version 0.16.0, renamed to {@link #customString(String, List) customString}
         */
        public Builder custom(String k, List<String> vs) {
            checkCustomAttribute(k);
            return this.customString(k, vs);
        }

        /**
         * Add a list of {@link java.lang.String}-valued custom attributes. When set to one of the
         * <a href="http://docs.launchdarkly.com/docs/targeting-users#targeting-based-on-user-attributes">
         * built-in user attribute keys</a>, this custom attribute will be ignored.
         *
         * @param k  the key for the list. When set to one of the built-in user attribute keys, this custom attribute will be ignored.
         * @param vs the values for the attribute
         * @return the builder
         */
        public Builder customString(String k, List<String> vs) {
            checkCustomAttribute(k);
            JsonArray array = new JsonArray();
            for (String v : vs) {
                if (v != null) {
                    array.add(new JsonPrimitive(v));
                }
            }
            custom.put(k, array);
            return this;
        }

        /**
         * Add a list of {@link java.lang.Integer}-valued custom attributes. When set to one of the
         * <a href="http://docs.launchdarkly.com/docs/targeting-users#targeting-based-on-user-attributes">
         * built-in user attribute keys</a>, this custom attribute will be ignored.
         *
         * @param k  the key for the list. When set to one of the built-in user attribute keys, this custom attribute will be ignored.
         * @param vs the values for the attribute
         * @return the builder
         */
        public Builder customNumber(String k, List<Number> vs) {
            checkCustomAttribute(k);
            JsonArray array = new JsonArray();
            for (Number v : vs) {
                if (v != null) {
                    array.add(new JsonPrimitive(v));
                }
            }
            custom.put(k, array);
            return this;
        }

        private void checkCustomAttribute(String key) {
            for (UserAttribute a : UserAttribute.values()) {
                if (a.name().equals(key)) {
                    Log.w(TAG, "Built-in attribute key: " + key + " added as custom attribute! This custom attribute will be ignored during Feature Flag evaluation");
                    return;
                }
            }
        }

        /**
         * Build the configured {@link com.launchdarkly.android.LDUser} object
         *
         * @return the {@link com.launchdarkly.android.LDUser} configured by this builder
         */
        public LDUser build() {
            return new LDUser(this);
        }
    }
}
