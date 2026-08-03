package com.zeubicardgames.app.core.data;

import com.zeubicardgames.app.core.database.GameDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class GameRepository_Factory implements Factory<GameRepository> {
  private final Provider<CatalogLoader> loaderProvider;

  private final Provider<GameDao> daoProvider;

  private GameRepository_Factory(Provider<CatalogLoader> loaderProvider,
      Provider<GameDao> daoProvider) {
    this.loaderProvider = loaderProvider;
    this.daoProvider = daoProvider;
  }

  @Override
  public GameRepository get() {
    return newInstance(loaderProvider.get(), daoProvider.get());
  }

  public static GameRepository_Factory create(Provider<CatalogLoader> loaderProvider,
      Provider<GameDao> daoProvider) {
    return new GameRepository_Factory(loaderProvider, daoProvider);
  }

  public static GameRepository newInstance(CatalogLoader loader, GameDao dao) {
    return new GameRepository(loader, dao);
  }
}
