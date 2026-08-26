package com.gameplatform.game.rules;

/**
 * outcome after validating player's play
 * if play is Valid   -> play is set, reason is null
 * if play is Invalid -> give the reason explains why, play is null
 */
public record MoveResult(boolean valid, String reason, Play play) {

    public static MoveResult ok(Play play) {
        return new MoveResult(true, null, play);
    }

    public static MoveResult invalid(String reason) {
        return new MoveResult(false, reason, null);
    }

}