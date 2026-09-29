package uz.auto.browser.car

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Template
import uz.auto.browser.R

/** "This feature is not available in the current Android Auto environment." */
class CarUnavailableScreen(carContext: CarContext) : Screen(carContext) {

    override fun onGetTemplate(): Template {
        val message = carContext.getString(R.string.error_aa_unsupported) + "\n\n" +
            carContext.getString(R.string.car_reason) + "\n" +
            carContext.getString(R.string.car_open_on_phone)
        return MessageTemplate.Builder(message)
            .setTitle(carContext.getString(R.string.app_name))
            .setHeaderAction(Action.BACK)
            .addAction(
                Action.Builder()
                    .setTitle(carContext.getString(R.string.car_ok))
                    .setOnClickListener { screenManager.pop() }
                    .build()
            )
            .build()
    }
}
