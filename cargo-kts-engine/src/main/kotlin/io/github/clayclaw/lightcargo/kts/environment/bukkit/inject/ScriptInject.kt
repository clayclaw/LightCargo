package io.github.clayclaw.lightcargo.kts.environment.bukkit.inject

import dev.reactant.reactant.core.ReactantCore
import dev.reactant.reactant.core.dependency.ProviderManager
import dev.reactant.reactant.core.dependency.injection.producer.ComponentProvider
import dev.reactant.reactant.core.dependency.injection.producer.DynamicProvider
import dev.reactant.reactant.core.dependency.injection.producer.Provider
import io.github.clayclaw.lightcargo.kts.environment.bukkit.BukkitScriptManager
import io.github.clayclaw.lightcargo.kts.environment.bukkit.ScriptLoader
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.createType
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.typeOf

/**
 * Resolve a Reactant injectable for scripts.
 *
 * Unlike Aelorn's `injectContainer()`, this also resolves `@Provide` services
 * (e.g. EventService) by going through [ProviderManager] with [ScriptLoader]
 * as the requester.
 */
inline fun <reified T : Any> inject(name: String = ""): T {
    return injectOrNull(name)
        ?: error(
            "No Reactant provider for ${T::class.qualifiedName}" +
                (name.takeIf { it.isNotEmpty() }?.let { " (name=\"$it\")" } ?: "") +
                ". Use inject() for @Component and @Provide services; " +
                "injectContainer() only finds @Component instances."
        )
}

inline fun <reified T : Any> injectOrNull(name: String = ""): T? {
    return ScriptReactantInjector.resolve(typeOf<T>(), T::class, name)
}

object ScriptReactantInjector {

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> resolve(requestedType: KType, requestedClass: KClass<T>, name: String): T? {
        val instanceManager = ReactantCore.instance.instanceManager

        instanceManager.getInstance(requestedClass)?.let { return it }

        val providerManager = instanceManager.getInstance(ProviderManager::class) ?: return null
        val requester = scriptRequester(providerManager) ?: return null

        val provider = selectProvider(
            providers = providerManager.availableProviders.filter { it.disabledReason == null },
            requestedType = requestedType,
            requestedClass = requestedClass,
            name = name
        ) ?: return null

        return provider.producer(requestedType, name, requester) as T?
    }

    internal fun selectProvider(
        providers: Collection<Provider>,
        requestedType: KType,
        requestedClass: KClass<*>,
        name: String
    ): Provider? {
        val matches = providers.filter { provider ->
            provider.canProvideType(requestedType) && nameMatches(name, provider.namePattern)
        }
        if (matches.isEmpty()) return null

        matches.firstOrNull { provider ->
            provider is ComponentProvider<*> && provider.componentClass == requestedClass
        }?.let { return it }

        // @Provide factories are DynamicProvider and usually set ignoreGenerics=true
        matches.firstOrNull { it is DynamicProvider<*, *> || it.ignoreGenerics }?.let { return it }

        matches.firstOrNull { it.productType.jvmErasure == requestedClass }?.let { return it }

        return matches.first()
    }

    /** Reactant matches the injection name against each provider's namePattern regex. */
    internal fun nameMatches(requestedName: String, namePattern: String): Boolean {
        return requestedName.matches(namePattern.toRegex())
    }

    private fun scriptRequester(providerManager: ProviderManager): Provider? {
        val componentProviders = providerManager.availableProviders.mapNotNull { it as? ComponentProvider<*> }
        return componentProviders.firstOrNull { it.componentClass == ScriptLoader::class }
            ?: componentProviders.firstOrNull { it.componentClass == BukkitScriptManager::class }
    }
}

fun <T : Any> inject(requestedClass: KClass<T>, name: String = ""): T {
    return ScriptReactantInjector.resolve(requestedClass.createType(), requestedClass, name)
        ?: error("No Reactant provider for ${requestedClass.qualifiedName}")
}
