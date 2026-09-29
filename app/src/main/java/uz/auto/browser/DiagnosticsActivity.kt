package uz.auto.browser

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.car.app.connection.CarConnection
import uz.auto.browser.databinding.ActivityDiagnosticsBinding

class DiagnosticsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDiagnosticsBinding
    private var carType: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDiagnosticsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        CarConnection(this).type.observe(this) {
            carType = it
            render()
        }
        binding.btnRefresh.setOnClickListener { render() }
        binding.btnCopy.setOnClickListener {
            getSystemService(ClipboardManager::class.java)?.setPrimaryClip(
                ClipData.newPlainText("Auto Browser diagnostics", binding.report.text)
            )
            Toast.makeText(this, R.string.diag_copied, Toast.LENGTH_SHORT).show()
        }
        render()
    }

    private fun render() {
        val items = Diagnostics.report(this, carType)
        binding.report.text = Diagnostics.asText(items)
        AbLog.d(AbLog.APP, "Diagnostics:\n" + binding.report.text)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
