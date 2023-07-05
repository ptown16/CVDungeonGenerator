package org.cubeville.cvdungeongenerator.dungeons;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.cubeville.cvgames.enums.CardinalDirection;

import java.util.HashMap;
import java.util.Map;


public class RotationUtils {

    private static final Map<CardinalDirection, Integer> directionToInteger = Map.of(
            CardinalDirection.NORTH, 0,
            CardinalDirection.EAST, 1,
            CardinalDirection.SOUTH, 2,
            CardinalDirection.WEST, 3
    );

    private static final CardinalDirection[] integerToDirection = {CardinalDirection.NORTH, CardinalDirection.EAST, CardinalDirection.SOUTH, CardinalDirection.WEST};

    private static final Map<CardinalDirection, Integer> reverseDirectionToInteger = Map.of(
            CardinalDirection.NORTH, 3,
            CardinalDirection.EAST, 2,
            CardinalDirection.SOUTH, 1,
            CardinalDirection.WEST, 0
    );

    private static final CardinalDirection[] integerToReverseDirection = {CardinalDirection.WEST, CardinalDirection.SOUTH, CardinalDirection.EAST, CardinalDirection.NORTH};

    private static final Map<Integer, Vector> rotationVectors = Map.of(
            0, new Vector(1, 1, 1),
            90, new Vector(-1, 1, 1),
            180, new Vector(-1, 1, -1),
            270, new Vector(1, 1, -1)
    );

    public static Vector getExitDirectionOffset(CardinalDirection direction) {
        switch (direction) {
            case NORTH:
                return new Vector(0, 0, -1);
            case EAST:
                return new Vector(1, 0, 0);
            case SOUTH:
                return new Vector(0, 0, 1);
            case WEST:
                return new Vector(-1, 0, 0);
        }
        return new Vector(0, 0, 0);
    }

    public static CardinalDirection applyRotationToCardinalDirection(CardinalDirection direction, int rotation) {
        int currentDirection = directionToInteger.get(direction);
        currentDirection += (rotation / 90);
        return integerToDirection[currentDirection % 4];
    }

    public static int getRotationFrom(CardinalDirection fromDirection, CardinalDirection toDirection) {
        int rotation = 0;
        final int startDirectionInt = directionToInteger.get(fromDirection);
        for (int i = startDirectionInt; i < startDirectionInt + 4; i++) {
            if (toDirection.equals(integerToDirection[i % 4])) {
                return rotation;
            }
            rotation += 90;
        }
        return 0;
    }

    public static Vector getRotatedRelativeMin(Vector min, Vector max, int rotation) {
        Vector swappedMin, swappedMax;
        // at 90 or 270, swap the X and Z values
        if (rotation % 180 == 0) {
            swappedMin = min.clone();
            swappedMax = max.clone();
        } else {
            swappedMin = new Vector(min.getZ(), min.getY(), min.getX());
            swappedMax = new Vector(max.getZ(), max.getY(), max.getX());
        }
        Vector adjustmentVector = rotationVectors.get(rotation);
        Vector rotatedRelativeMin = swappedMin.multiply(adjustmentVector);
        Vector rotatedRelativeMax = swappedMax.multiply(adjustmentVector);

        // Use the lowest of the 2 X and Z values
        if (rotatedRelativeMin.getBlockX() > rotatedRelativeMax.getBlockX()) {
            rotatedRelativeMin.setX(rotatedRelativeMax.getBlockX());
        }
        if (rotatedRelativeMin.getBlockZ() > rotatedRelativeMax.getBlockZ()) {
            rotatedRelativeMin.setZ(rotatedRelativeMax.getBlockZ());
        }
        return rotatedRelativeMin;
    }
}
