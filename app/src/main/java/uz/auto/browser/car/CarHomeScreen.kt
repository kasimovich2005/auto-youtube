package uz.auto.browser.car

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import uz.auto.browser.AbLog
import uz.auto.browser.Prefs
import uz.auto.browser.R
import uz.auto.browser.UrlUtils

/**
 * Car-screen home. Mirrors the phone home screen, but the Car App Library
 * only offers templates (lists, messages, panes): it has no WebView or video
 * surface for this app category, so opening a site explains why it can't be
 * shown on the car screen instead of trying to render it.
 */
class CarHomeScreen(carContext: CarContext) : Screen(carContext) {

    override fun onGetTemplate(): Template {
        val list = ItemList.Builder()
            .addItem(siteRow(carContext.getString(R.string.youtube), UrlUtils.YOUTUBE))
            .addItem(siteRow(carContext.getString(R.string.google), UrlUtils.GOOGLE))
            .addItem(siteRow(carContext.getString(R.string.enter_website), null))

        Prefs(carContext).recent
            .filter { UrlUtils.label(it) != UrlUtils.label(UrlUtils.YOUTUBE) &&
                UrlUtils.label(it) != UrlUtils.label(UrlUtils.GOOGLE) }
            .take(2)
            .forEach { list.addItem(siteRow(UrlUtils.label(it), it)) }

        list.addItem(
            Row.Builder()
                .setTitle(carContext.getString(R.string.car_diagnostics))
                .setBrowsable(true)
                .setOnClickListener { screenManager.push(CarDiagnosticsScreen(carContext)) }
                .build()
        )

        return ListTemplate.Builder()
            .setTitle(carContext.getString(R.string.app_name))
            .setHeaderAction(Action.APP_ICON)
            .setSingleList(list.build())
            .build()
    }

    private fun siteRow(title: String, url: String?): Row {
        val row = Row.Builder()
            .setTitle(title)
            .setOnClickListener {
                AbLog.d(AbLog.AUTO, "Car screen requested ${url ?: "URL entry"}; not renderable on Android Auto")
                screenManager.push(CarUnavailableScreen(carContext))
            }
        if (url != null) row.addText(url)
        return row.build()
    }
}
