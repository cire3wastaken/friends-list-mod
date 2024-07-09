package me.cire3;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URL;

public class FriendsListMod implements ModInitializer {
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger("friends-list-mod");
	public static JsonObject data = null;

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("Hello Fabric world!");

		try (BufferedInputStream in = new BufferedInputStream(new URL("https://github.com/cire3wastaken/friends-list-mod/blob/1_21/src/main/resources/data.json").openStream())) {
			ByteArrayOutputStream bao = new ByteArrayOutputStream();
			byte[] buffer = new byte[1024];
			int count = 0;
			while ((count = in.read(buffer, 0, 1024)) != -1) {
				bao.write(buffer, 0, count);
			}
			data = JsonParser.parseString(bao.toString()).getAsJsonObject();
		} catch (Exception e) {
			// use hard coded list instead
			data = null;
		}

		if (data == null) {
			// TODO
		}
	}
}