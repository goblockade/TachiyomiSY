package exh.log

import android.util.Log
import com.elvishew.xlog.printer.Printer

class CrashlyticsPrinter(private val logLevel: Int) : Printer {
    override fun println(logLevel: Int, tag: String?, msg: String?) {
        if (logLevel >= this.logLevel) {
            Log.println(logLevel, tag ?: "XLog", msg ?: "")
        }
    }
}
