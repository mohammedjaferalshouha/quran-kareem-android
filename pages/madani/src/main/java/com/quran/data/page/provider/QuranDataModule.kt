package com.quran.data.page.provider

import com.quran.common.upgrade.LocalDataUpgrade
import com.quran.common.upgrade.PreferencesUpgrade
import com.quran.data.constant.DependencyInjectionConstants
import com.quran.data.page.provider.madani.MadaniPageProvider
import com.quran.data.page.provider.madani.Madani1441LinePageProvider
import com.quran.data.pageinfo.mapper.AyahMapper
import com.quran.data.pageinfo.mapper.IdentityAyahMapper
import com.quran.data.dao.Settings
import com.quran.data.source.PageProvider
import com.quran.labs.androidquran.pages.common.madani.upgrade.MadaniPreferencesUpgrade
import com.quran.page.common.draw.ImageDrawHelper
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ElementsIntoSet
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Named
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.StringKey


@BindingContainer
object QuranDataModule {

  @Named(DependencyInjectionConstants.FALLBACK_PAGE_TYPE)
  @JvmStatic
  @Provides
  fun provideFallbackPageType(): String = Madani1441LinePageProvider.PAGE_TYPE

  @JvmStatic
  @Provides
  @IntoMap
  @StringKey(Madani1441LinePageProvider.PAGE_TYPE)
  fun provideMadani1441LinePageSet(): PageProvider {
    return Madani1441LinePageProvider()
  }

  @JvmStatic
  @Provides
  @IntoMap
  @StringKey("madani")
  fun provideMadaniPageSet(): PageProvider {
    return MadaniPageProvider()
  }

  @JvmStatic
  @Provides
  @ElementsIntoSet
  fun provideImageDrawHelpers(): Set<ImageDrawHelper> {
    return emptySet()
  }

  @JvmStatic
  @Provides
  fun provideLocalDataUpgrade(): LocalDataUpgrade = object : LocalDataUpgrade {  }

  @JvmStatic
  @Provides
  fun providePreferencesUpgrade(settings: Settings): PreferencesUpgrade =
    MadaniPreferencesUpgrade(settings)

  @JvmStatic
  @Provides
  fun provideAyahMapper(): AyahMapper = IdentityAyahMapper()
}
