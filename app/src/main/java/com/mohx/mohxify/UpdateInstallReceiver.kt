package com.mohx.mohxify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.widget.Toast

class UpdateInstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                @Suppress("DEPRECATION")
                val confirmation = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                confirmation?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (confirmation != null) context.startActivity(confirmation)
            }
            PackageInstaller.STATUS_SUCCESS ->
                Toast.makeText(context, "Mohxify updated successfully", Toast.LENGTH_LONG).show()
            else -> {
                val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                    ?: "Android could not install this update"
                val userMessage = if (
                    message.contains("UPDATE_INCOMPATIBLE", ignoreCase = true) ||
                    message.contains("signature", ignoreCase = true)
                ) {
                    "Signing key mismatch. Back up your loadouts, reinstall the signed release, then restore the backup."
                } else {
                    "Update failed: $message"
                }
                Toast.makeText(context, userMessage, Toast.LENGTH_LONG).show()
            }
        }
    }
}
