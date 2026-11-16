package com.mtg.mtgwalletbe.repository;

/**
 * A row whose search column still needs filling: its id and the text to fold (see SearchTextBackfill).
 */
public interface SearchTextSource {
    Long getId();

    String getText();
}
