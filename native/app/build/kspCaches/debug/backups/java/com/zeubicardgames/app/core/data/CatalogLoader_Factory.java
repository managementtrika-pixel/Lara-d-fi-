package com.zeubicardgames.app.core.data;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class CatalogLoader_Factory implements Factory<CatalogLoader> {
  private final Provider<Context> contextProvider;

  private CatalogLoader_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public CatalogLoader get() {
    return newInstance(contextProvider.get());
  }

  public static CatalogLoader_Factory create(Provider<Context> contextProvider) {
    return new CatalogLoader_Factory(contextProvider);
  }

  public static CatalogLoader newInstance(Context context) {
    return new CatalogLoader(context);
  }
}
