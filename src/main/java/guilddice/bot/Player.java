package guilddice.bot;

import com.alibaba.fastjson2.JSONObject;
import guilddice.Main;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
public class Player {
    private final LinkedList<PlayerCharacter> characters;
    private final UUID id;
    private final String name;
    private PlayerCharacter currentCharacter;

    public Player(UUID id, String name) {
        this.characters = new LinkedList<>();
        this.id = id;
        this.name = name;
    }

    public Player(UUID id, String name, List<PlayerCharacter> characters) {
        this.characters = new LinkedList<>();
        this.characters.addAll(characters);
        this.id = id;
        this.name = name;
    }

    public static Player load(UUID id) throws IOException {
        final Path plPath = Main.PC_ROOT.resolve(id.toString().concat(".json"));
        if (!Files.exists(plPath)) {
            return null;
        }

        final JSONObject obj = JSONObject.parse(Files.readString(plPath));
        if (obj == null) return null;

        final Player player = new Player(id, obj.getString("name"), obj.getList("pc", PlayerCharacter.class));
        final String selectedStr = obj.getString("selected");
        if (selectedStr != null) {
            player.getCharacters().stream().filter(e -> Objects.equals(e.getName(), selectedStr)).findAny().ifPresent(player::setCurrentCharacter);
        }
        return player;
    }

    public void save() throws IOException {
        if (!Files.exists(Main.PC_ROOT)) Files.createDirectories(Main.PC_ROOT);

        final Path curPlPath = Main.PC_ROOT.resolve(id.toString().concat(".json"));
        if (Files.exists(curPlPath)) Files.delete(curPlPath);
        Files.createFile(curPlPath);

        final JSONObject obj = new JSONObject();
        obj.put("name", name);
        obj.put("id", id);
        if (currentCharacter != null) {
            obj.put("selected", currentCharacter.getName());
        }

        obj.put("pc", characters);

        Files.writeString(curPlPath, obj.toString());
    }

    public void delete() throws IOException {
        final Path plPath = Main.PC_ROOT.resolve(id.toString().concat(".json"));
        if (!Files.exists(plPath)) {
            return;
        }
        Files.delete(plPath);
    }
}
