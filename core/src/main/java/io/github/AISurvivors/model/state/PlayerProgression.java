package io.github.AISurvivors.model.state;

/**
 * Progressao de nivel do player.
 *
 * <p>Nesta versao nao existe gema de XP: cada inimigo morto ja concede XP direto
 * (o {@code GameScreen} converte abates em XP). Ao acumular XP suficiente o
 * player sobe de nivel, e a quantidade necessaria para o proximo nivel cresce.</p>
 */
public class PlayerProgression {
    private static final int BASE_XP_TO_LEVEL = 30;   // XP para ir do nivel 1 ao 2
    private static final int XP_STEP_PER_LEVEL = 20;  // quanto o custo cresce por nivel

    private int level = 1;
    private int xp;                                   // XP acumulado dentro do nivel atual
    private int xpToNextLevel = BASE_XP_TO_LEVEL;

    /**
     * Adiciona XP e retorna quantos niveis subiram nesta chamada (0 se nenhum).
     * O laco trata o caso de ganhar XP suficiente para varios niveis de uma vez.
     */
    public int addXp(int amount) {
        if (amount <= 0) {
            return 0;
        }

        xp += amount;
        int levelsGained = 0;
        while (xp >= xpToNextLevel) {
            xp -= xpToNextLevel;
            level++;
            levelsGained++;
            xpToNextLevel = BASE_XP_TO_LEVEL + (level - 1) * XP_STEP_PER_LEVEL;
        }

        return levelsGained;
    }

    public int getLevel() {
        return level;
    }

    public int getXp() {
        return xp;
    }

    public int getXpToNextLevel() {
        return xpToNextLevel;
    }

    /** Fracao (0..1) do XP em direcao ao proximo nivel, para desenhar a barra. */
    public float getXpPercent() {
        return (float) xp / xpToNextLevel;
    }
}
