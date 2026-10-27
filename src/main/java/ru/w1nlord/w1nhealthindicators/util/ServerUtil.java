package ru.w1nlord.w1nhealthindicators.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.ReadableScoreboardScore;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.number.NumberFormat;
import net.minecraft.scoreboard.number.StyledNumberFormat;
import ru.w1nlord.w1nhealthindicators.config.Config;

public final class ServerUtil {

    public static boolean isNPC(PlayerEntity player) {
        return MinecraftClient.getInstance().getNetworkHandler().getPlayerListEntry(player.getUuid()) == null;
    }

    public static float resolveHealth(LivingEntity living, Config.HealthSource source) {
        float health = switch (source) {
            case SCOREBOARD -> fromScore(living);
            case FORMATTED_TEXT -> fromString(living);
            case NONE -> living.getHealth();
        };
        return health != -1 ? health : living.getHealth();
    }

    private static int fromScore(LivingEntity living) {
        Scoreboard scoreboard = living.getEntityWorld().getScoreboard();
        ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.BELOW_NAME);
        if (objective == null) return -1;
        ReadableScoreboardScore score = scoreboard.getScore(living, objective);
        if (score == null) return -1;
        return score.getScore();
    }

    private static int fromString(LivingEntity living) {
        Scoreboard scoreboard = living.getEntityWorld().getScoreboard();
        ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.BELOW_NAME);
        if (objective == null) return -1;
        ReadableScoreboardScore score = scoreboard.getScore(living, objective);
        if (score == null) return -1;
        NumberFormat format = score.getNumberFormat();
        if (format == null) {
            format = StyledNumberFormat.EMPTY;
        }
        String text = score.getFormattedScore(format).getString();
        int length = text.length();
        int i = 0;
        while (i < length) {
            char c = text.charAt(i);
            if (c == '§' && i + 1 < length) {
                i += 2;
                continue;
            }
            if (c < '0' || c > '9') {
                i++;
                continue;
            }
            int start = i;
            int number = 0;
            while (i < length) {
                c = text.charAt(i);

                if (c >= '0' && c <= '9') {
                    number = number * 10 + c - '0';
                    i++;
                } else {
                    break;
                }
            }
            int prev = start - 1;
            while (prev >= 0 && text.charAt(prev) == '§') {
                prev--;
            }
            if (prev >= 0 && (text.charAt(prev) == 'T' || text.charAt(prev) == 't')) {
                continue;
            }
            return number;
        }
        return -1;
    }
}
