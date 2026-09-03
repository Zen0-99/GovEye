package com.goveye.app.ui.screens.directory

import com.goveye.app.data.local.dao.CommitteeSummary
import com.goveye.app.data.local.dao.CouncilSummary
import com.goveye.app.data.local.dao.PartySummary
import com.goveye.app.domain.model.GovernmentPublication
import com.goveye.app.domain.model.Legislation
import com.goveye.app.domain.model.Mp
import com.goveye.app.domain.model.WrittenStatement

/**
 * Process-level cache for Directory screen data.
 *
 * Same pattern as [com.goveye.app.ui.screens.feed.FeedCache] — the
 * DirectoryViewModel is recreated on every tab switch (Nav3 limitation).
 * Without a cache, each recreation re-queries parties, committees,
 * councils, and government data from the DB.
 *
 * Unlike FeedCache (single state), DirectoryCache stores each list
 * independently because DirectoryViewModel exposes multiple StateFlows
 * rather than a single UiState. Each cached list is used as the initial
 * value for its corresponding StateFlow.
 *
 * The one-shot lists (parties, committees, councils) use
 * SharingStarted.Eagerly — they run once per VM lifetime. Caching
 * their results means the next VM instance starts with the data
 * already populated, and the Eagerly flow just confirms it's still
 * current.
 *
 * The government lists (publications, statements, legislation) are
 * reactive Room flows with WhileSubscribed(5000). Caching them means
 * the Government tab shows content immediately on navigation instead
 * of the skeleton screen.
 */
object DirectoryCache {
    private var _parties: List<PartySummary>? = null
    private var _councils: List<CouncilSummary>? = null
    private var _committees: List<CommitteeSummary>? = null
    private var _publications: List<GovernmentPublication>? = null
    private var _statements: List<WrittenStatement>? = null
    private var _legislation: List<Legislation>? = null
    private var _firstPageMps: List<Mp>? = null

    val parties: List<PartySummary>? get() = _parties
    val councils: List<CouncilSummary>? get() = _councils
    val committees: List<CommitteeSummary>? get() = _committees
    val publications: List<GovernmentPublication>? get() = _publications
    val statements: List<WrittenStatement>? get() = _statements
    val legislation: List<Legislation>? get() = _legislation
    val firstPageMps: List<Mp>? get() = _firstPageMps

    fun updateParties(value: List<PartySummary>) {
        _parties = value
    }
    fun updateCouncils(value: List<CouncilSummary>) {
        _councils = value
    }
    fun updateCommittees(value: List<CommitteeSummary>) {
        _committees = value
    }
    fun updatePublications(value: List<GovernmentPublication>) {
        _publications = value
    }
    fun updateStatements(value: List<WrittenStatement>) {
        _statements = value
    }
    fun updateLegislation(value: List<Legislation>) {
        _legislation = value
    }
    fun updateFirstPageMps(value: List<Mp>) {
        _firstPageMps = value
    }

    fun clear() {
        _parties = null
        _councils = null
        _committees = null
        _publications = null
        _statements = null
        _legislation = null
        _firstPageMps = null
    }
}
