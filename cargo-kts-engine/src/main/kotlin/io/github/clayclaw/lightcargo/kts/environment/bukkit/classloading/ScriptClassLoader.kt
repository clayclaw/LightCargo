package io.github.clayclaw.lightcargo.kts.environment.bukkit.classloading

import java.net.URL
import java.net.URLClassLoader

class ScriptClassLoader(
    scriptClasspath: List<URL>,
    private val policy: ClassLoadingPolicy
) : URLClassLoader(scriptClasspath.toTypedArray(), null) {

    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        synchronized(getClassLoadingLock(name)) {
            findLoadedClass(name)?.let {
                if (resolve) resolveClass(it)
                return it
            }

            policy.routeFor(name)?.let { route ->
                return route.classLoader.loadRoutedClass(name, resolve)
            }

            runCatching {
                val loadedClass = findClass(name)
                if (resolve) resolveClass(loadedClass)
                return loadedClass
            }

            policy.serverClassLoader?.let { serverLoader ->
                runCatching {
                    return serverLoader.loadRoutedClass(name, resolve)
                }
            }

            return super.loadClass(name, resolve)
        }
    }

    private fun ClassLoader.loadRoutedClass(name: String, resolve: Boolean): Class<*> {
        val loadedClass = loadClass(name)
        if (resolve) resolveClass(loadedClass)
        return loadedClass
    }
}
