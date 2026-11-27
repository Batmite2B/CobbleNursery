package bat.cobblenursery.nest;

import bat.cobblenursery.text.NurseryMessages;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NurseryLocalizationTest {
    @Test void regionalSpanishFilesAndLanguageKeysAreIdentical() throws Exception {
        Path lang=Path.of("src/main/resources/assets/cobblenursery/lang");
        byte[] spanish=Files.readAllBytes(lang.resolve("es_es.json"));
        for(String locale:List.of("es_ar","es_cl","es_ec","es_mx","es_uy","es_ve"))
            assertArrayEquals(spanish,Files.readAllBytes(lang.resolve(locale+".json")),locale);
        var en=JsonParser.parseString(Files.readString(lang.resolve("en_us.json"))).getAsJsonObject();
        var es=JsonParser.parseString(Files.readString(lang.resolve("es_es.json"))).getAsJsonObject();
        assertEquals(en.keySet(),es.keySet());
        for(String key:en.keySet()) {
            var slots=Pattern.compile("%(?:[0-9]+\\$)?s");
            assertEquals(slots.matcher(en.get(key).getAsString()).results().count(),
                    slots.matcher(es.get(key).getAsString()).results().count(),key);
        }
    }

    @Test void wireFailuresBecomeClientTranslationsWithNestedLocalizedReason() {
        var fee=(TranslatableContents)NurseryMessages.component("You need 128 relic coins to unlock this yard.").getContents();
        assertEquals("gui.cobblenursery.feedback.cost",fee.getKey());
        assertArrayEquals(new Object[]{"128"},fee.getArgs());
        var pair=(TranslatableContents)NurseryMessages.component("This pair cannot breed: no_shared_egg_group").getContents();
        assertEquals("gui.cobblenursery.feedback.cannot_breed",pair.getKey());
        var reason=(net.minecraft.network.chat.Component)pair.getArgs()[0];
        assertEquals("gui.cobblenursery.feedback.no_shared_egg_group",((TranslatableContents)reason.getContents()).getKey());
        assertEquals("Unknown older server message",NurseryMessages.component("Unknown older server message").getString());
    }
}
