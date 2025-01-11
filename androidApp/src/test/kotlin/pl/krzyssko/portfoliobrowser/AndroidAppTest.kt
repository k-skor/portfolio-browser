package pl.krzyssko.portfoliobrowser

import org.junit.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.test.KoinTest
import org.koin.test.verify.verify
import pl.krzyssko.portfoliobrowser.android.di.androidAppModule
import pl.krzyssko.portfoliobrowser.di.sharedAppModule

class CheckModulesTest : KoinTest {

    @Test
    @KoinExperimentalAPI
    fun checkAllModules() {
        sharedAppModule().verify()
        androidAppModule.verify()
    }
}
