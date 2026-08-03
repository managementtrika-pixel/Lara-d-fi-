package com.zeubicardgames.app.di;

import com.zeubicardgames.app.core.database.GameDao;
import com.zeubicardgames.app.core.database.ZeubiDatabase;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata
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
public final class AppModule_DaoFactory implements Factory<GameDao> {
  private final Provider<ZeubiDatabase> dbProvider;

  private AppModule_DaoFactory(Provider<ZeubiDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public GameDao get() {
    return dao(dbProvider.get());
  }

  public static AppModule_DaoFactory create(Provider<ZeubiDatabase> dbProvider) {
    return new AppModule_DaoFactory(dbProvider);
  }

  public static GameDao dao(ZeubiDatabase db) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.dao(db));
  }
}
