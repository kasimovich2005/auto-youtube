package uz.auto.browser.car

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.connection.CarConnection
import androidx.car.app.model.Action
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import uz.auto.browser.Diagnostics
import uz.auto.browser.R

/** Diagnostics on the car screen. The host may cap the row count while driving. */
class CarDiagnosticsScreen(carContext: CarContext) : Screen(carContext) {

    override fun onGetTemplate(): Template {
        val list = ItemList.Builder()
        Diagnostics.report(carContext, CarConnection.CONNECTION_TYPE_PROJECTION).forEach { (key, value) ->
            list.addItem(Row.Builder().setTitle(key).addText(value).build())
        }
        return ListTemplate.Builder()
            .setTitle(carContext.getString(R.string.car_diagnostics))
            .setHeaderAction(Action.BACK)
            .setSingleList(list.build())
            .build()
    }
}
