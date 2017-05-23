CREATE TABLE public.matches
(
    ID UUID DEFAULT uuid_generate_v4() PRIMARY KEY NOT NULL,
    attacker UUID NOT NULL,
    defender UUID NOT NULL,
    winner UUID,
    start_time TIMESTAMP DEFAULT now(),
    end_time TIMESTAMP,
    max_moves INT NOT NULL,
    castleHP INT NOT NULL,
    CONSTRAINT matches_attacker_users_id_fk FOREIGN KEY (attacker) REFERENCES users (id),
    CONSTRAINT matches_defender_users_id_fk FOREIGN KEY (defender) REFERENCES users (id),
    CONSTRAINT matches_winner_users_id_fk FOREIGN KEY (winner) REFERENCES users (id)
);
CREATE INDEX matches_attacker_index ON public.matches (attacker);
CREATE INDEX matches_defender_index ON public.matches (defender);
CREATE INDEX matches_winner_index ON public.matches (winner);


CREATE TABLE public.moves
(
    ID UUID NOT NULL,
    RowID UUID DEFAULT uuid_generate_v4() PRIMARY KEY NOT NULL,
    move_number INT NOT NULL,
    initial_castle_hp INT,
    current_castle_hp INT,
    start_processing_datetime TIMESTAMP DEFAULT now() NOT NULL,
    units VARCHAR(4000),
    alive_units VARCHAR(4000),
    actions VARCHAR(4000),
    attacker_available_cards VARCHAR(4000) NOT NULL,
    defender_available_cards VARCHAR(4000) NOT NULL,
    attacker_chosen_cards VARCHAR(4000),
    defender_chosen_cards VARCHAR(4000),
    CONSTRAINT moves_matches_id_fk FOREIGN KEY (ID) REFERENCES matches (id) ON DELETE CASCADE
);
CREATE UNIQUE INDEX moves_id_move_number_index ON public.moves (id, move_number DESC);