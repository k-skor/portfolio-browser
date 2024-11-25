package pl.krzyssko.portfoliobrowser.android

import android.app.Application
import org.koin.core.context.GlobalContext.startKoin
import pl.krzyssko.portfoliobrowser.android.di.androidAppModule
import pl.krzyssko.portfoliobrowser.di.sharedAppModule

class AndroidApp: Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            modules(sharedAppModule() + androidAppModule)
        }
    }
}