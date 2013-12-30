package com.readtracker.support;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ReadTrackerUserTest {

    @Test
    public void createFromJson() throws JSONException {
        JSONObject blob = new JSONObject() {{
            put("id", 123L);
            put("email", "janedoe@example.com");
            put("fullname", "Jane Doe");
            put("permalink_url", "https://readmill.com/janedoe");
            put("avatar_url", "https://avatars.readmill.com/janedoe.jpeg");
        }};

        ReadTrackerUser user = new ReadTrackerUser(blob);

        assertEquals(123L, user.getReadmillId());
        assertEquals("janedoe@example.com", user.getEmail());
        assertEquals("Jane Doe", user.getDisplayName());
        assertEquals("https://readmill.com/janedoe", user.getWebURL());
        assertEquals("https://avatars.readmill.com/janedoe.jpeg", user.getAvatarURL());
    }
}
