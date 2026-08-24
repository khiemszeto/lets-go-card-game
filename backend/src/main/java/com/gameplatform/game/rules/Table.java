package com.gameplatform.game.rules;

/** all this class does is record what happened on the table*/
public record Table(Play standing, boolean standingIsChop) { // using bomb to kill a 2, hence, chopped

    public static final Table EMPTY = new Table(null, false);

    public boolean isEmpty() {
        return standing == null;
    }

}