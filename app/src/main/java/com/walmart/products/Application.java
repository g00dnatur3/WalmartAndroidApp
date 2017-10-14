package com.walmart.products;

import android.content.Context;

import com.walmart.products.service.WalmartService;
import com.walmart.products.service.WalmartServiceUtils;

import java.io.IOException;
import java.util.Properties;

import javax.inject.Singleton;

import dagger.Component;

/**
 * This is the entry point for Dagger2 dependency injection.
 *
 * https://developer.android.com/reference/android/app/Application.html
 *
 * <pre>"You can provide your own implementation by creating a subclass and specifying
 * the fully-qualified name of this subclass as the "android:name" attribute in
 * your AndroidManifest.xml's <application> tag."</pre>
 */
public class Application extends android.app.Application {

    private ApplicationComponent mComponent;

    private static Properties mProperties;

    public static Application get(Context context) {
        return (Application) context.getApplicationContext();
    }

    public static String getProperty(String name) {
        return mProperties.getProperty(name);
    }

    @Singleton
    @Component(modules = AppModule.class)
    public interface ApplicationComponent {
        void inject(Application application);
        void inject(WalmartService walmartService);
        void inject(WalmartServiceUtils walmartServiceUtils);
    }

    @Override public void onCreate() {
        super.onCreate();
        mProperties = loadProperties("application.properties");
        mComponent = DaggerApplication_ApplicationComponent.builder()
                .appModule(new AppModule(this))
                .build();
        component().inject(this);
    }

    public ApplicationComponent component() {
        return mComponent;
    }

    protected Properties loadProperties(String name) {
        Properties props = new Properties();
        try {
            props.load(getAssets().open(name));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return props;
    }
}
