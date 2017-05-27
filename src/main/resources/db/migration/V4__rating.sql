ALTER TABLE public.users ADD rating INT DEFAULT 0 NOT NULL;
CREATE INDEX users_rating_index ON public.users (rating DESC);

ALTER TABLE public.moves ALTER COLUMN units TYPE TEXT USING units::TEXT;
ALTER TABLE public.moves ALTER COLUMN alive_units TYPE TEXT USING alive_units::TEXT;
ALTER TABLE public.moves ALTER COLUMN actions TYPE TEXT USING actions::TEXT;
ALTER TABLE public.moves ALTER COLUMN attacker_available_cards TYPE TEXT USING attacker_available_cards::TEXT;
ALTER TABLE public.moves ALTER COLUMN defender_available_cards TYPE TEXT USING defender_available_cards::TEXT;
ALTER TABLE public.moves ALTER COLUMN attacker_chosen_cards TYPE TEXT USING attacker_chosen_cards::TEXT;
ALTER TABLE public.moves ALTER COLUMN defender_chosen_cards TYPE TEXT USING defender_chosen_cards::TEXT;