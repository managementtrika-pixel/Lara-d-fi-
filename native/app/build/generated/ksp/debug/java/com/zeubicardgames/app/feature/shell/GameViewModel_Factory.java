package com.zeubicardgames.app.feature.shell;

import com.zeubicardgames.app.core.data.GameRepository;
import com.zeubicardgames.app.core.data.PreferencesStore;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class GameViewModel_Factory implements Factory<GameViewModel> {
  private final Provider<GameRepository> repositoryProvider;

  private final Provider<PreferencesStore> preferencesProvider;

  private GameViewModel_Factory(Provider<GameRepository> repositoryProvider,
      Provider<PreferencesStore> preferencesProvider) {
    this.repositoryProvider = repositoryProvider;
    this.preferencesProvider = preferencesProvider;
  }

  @Override
  public GameViewModel get() {
    return newInstance(repositoryProvider.get(), preferencesProvider.get());
  }

  public static GameViewModel_Factory create(Provider<GameRepository> repositoryProvider,
      Provider<PreferencesStore> preferencesProvider) {
    return new GameViewModel_Factory(repositoryProvider, preferencesProvider);
  }

  public static GameViewModel newInstance(GameRepository repository, PreferencesStore preferences) {
    return new GameViewModel(repository, preferences);
  }
}
