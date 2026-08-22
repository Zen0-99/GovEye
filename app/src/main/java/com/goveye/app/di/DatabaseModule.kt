package com.goveye.app.di

import android.content.Context
import androidx.room.Room
import com.goveye.app.data.local.BundledDatabase
import com.goveye.app.data.local.LocalDatabase
import com.goveye.app.data.local.dao.BillDao
import com.goveye.app.data.local.dao.BillFollowDao
import com.goveye.app.data.local.dao.BioDataDao
import com.goveye.app.data.local.dao.CommitteeDao
import com.goveye.app.data.local.dao.DatabaseUpdateDao
import com.goveye.app.data.local.dao.DebateSpeechDao
import com.goveye.app.data.local.dao.DivisionDao
import com.goveye.app.data.local.dao.ExpenseDao
import com.goveye.app.data.local.dao.FollowDao
import com.goveye.app.data.local.dao.HansardDao
import com.goveye.app.data.local.dao.HistoricalMemberDao
import com.goveye.app.data.local.dao.InterestDao
import com.goveye.app.data.local.dao.ManifestoDao
import com.goveye.app.data.local.dao.MpDao
import com.goveye.app.data.local.dao.MpLinkDao
import com.goveye.app.data.local.dao.MpStatsDao
import com.goveye.app.data.local.dao.MpNotificationPreferenceDao
import com.goveye.app.data.local.dao.PartyStatsDao
import com.goveye.app.data.local.dao.RecessDateDao
import com.goveye.app.data.local.dao.SearchDao
import com.goveye.app.data.mapper.HansardMapper
import com.goveye.app.data.mapper.MemberMapper
import com.goveye.app.data.repo.BillFollowRepository
import com.goveye.app.data.repo.BillsRepository
import com.goveye.app.data.repo.BioDataRepository
import com.goveye.app.data.repo.CommitteesRepository
import com.goveye.app.data.repo.ExpensesRepository
import com.goveye.app.data.repo.FeedRepository
import com.goveye.app.data.repo.FollowRepository
import com.goveye.app.data.repo.HansardRepository
import com.goveye.app.data.repo.HistoricalMemberRepository
import com.goveye.app.data.repo.InterestsRepository
import com.goveye.app.data.repo.ManifestoRepository
import com.goveye.app.data.repo.MembersRepository
import com.goveye.app.data.repo.MpLinksRepository
import com.goveye.app.data.repo.NotificationPreferenceRepository
import com.goveye.app.data.repo.PartyStatsRepository
import com.goveye.app.data.repo.StatsRepository
import com.goveye.app.data.repo.VotesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideBundledDatabase(@ApplicationContext context: Context): BundledDatabase {
        // Ensure the databases directory exists so that the first-launch download
        // can place the DB file at Room's expected path before Room opens (D-04).
        context.getDatabasePath(BundledDatabase.DATABASE_NAME).parentFile?.mkdirs()
        return Room
            .databaseBuilder(
                context,
                BundledDatabase::class.java,
                BundledDatabase.DATABASE_NAME
            ).fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    @Provides
    @Singleton
    fun provideLocalDatabase(@ApplicationContext context: Context): LocalDatabase {
        // LocalDatabase is created fresh on first launch by Room (no pre-placed file).
        // User data persists across all DB updates and seed DB swaps (D-10a).
        return Room
            .databaseBuilder(
                context,
                LocalDatabase::class.java,
                LocalDatabase.DATABASE_NAME
            ).fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    // ── Bundled table DAOs (from BundledDatabase) ──────────────────────

    @Provides
    fun provideSearchDao(database: BundledDatabase): SearchDao = database.searchDao()

    @Provides
    fun provideMpDao(database: BundledDatabase): MpDao = database.mpDao()

    @Provides
    fun provideCommitteeDao(database: BundledDatabase): CommitteeDao = database.committeeDao()

    @Provides
    fun provideDivisionDao(database: BundledDatabase): DivisionDao = database.divisionDao()

    @Provides
    fun provideBillDao(database: BundledDatabase): BillDao = database.billDao()

    @Provides
    fun provideHansardDao(database: BundledDatabase): HansardDao = database.hansardDao()

    @Provides
    fun provideInterestDao(database: BundledDatabase): InterestDao = database.interestDao()

    @Provides
    fun provideRecessDateDao(database: BundledDatabase): RecessDateDao = database.recessDateDao()

    @Provides
    fun provideDebateSpeechDao(database: BundledDatabase): DebateSpeechDao = database.debateSpeechDao()

    @Provides
    fun provideBioDataDao(database: BundledDatabase): BioDataDao = database.bioDataDao()

    @Provides
    fun provideExpenseDao(database: BundledDatabase): ExpenseDao = database.expenseDao()

    @Provides
    fun provideMpLinkDao(database: BundledDatabase): MpLinkDao = database.mpLinkDao()

    @Provides
    fun provideManifestoDao(database: BundledDatabase): ManifestoDao = database.manifestoDao()

    @Provides
    fun providePartyStatsDao(database: BundledDatabase): PartyStatsDao = database.partyStatsDao()

    @Provides
    fun provideHistoricalMemberDao(database: BundledDatabase): HistoricalMemberDao = database.historicalMemberDao()

    @Provides
    fun provideMpStatsDao(database: BundledDatabase): MpStatsDao = database.mpStatsDao()

    @Provides
    fun provideDatabaseUpdateDao(database: BundledDatabase): DatabaseUpdateDao = database.databaseUpdateDao()

    // ── User-data DAOs (from LocalDatabase) ────────────────────────────

    @Provides
    fun provideFollowDao(database: LocalDatabase): FollowDao = database.followDao()

    @Provides
    fun provideBillFollowDao(database: LocalDatabase): BillFollowDao = database.billFollowDao()

    @Provides
    fun provideMpNotificationPreferenceDao(database: LocalDatabase): MpNotificationPreferenceDao =
        database.mpNotificationPreferenceDao()

    // ── Mappers ────────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideMemberMapper(): MemberMapper = MemberMapper

    @Provides
    @Singleton
    fun provideHansardMapper(): HansardMapper = HansardMapper

    // ── Repositories ───────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideMembersRepository(
        mpDao: MpDao,
        searchDao: SearchDao,
        membersApi: com.goveye.app.data.api.MembersApi,
        memberMapper: MemberMapper,
        historicalMemberDao: com.goveye.app.data.local.dao.HistoricalMemberDao
    ): MembersRepository = MembersRepository(mpDao, searchDao, membersApi, memberMapper, historicalMemberDao)

    @Provides
    @Singleton
    fun provideCommitteesRepository(committeeDao: CommitteeDao): CommitteesRepository =
        CommitteesRepository(committeeDao)

    @Provides
    @Singleton
    fun provideVotesRepository(divisionDao: DivisionDao, debateSpeechDao: DebateSpeechDao): VotesRepository =
        VotesRepository(divisionDao, debateSpeechDao)

    @Provides
    @Singleton
    fun provideFeedRepository(
        divisionDao: DivisionDao,
        followDao: FollowDao,
        recessDateDao: RecessDateDao
    ): FeedRepository = FeedRepository(divisionDao, followDao, recessDateDao)

    @Provides
    @Singleton
    fun provideBillsRepository(billDao: BillDao): BillsRepository = BillsRepository(billDao)

    @Provides
    @Singleton
    fun provideHansardRepository(
        hansardDao: HansardDao,
        hansardApi: com.goveye.app.data.api.HansardApi,
        hansardMapper: HansardMapper
    ): HansardRepository = HansardRepository(hansardDao, hansardApi, hansardMapper)

    @Provides
    @Singleton
    fun provideInterestsRepository(interestDao: InterestDao): InterestsRepository = InterestsRepository(interestDao)

    @Provides
    @Singleton
    fun provideBioDataRepository(bioDataDao: BioDataDao): BioDataRepository = BioDataRepository(bioDataDao)

    @Provides
    @Singleton
    fun provideExpensesRepository(expenseDao: ExpenseDao): ExpensesRepository = ExpensesRepository(expenseDao)

    @Provides
    @Singleton
    fun provideMpLinksRepository(mpLinkDao: MpLinkDao): MpLinksRepository = MpLinksRepository(mpLinkDao)

    @Provides
    @Singleton
    fun provideManifestoRepository(manifestoDao: ManifestoDao): ManifestoRepository = ManifestoRepository(manifestoDao)

    @Provides
    @Singleton
    fun providePartyStatsRepository(partyStatsDao: PartyStatsDao): PartyStatsRepository =
        PartyStatsRepository(partyStatsDao)

    @Provides
    @Singleton
    fun provideHistoricalMemberRepository(historicalMemberDao: HistoricalMemberDao): HistoricalMemberRepository =
        HistoricalMemberRepository(historicalMemberDao)

    @Provides
    @Singleton
    fun provideStatsRepository(
        divisionDao: DivisionDao,
        committeeDao: CommitteeDao,
        debateSpeechDao: DebateSpeechDao,
        hansardDao: HansardDao,
        mpDao: MpDao,
        mpStatsDao: MpStatsDao
    ): StatsRepository = StatsRepository(divisionDao, committeeDao, debateSpeechDao, hansardDao, mpDao, mpStatsDao)

    @Provides
    @Singleton
    fun provideFollowRepository(followDao: FollowDao, mpDao: MpDao): FollowRepository =
        FollowRepository(followDao, mpDao)

    @Provides
    @Singleton
    fun provideBillFollowRepository(billFollowDao: BillFollowDao): BillFollowRepository =
        BillFollowRepository(billFollowDao)
}
