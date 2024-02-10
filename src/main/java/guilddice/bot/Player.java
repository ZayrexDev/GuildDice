package guilddice.bot;

import lombok.Getter;
import lombok.Setter;

import java.util.LinkedList;

@Getter @Setter
public class Player {
    private final LinkedList<PlayerCharacter> characters;
    private PlayerCharacter currentCharacter;
    private final String id;
    private final String name;

    public Player(String id, String name) {
        this.characters = new LinkedList<>();
        this.id = id;
        this.name = name;
    }
}
