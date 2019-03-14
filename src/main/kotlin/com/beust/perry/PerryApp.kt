package com.beust.perry

import com.hubspot.dropwizard.guice.GuiceBundle
import io.dropwizard.Application
import io.dropwizard.assets.AssetsBundle
import io.dropwizard.auth.AuthDynamicFeature
import io.dropwizard.auth.AuthValueFactoryProvider
import io.dropwizard.auth.basic.BasicCredentialAuthFilter
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment




class PerryApp : Application<DemoConfig>() {
    private lateinit var guiceBundle: GuiceBundle<DemoConfig>

    override fun initialize(configuration: Bootstrap<DemoConfig>) {
        configuration.addBundle(AssetsBundle("/assets", "/", "index.html", "static"));
        guiceBundle = GuiceBundle.newBuilder<DemoConfig>()
                .addModule(PerryModule())
                .setConfigClass(DemoConfig::class.java)
                .build()

        configuration.addBundle(guiceBundle)
    }

    override fun run(config: DemoConfig, env: Environment) {
        listOf(PerryService::class.java).forEach {
            env.jersey().register(it)
        }

        env.jersey().register(AuthValueFactoryProvider.Binder(User::class.java))

        env.jersey().register(AuthDynamicFeature(BasicCredentialAuthFilter.Builder<User>()
                .setAuthenticator(PerryAuthenticator(guiceBundle.injector.getInstance(PerryContext::class.java)))
                .setAuthorizer(PerryAuthorizer())
                .setRealm("BASIC-AUTH-REALM")
                .buildAuthFilter()))

//        env.jersey().register(AuthDynamicFeature(
//                PerryAuthFilterBuilder()
//                        .setAuthenticator(PerryAuthenticator())
//                        .setAuthorizer(PerryAuthorizer())
//                        .buildAuthFilter()
//        ))

        env.healthChecks().register("template", DemoCheck(config.version))
    }
}