package com.goveye.app.di

import android.content.Context
import androidx.room.Room
import com.goveye.app.data.local.GovEyeDatabase
import com.goveye.app.data.local.dao.BillDao
import com.goveye.app.data.local.dao.BillFollowDao
import com.goveye.app.data.local.dao.DatabaseUpdateDao
import com.goveye.app.data.local.dao.DivisionDao
import com.goveye.app.data.local.dao.FollowDao
import com.goveye.app.data.local.dao.HansardDao
import com.goveye.app.data.local.dao.InterestDao
import com.goveye.app.data.local.dao.CommitteeDao
import com.goveye.app.data.local.dao.MpDao
import com.goveye.app.data.local.dao.MpNotificationPreferenceDao
import com.goveye.app.data.local.dao.RecessDateDao
import com.goveye.app.data.local.dao.RemoteKeyDao
import com.goveye.app.data.local.dao.SearchDao
import com.goveye.app.data.mapper.BillMapper
import com.goveye.app.data.mapper.DivisionMapper
import com.goveye.app.data.mapper.HansardMapper
import com.goveye.app.data.mapper.InterestMapper
import com.goveye.app.data.mapper.MemberMapper
import com.goveye.app.data.repo.BillsRepository
import com.goveye.app.data.repo.BillFollowRepository
import com.goveye.app.data.repo.CommitteesRepository
import com.goveye.app.data.repo.FollowRepository
import com.goveye.app.data.repo.HansardRepository
import com.goveye.app.data.repo.InterestsRepository
import com.goveye.app.data.repo.MembersRepository
import com.goveye.app.data.repo.MpRemoteMediator
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
    fun provideGovEyeDatabase(@ApplicationContext context: Context): GovEyeDatabase {
        // Ensure the databases directory exists so that the first-launch download
        // can place the DB file at Room's expected path before Room opens (D-04).
        context.getDatabasePath(GovEyeDatabase.DATABASE_NAME).parentFile?.mkdirs()
        return Room
            .databaseBuilder(
                context,
                GovEyeDatabase::class.java,
                GovEyeDatabase.DATABASE_NAME,
            ).fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    @Provides
    fun provideSearchDao(database: GovEyeDatabase): SearchDao = database.searchDao()

    @Provides
    fun provideMpDao(database: GovEyeDatabase): MpDao = database.mpDao()

    @Provides
    fun provideRemoteKeyDao(database: GovEyeDatabase): RemoteKeyDao = database.remoteKeyDao()

    @Provides
    fun provideCommitteeDao(database: GovEyeDatabase): CommitteeDao = database.committeeDao()

    @Provides
    fun provideDivisionDao(database: GovEyeDatabase): DivisionDao = database.divisionDao()

    @Provides
    fun provideBillDao(database: GovEyeDatabase): BillDao = database.billDao()

    @Provides
    fun provideBillFollowDao(database: GovEyeDatabase): BillFollowDao = database.billFollowDao()

    @Provides
    fun provideHansardDao(database: GovEyeDatabase): HansardDao = database.hansardDao()

    @Provides
    fun provideInterestDao(database: GovEyeDatabase): InterestDao = database.interestDao()

    @Provides
    fun provideFollowDao(database: GovEyeDatabase): FollowDao = database.followDao()

    @Provides
    fun provideRecessDateDao(database: GovEyeDatabase): RecessDateDao = database.recessDateDao()

    @Provides
    fun provideMpNotificationPreferenceDao(database: GovEyeDatabase): MpNotificationPreferenceDao =
        database.mpNotificationPreferenceDao()

    @Provides
    fun provideDatabaseUpdateDao(database: GovEyeDatabase): DatabaseUpdateDao =
        database.databaseUpdateDao()

    @Provides
    @Singleton
    fun provideMemberMapper(): MemberMapper = MemberMapper

    @Provides
    @Singleton
    fun provideDivisionMapper(): DivisionMapper = DivisionMapper

    @Provides
    @Singleton
    fun provideBillMapper(): BillMapper = BillMapper

    @Provides
    @Singleton
    fun provideHansardMapper(): HansardMapper = HansardMapper

    @Provides
    @Singleton
    fun provideInterestMapper(): InterestMapper = InterestMapper

    @Provides
    @Singleton
    fun provideMpRemoteMediator(
        mpDao: MpDao,
        remoteKeyDao: RemoteKeyDao,
        membersApi: com.goveye.app.data.api.MembersApi,
        memberMapper: MemberMapper,
    ): MpRemoteMediator = MpRemoteMediator(mpDao, remoteKeyDao, membersApi, memberMapper)

    @Provides
    @Singleton
    fun provideMembersRepository(
        mpDao: MpDao,
        searchDao: SearchDao,
        membersApi: com.goveye.app.data.api.MembersApi,
        memberMapper: MemberMapper,
        mpRemoteMediator: MpRemoteMediator,
        remoteKeyDao: RemoteKeyDao,
    ): MembersRepository = MembersRepository(mpDao, searchDao, membersApi, memberMapper, mpRemoteMediator, remoteKeyDao)

    @Provides
    @Singleton
    fun provideCommitteesRepository(
        committeeDao: CommitteeDao,
        committeesApi: com.goveye.app.data.api.CommitteesApi,
    ): CommitteesRepository = CommitteesRepository(committeeDao, committeesApi)

    @Provides
    @Singleton
    fun provideVotesRepository(
        divisionDao: DivisionDao,
        votesApi: com.goveye.app.data.api.VotesApi,
        lordsVotesApi: com.goveye.app.data.api.LordsVotesApi,
        divisionMapper: DivisionMapper,
    ): VotesRepository = VotesRepository(divisionDao, votesApi, lordsVotesApi, divisionMapper)

    @Provides
    @Singleton
    fun provideBillsRepository(
        billDao: BillDao,
        billsApi: com.goveye.app.data.api.BillsApi,
        billMapper: BillMapper,
    ): BillsRepository = BillsRepository(billDao, billsApi, billMapper)

    @Provides
    @Singleton
    fun provideHansardRepository(
        hansardDao: HansardDao,
        hansardApi: com.goveye.app.data.api.HansardApi,
        hansardMapper: HansardMapper,
    ): HansardRepository = HansardRepository(hansardDao, hansardApi, hansardMapper)

    @Provides
    @Singleton
    fun provideInterestsRepository(
        interestDao: InterestDao,
        interestsApi: com.goveye.app.data.api.InterestsApi,
        interestMapper: InterestMapper,
    ): InterestsRepository = InterestsRepository(interestDao, interestsApi, interestMapper)

    @Provides
    @Singleton
    fun provideFollowRepository(followDao: FollowDao): FollowRepository = FollowRepository(followDao)

    @Provides
    @Singleton
    fun provideBillFollowRepository(billFollowDao: BillFollowDao): BillFollowRepository = BillFollowRepository(billFollowDao)
}
