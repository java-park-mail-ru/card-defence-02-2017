package com.kvteam.backend.gameplay;

import java.util.UUID;

/**
 * Created by maxim on 23.04.17.
 */
public class CastleTowers {
    public static final Unit TOP =
            new Unit(UUID.fromString("11111111-1111-1111-1111-111111111111"), "fictive top castle tower");

    public static final Unit BOTTOM =
            new Unit(UUID.fromString("22222222-2222-2222-2222-222222222222"), "fictive bottom castle tower");
}
