package com.blakekellar.sportscomputer.model

import com.fasterxml.jackson.annotation.JsonProperty

data class MlbGames(
        val copyright: String, //Copyright 2018 MLB Advanced Media, L.P.  Use of any content on this page acknowledges agreement to the terms posted here http://gdx.mlb.com/components/copyright.txt
        val dates: List<Date>,
        val totalEvents: Int, //0
        val totalGames: Int, //2969
        val totalGamesInProgress: Int, //3
        val totalItems: Int //2969
)

data class Date(
        // TODO: Local? Date
        val date: String, //2018-09-30
        val events: List<Any>,
        val games: List<Game>,
        val totalEvents: Int, //0
        val totalGames: Int, //15
        val totalGamesInProgress: Int, //0
        val totalItems: Int //15
)

data class Game(
        // TODO: Type
        val calendarEventID: String, //14-531834-2018-09-30
        val content: Content,
        // TODO: Enum
        val dayNight: String, //day
        // TODO: Enum
        val doubleHeader: String, //N
        // TODO: ISO8601 Date
        val gameDate: String, //2018-09-30T19:20:00Z
        val gameNumber: Int, //1
        val gamePk: Int, //531834
        // TODO: Enum
        val gameType: String, //R
        // TODO: Enum
        val gamedayType: String, //P
        val gamesInSeries: Int, //3
        // TODO: Enum
        val ifNecessary: String, //N
        // TODO: Enum
        val ifNecessaryDescription: String, //Normal Game
        val link: String, ///api/v1/game/531834/feed/live
        // TODO: Enum
        val recordSource: String, //S
        val scheduledInnings: Int, //9
        // TODO: String->Int?
        val season: String, //2018
        // TODO: String->Int?
        val seasonDisplay: String, //2018
        // TODO: Enum
        val seriesDescription: SeriesDescription, //Regular Season
        val seriesGameNumber: Int, //3
        val status: Status,
        val teams: Teams,
        // TODO: Enum
        val tiebreaker: String, //N
        val venue: Venue
)

enum class SeriesDescription {
    @JsonProperty("Exhibition Game")
    EXHIBITION,
    @JsonProperty("MLB All-Star Game")
    ALLSTAR,
    @JsonProperty("Spring Training")
    SPRINGTRAINING,
    @JsonProperty("Regular Season")
    REGULAR,
    @JsonProperty("AL Wild Card Game")
    ALWILDCARD,
    @JsonProperty("NL Wild Card Game")
    NLWILDCARD,
    @JsonProperty("AL Division Series")
    ALDIVISIONSERIES,
    @JsonProperty("NL Division Series")
    NLDIVISIONSERIES,
    @JsonProperty("AL Championship Series")
    ALCHAMPIONSHIPSERIES,
    @JsonProperty("NL Championship Series")
    NLCHAMPIONSHIPSERIES,
    @JsonProperty("World Series")
    WORLDSERIES
}

enum class DetailedState {
    @JsonProperty("Postponed")
    POSTPONED,
    @JsonProperty("Final")
    FINAL,
    @JsonProperty("Completed Early")
    COMPLETEDEARLY,
    @JsonProperty("Cancelled")
    CANCELLED,
    @JsonProperty("Suspended")
    SUSPENDED,
    @JsonProperty("In Progress")
    INPROGRESS,
    @JsonProperty("Scheduled")
    SCHEDULED,
    @JsonProperty("Pre-Game")
    PREGAME,
    @JsonProperty("Game Over")
    GAMEOVER
}

data class Status(
        // TODO: Enum
        val abstractGameCode: String, //P
        // TODO: Enum
        val abstractGameState: String, //Preview
        // TODO: Enum
        val codedGameState: String, //S
        val detailedState: DetailedState, //Scheduled
        // TODO: Enum
        val statusCode: String //S
)

data class Content(
        val link: String ///api/v1/game/531834/content
)

data class Venue(
        val id: Int, //17
        val link: String, ///api/v1/venues/17
        val name: String //Wrigley Field
)

data class Teams(
        val away: Away,
        val home: Home
)

// TODO: Combine Away and Home models
data class Away(
        val leagueRecord: LeagueRecord,
        val seriesNumber: Int, //52
        val splitSquad: Boolean, //false
        val team: Team,
        // TODO: Find other nullable fields that generated kotlin does not contain
        val score: Int?
)

data class Home(
        val leagueRecord: LeagueRecord,
        val seriesNumber: Int, //53
        val splitSquad: Boolean, //false
        val team: Team,
        val score: Int?
)

data class LeagueRecord(
        val losses: Int, //21
        // TODO: String->Double
        val pct: String, //.553
        val wins: Int //26
)

data class Team(
        val id: Int, //112
        val link: String, ///api/v1/teams/112
        val name: String //Chicago Cubs
)