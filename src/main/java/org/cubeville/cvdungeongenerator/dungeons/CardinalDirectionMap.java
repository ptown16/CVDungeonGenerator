package org.cubeville.cvdungeongenerator.dungeons;

import org.cubeville.cvgames.enums.CardinalDirection;

import java.util.HashMap;
import java.util.Map;

public class CardinalDirectionMap {
    private static final Map<CardinalDirection, Integer> directionToInteger = Map.of(
        CardinalDirection.NORTH, 0,
        CardinalDirection.EAST, 1,
        CardinalDirection.SOUTH, 2,
        CardinalDirection.WEST, 3
    );

    private static final CardinalDirection[] integerToDirection = { CardinalDirection.NORTH, CardinalDirection.EAST, CardinalDirection.SOUTH, CardinalDirection.WEST };

    static CardinalDirection combineRotations(CardinalDirection d1, CardinalDirection d2) {
        return integerToDirection[(directionToInteger.get(d1) + directionToInteger.get(d2)) % 4];
    }
}
